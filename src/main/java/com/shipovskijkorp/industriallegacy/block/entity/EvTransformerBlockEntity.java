package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.EvTransformerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.EvTransformerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * IL-like LV Transformer.
 *
 * <p>Default mode is redstone controlled. When powered it step-ups LV to MV. When unpowered it
 * step-downs MV to LV. The facing side is the special side.</p>
 */
public class EvTransformerBlockEntity extends BlockEntity implements IEuEnergyStorage, ExtendedScreenHandlerFactory {
    private static final int DEFAULT_TIER = 4; // LV
    private static final long LV_PACKET = EuUtil.powerFromTier(DEFAULT_TIER);
    private static final long MV_PACKET = EuUtil.powerFromTier(DEFAULT_TIER + 1);
    private static final long CAPACITY = LV_PACKET * 8L; // IL Energy component size
    private static final int STEP_DOWN_PACKET_COUNT = 4;

    private enum Mode {
        REDSTONE,
        STEPDOWN,
        STEPUP;

        private static final Mode[] VALUES = values();

        private static Mode byOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                return REDSTONE;
            }
            return VALUES[ordinal];
        }
    }

    private long energy;
    private Mode configuredMode = Mode.REDSTONE;
    @Nullable
    private Mode transformMode;
    private double inputFlow;
    private double outputFlow;
    private int emissionRoundRobin;

    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override
        public int size() {
            return EvTransformerScreenHandler.PROP_COUNT;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> configuredMode.ordinal();
                case 1 -> (int) Math.round(getInputFlowDisplay());
                case 2 -> (int) Math.round(getOutputFlowDisplay());
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                configuredMode = Mode.byOrdinal(value);
            }
        }
    };

    public EvTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EV_TRANSFORMER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, EvTransformerBlockEntity be) {
        if (world.isClient) return;

        be.updateTransformMode(false);
        be.emitEnergy();
    }

    public PropertyDelegate getGuiProperties() {
        return guiProps;
    }

    public void onFacingChanged() {
        if (world == null || world.isClient) return;
        updateTransformMode(true);
    }

    public void handleClientEvent(int event) {
        if (event >= 0 && event < Mode.VALUES.length) {
            configuredMode = Mode.VALUES[event];
            updateTransformMode(false);
        } else if (event == 3) {
            // IL sends event 3 when clicking the current-mode wrench icon. It is intentionally a no-op.
        }
    }

    public int getModeOrdinal() {
        return configuredMode.ordinal();
    }

    public double getInputFlowDisplay() {
        return isDisplayStepUp() ? MV_PACKET : LV_PACKET;
    }

    public double getOutputFlowDisplay() {
        return isDisplayStepUp() ? LV_PACKET : MV_PACKET;
    }

    private void emitEnergy() {
        if (world == null || transformMode == null) return;

        if (isStepUp()) {
            Direction outputSide = getFacing();
            if (energy >= MV_PACKET) {
                long spent = EuNetwork.route(world, pos, this, outputSide, MV_PACKET);
                if (spent > 0) {
                    markDirty();
                }
            }
            return;
        }

        if (energy < LV_PACKET) return;

        Direction facing = getFacing();
        Direction[] directions = Direction.values();
        int sent = 0;
        int start = emissionRoundRobin % directions.length;
        emissionRoundRobin++;

        for (int i = 0; i < directions.length && sent < STEP_DOWN_PACKET_COUNT && energy >= LV_PACKET; i++) {
            Direction direction = directions[(start + i) % directions.length];
            if (direction == facing) continue;

            long spent = EuNetwork.route(world, pos, this, direction, LV_PACKET);
            if (spent > 0) {
                sent++;
                markDirty();
            }
        }
    }

    private void updateTransformMode(boolean force) {
        if (world == null || world.isClient) return;

        Mode newMode = switch (configuredMode) {
            case REDSTONE -> world.isReceivingRedstonePower(pos) ? Mode.STEPUP : Mode.STEPDOWN;
            case STEPDOWN -> Mode.STEPDOWN;
            case STEPUP -> Mode.STEPUP;
        };

        if (!force && transformMode == newMode) {
            return;
        }

        transformMode = newMode;
        inputFlow = EuUtil.powerFromTierD(getActualSinkTier());
        outputFlow = EuUtil.powerFromTierD(getActualSourceTier());
        syncActiveState();
        invalidateNetwork();
        markDirty();
    }

    private void syncActiveState() {
        if (world == null) return;

        BlockState state = getCachedState();
        if (!state.contains(EvTransformerBlock.ACTIVE)) return;

        boolean shouldBeActive = isStepUp();
        if (state.get(EvTransformerBlock.ACTIVE) != shouldBeActive) {
            world.setBlockState(pos, state.with(EvTransformerBlock.ACTIVE, shouldBeActive), Block.NOTIFY_ALL);
        }
    }

    private void invalidateNetwork() {
        if (world == null || world.isClient) return;
        EvTransformerBlock.invalidateAround(world, pos);
    }

    private Direction getFacing() {
        BlockState state = getCachedState();
        if (state.contains(EvTransformerBlock.DOT)) {
            return state.get(EvTransformerBlock.DOT);
        }
        return Direction.NORTH;
    }

    private boolean isStepUp() {
        return transformMode == Mode.STEPUP;
    }

    private boolean isDisplayStepUp() {
        Mode effectiveMode = getEffectiveMode();
        return effectiveMode == Mode.STEPUP;
    }

    private Mode getEffectiveMode() {
        if (configuredMode == Mode.REDSTONE) {
            if (world != null) {
                return world.isReceivingRedstonePower(pos) ? Mode.STEPUP : Mode.STEPDOWN;
            }
            if (transformMode != null) {
                return transformMode;
            }
            return Mode.STEPDOWN;
        }

        return configuredMode;
    }

    private int getActualSinkTier() {
        return isStepUp() ? DEFAULT_TIER : DEFAULT_TIER + 1;
    }

    private int getActualSourceTier() {
        return isStepUp() ? DEFAULT_TIER + 1 : DEFAULT_TIER;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy", energy);
        nbt.putInt("mode", configuredMode.ordinal());
        nbt.putInt("emit_cursor", emissionRoundRobin);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        configuredMode = Mode.byOrdinal(nbt.getInt("mode"));
        emissionRoundRobin = nbt.getInt("emit_cursor");
    }

    @Override
    public long getEuStored() {
        return energy;
    }

    @Override
    public long getEuCapacity() {
        return CAPACITY;
    }

    @Override
    public int getSinkTier() {
        return DEFAULT_TIER + 1;
    }

    @Override
    public int getSourceTier() {
        return DEFAULT_TIER + 1;
    }

    @Override
    public int getSinkTier(Direction side) {
        return canInsert(side) ? getActualSinkTier() : DEFAULT_TIER + 1;
    }

    @Override
    public int getSourceTier(Direction side) {
        return canExtract(side) ? getActualSourceTier() : DEFAULT_TIER + 1;
    }

    @Override
    public boolean isFullEnergyOutput() {
        return true;
    }

    @Override
    public boolean canInsert(Direction from) {
        Direction facing = getFacing();
        return isStepUp() ? from != facing : from == facing;
    }

    @Override
    public boolean canExtract(Direction to) {
        Direction facing = getFacing();
        return isStepUp() ? to == facing : to != facing;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0 || !canInsert(from)) return 0L;

        long packetLimit = EuUtil.powerFromTier(getActualSinkTier());
        long accepted = Math.min(amount, packetLimit);
        accepted = Math.min(accepted, CAPACITY - energy);

        if (!simulate && accepted > 0) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0 || !canExtract(to)) return 0L;

        long packetLimit = EuUtil.powerFromTier(getActualSourceTier());
        long extracted = Math.min(amount, packetLimit);
        extracted = Math.min(extracted, energy);

        if (!simulate && extracted > 0) {
            energy -= extracted;
            markDirty();
        }
        return extracted;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.industrial_legacy.ev_transformer");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new EvTransformerScreenHandler(syncId, inventory, this);
    }
}
