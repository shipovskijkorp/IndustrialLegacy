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

    // Detector-only: accumulated energy in the current 32-tick window.
    private double energyInWindow = 0.0;
    private int ticker = 0;

    private boolean active = true;
    private int redstoneLevel = 0;
    private int comparatorLevel = 0;

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

    public int getRedstoneLevel() {
        return redstoneLevel;
    }

    public int getComparatorLevel() {
        return comparatorLevel;
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
     * Force-sync derived state (mainly for splitter cables right after placement).
     */
    public void refreshDerivedState() {
        World world = getWorld();
        if (world == null || world.isClient) return;
        BlockState state = getCachedState();
        if (state.getBlock() instanceof CableBlock cb) {
            if (cb.getKind() == CableKind.SPLITTER) {
                boolean newActive = !world.isReceivingRedstonePower(pos);
                if (setActiveInternal(newActive)) {
                    com.shipovskijkorp.industriallegacy.energy.EuNetwork.invalidate(world, pos);
                    sync();
                }
            }
        }
    }


    /** Server tick; wired from {@link CableBlock#getTicker}. */
    public static void tick(World world, BlockPos pos, BlockState state, CableBlockEntity be) {
        if (world.isClient) return;
        if (!(state.getBlock() instanceof CableBlock cb)) return;

        CableKind kind = cb.getKind();

        // Splitter: active state is purely redstone-controlled (matches IL load/unload toggle).
        if (kind == CableKind.SPLITTER) {
            boolean newActive = !world.isReceivingRedstonePower(pos);
            if (be.setActiveInternal(newActive)) {
                be.sync();
            }
            return;
        }

        // Detector: every 32 ticks, emit redstone if there was energy input.
        if (kind != CableKind.DETECTOR) {
            return;
        }

        // Accumulate energy-in from the previous tick's NodeStats snapshot.
        // (END_WORLD_TICK snapshot avoids ordering issues.)
        var stats = com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal.get(world).getNodeStats(pos);
        if (stats.energyIn() > 0.0) be.energyInWindow += stats.energyIn();

        if (++be.ticker % 32 != 0) {
            return;
        }

        double energy = be.energyInWindow;
        be.energyInWindow = 0.0;

        boolean newActive = energy > 0.0;
        int newRs = newActive ? 15 : 0;

        // IL: comparator level ~= map(energyIn / (breakdownEnergy - 1), 1 -> 15)
        // where breakdownEnergy = capacity + 1, thus (breakdownEnergy - 1) = capacity.
        double denom = Math.max(1.0, cb.getKind().getConductorBreakdownEnergy() - 1.0);
        double ratio = energy / denom;
        if (Double.isNaN(ratio) || ratio < 0.0) ratio = 0.0;
        if (ratio > 1.0) ratio = 1.0;
        int newComp = (int) (ratio * 15.0);

        boolean changed = false;
        if (be.active != newActive) {
            be.active = newActive;
            changed = true;
        }
        if (be.redstoneLevel != newRs) {
            be.redstoneLevel = newRs;
            changed = true;
        }
        if (be.comparatorLevel != newComp) {
            be.comparatorLevel = newComp;
            changed = true;
        }

        if (changed) {
            be.markDirty();
            be.sync();

            Block b = state.getBlock();
            world.updateNeighborsAlways(pos, b);
            world.updateComparators(pos, b);
        }
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
        this.energyInWindow = nbt.getDouble("energyInWindow");
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
        nbt.putDouble("energyInWindow", this.energyInWindow);
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