package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Cable block entity.
 *
 * <p>In IL, cables are TE-driven for both rendering (thin geometry + connections) and
 * participation in the EnergyNet (detectors/splitters, per-node stats).</p>
 *
 * <p>This implementation keeps the same split of responsibilities:
 * <ul>
 *   <li>Stores last transferred EU for debugging/visualization.</li>
 *   <li>Accumulates energy-in for detector cables and exposes redstone/comparator output.</li>
 *   <li>Tracks active state for splitter/detector textures.</li>
 * </ul></p>
 */
public class CableBlockEntity extends BlockEntity {

    private long lastTransferredEu = 0;

    private int ticker = 0;

    private boolean active = false;
    private int redstoneLevel = 0;
    private int comparatorLevel = 0;

    // IL colored cable state. -1/black means uncolored and acts as a wildcard.
    private int color = -1;

    // Copper cable oxidation (IL extension; only for COPPER + insulation=0)
    private int oxidationLevel = 0; // 0 clean, 1 exposed, 2 weathered, 3 oxidized

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE, pos, state);
    }

    public long getLastTransferredEu() {
        return lastTransferredEu;
    }

    /**
     * Called by the EU network after a successful insert.
     * We also accumulate it for detector-cable redstone behavior.
     */
    public void setLastTransferredEu(long lastTransferredEu) {
        this.lastTransferredEu = lastTransferredEu;
        markDirty();
    }

    public boolean isActive() {
        return active;
    }

    public boolean isSplitterActive() {
        BlockState state = getCachedState();
        if (!(state.getBlock() instanceof CableBlock cb) || cb.getKind() != CableKind.SPLITTER) {
            return true;
        }
        return active;
    }

    public int getRedstoneLevel() {
        return redstoneLevel;
    }

    public int getComparatorLevel() {
        return comparatorLevel;
    }

    public int getColor() {
        return color;
    }

    public boolean hasColor() {
        return color >= 0;
    }

    public boolean canBeColored() {
        BlockState state = getCachedState();
        return state.getBlock() instanceof CableBlock cb && cb.getKind().canBeColored(cb.getInsulation());
    }

    public boolean recolor(DyeColor dyeColor) {
        int newColor = dyeColor == DyeColor.BLACK ? -1 : dyeColor.getId();
        return setColor(newColor);
    }

    public boolean setColor(int color) {
        int clamped = color < 0 ? -1 : DyeColor.byId(color).getId();
        if (clamped >= 0 && !canBeColored()) {
            clamped = -1;
        }
        if (this.color == clamped) {
            return false;
        }
        this.color = clamped;
        markDirty();
        sync();
        return true;
    }

    public void refreshColorValidity() {
        if (this.color >= 0 && !canBeColored()) {
            this.color = -1;
            markDirty();
            sync();
        }
    }

    public static boolean colorsInteract(int firstColor, int secondColor) {
        return firstColor < 0 || secondColor < 0 || firstColor == secondColor;
    }

    public int getOxidationLevel() {
        return oxidationLevel;
    }

    public void setOxidationLevel(int level) {
        int clamped = Math.max(0, Math.min(3, level));
        if (this.oxidationLevel != clamped) {
            this.oxidationLevel = clamped;
            markDirty();
            sync();
        }
    }

    public static double oxidationLossMultiplier(int level) {
        return switch (Math.max(0, Math.min(3, level))) {
            case 1 -> 2.0;
            case 2 -> 3.0;
            case 3 -> 10.0;
            default -> 1.0;
        };
    }

    /**
     * Force-sync derived state. IL splitter cables are present in the energy net only while
     * they are not receiving redstone; detector cables start inactive until energy is detected.
     */
    public void refreshDerivedState() {
        World world = getWorld();
        if (world == null || world.isClient) return;
        BlockState state = getCachedState();
        if (state.getBlock() instanceof CableBlock cb) {
            refreshColorValidity();
            if (cb.getKind() == CableKind.SPLITTER) {
                syncSplitterActive(world);
            } else if (cb.getKind() == CableKind.DETECTOR) {
                setDetectorLevels(world, state, false, 0, 0);
            }
        }
    }


    /** Server tick; wired from {@link CableBlock#getTicker}. */
    public static void tick(World world, BlockPos pos, BlockState state, CableBlockEntity be) {
        if (world.isClient) return;
        if (!(state.getBlock() instanceof CableBlock cb)) return;

        CableKind kind = cb.getKind();

        // Splitter: IL unloads the cable from EnergyNet while redstone-powered and reloads it
        // when power is removed. In this port the active flag mirrors that net membership.
        if (kind == CableKind.SPLITTER) {
            be.syncSplitterActive(world);
            return;
        }

        // Detector: IL samples NodeStats every 32 ticks. It does not sum the whole 32-tick
        // window, so we read the latest previous-tick snapshot at the sample moment.
        if (kind != CableKind.DETECTOR) {
            return;
        }

        if (++be.ticker % 32 != 0) {
            return;
        }

        double energy = com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal.get(world)
                .getNodeStats(pos)
                .energyIn();

        boolean newActive = energy > 0.0;
        int newRs = newActive ? 15 : 0;

        // IL: Util.map(energyIn / (conductorBreakdownEnergy - 1), 1, 15), cast to int.
        double denom = Math.max(1.0, cb.getKind().getConductorBreakdownEnergy() - 1.0);
        double ratio = energy / denom;
        if (Double.isNaN(ratio) || ratio < 0.0) ratio = 0.0;
        if (ratio > 1.0) ratio = 1.0;
        int newComp = (int) (ratio * 15.0);

        be.setDetectorLevels(world, state, newActive, newRs, newComp);
    }

    private void syncSplitterActive(World world) {
        boolean newActive = !world.isReceivingRedstonePower(pos);
        if (!setActiveInternal(newActive)) {
            return;
        }

        // IL removes/loads the splitter energy tile when redstone toggles. In this port that
        // means both the EU graph and all neighboring cable render states must be refreshed.
        com.shipovskijkorp.industriallegacy.energy.net.EuNetwork.invalidate(world, pos);
        for (net.minecraft.util.math.Direction direction : net.minecraft.util.math.Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            com.shipovskijkorp.industriallegacy.energy.net.EuNetwork.invalidate(world, neighborPos);

            BlockState neighborState = world.getBlockState(neighborPos);
            world.updateListeners(neighborPos, neighborState, neighborState, Block.NOTIFY_ALL);
        }

        sync();
    }

    private void setDetectorLevels(World world, BlockState state, boolean newActive, int newRs, int newComp) {
        boolean changed = false;
        if (this.active != newActive) {
            this.active = newActive;
            changed = true;
        }
        if (this.redstoneLevel != newRs) {
            this.redstoneLevel = newRs;
            changed = true;
        }
        if (this.comparatorLevel != newComp) {
            this.comparatorLevel = newComp;
            changed = true;
        }

        if (!changed) {
            return;
        }

        markDirty();
        sync();

        Block block = state.getBlock();
        world.updateNeighborsAlways(pos, block);
        world.updateComparators(pos, block);
    }

    private boolean setActiveInternal(boolean newActive) {
        if (this.active == newActive) return false;
        this.active = newActive;
        markDirty();
        return true;
    }

    private void sync() {
        if (world == null) return;
        BlockState s = getCachedState();
        world.updateListeners(pos, s, s, Block.NOTIFY_ALL);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lastTransferredEu = nbt.getLong("lastTransferredEu");
        this.active = nbt.getBoolean("active");
        this.redstoneLevel = nbt.getInt("rs");
        this.comparatorLevel = nbt.getInt("cmp");
        int storedColor = nbt.contains("color") ? nbt.getInt("color") : -1;
        this.color = storedColor < 0 ? -1 : DyeColor.byId(storedColor).getId();
        this.ticker = nbt.getInt("ticker");
        this.oxidationLevel = nbt.getInt("ox");
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("lastTransferredEu", this.lastTransferredEu);
        nbt.putBoolean("active", this.active);
        nbt.putInt("rs", this.redstoneLevel);
        nbt.putInt("cmp", this.comparatorLevel);
        nbt.putInt("color", this.color);
        nbt.putInt("ticker", this.ticker);
        nbt.putInt("ox", this.oxidationLevel);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}