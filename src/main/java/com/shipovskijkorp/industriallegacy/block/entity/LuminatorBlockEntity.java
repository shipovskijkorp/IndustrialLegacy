package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.LuminatorBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** IC2-style luminator storage + logic. */
public class LuminatorBlockEntity extends BlockEntity implements IEuEnergyStorage {
    private static final long MANUAL_CHARGE_CAPACITY_EU = 10_000L;
    private static final long QUARTERS_PER_EU = 4L;
    private static final long CAPACITY_Q = MANUAL_CHARGE_CAPACITY_EU * QUARTERS_PER_EU;
    private static final long ACTIVE_DRAIN_Q = 1L; // 0.25 EU/t
    private static final int DEFAULT_SINK_TIER = 1;

    private long energyQ = 0L;
    private boolean invertRedstone = false;
    private boolean pendingInitialSupportCheck = true;

    public LuminatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LUMINATOR, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, LuminatorBlockEntity be) {
        if (be.pendingInitialSupportCheck) {
            be.pendingInitialSupportCheck = false;
            if (!isValidSupport(world, pos, be.getFacing())) {
                world.breakBlock(pos, true);
                return;
            }
        }

        long oldEnergyQ = be.energyQ;
        int oldComparator = be.getComparatorOutput();

        boolean active = false;
        if (be.isLitByRedstone() && be.energyQ >= ACTIVE_DRAIN_Q) {
            be.energyQ -= ACTIVE_DRAIN_Q;
            active = true;
        }

        if (state.get(LuminatorBlock.ACTIVE) != active) {
            world.setBlockState(pos, state.with(LuminatorBlock.ACTIVE, active), Block.NOTIFY_LISTENERS);
        }

        if (be.energyQ != oldEnergyQ) {
            be.markDirty();
            int newComparator = be.getComparatorOutput();
            if (newComparator != oldComparator) {
                world.updateComparators(pos, state.getBlock());
            }
        }
    }

    private Direction getFacing() {
        BlockState state = this.getCachedState();
        if (state.contains(LuminatorBlock.FACING)) {
            return state.get(LuminatorBlock.FACING);
        }
        return Direction.NORTH;
    }

    private boolean isLitByRedstone() {
        if (this.world == null) return false;
        return this.world.isReceivingRedstonePower(this.pos) != this.invertRedstone;
    }

    public void toggleInvertRedstone() {
        this.invertRedstone = !this.invertRedstone;
        this.markDirty();
    }

    public long getManualChargeFreeEu() {
        long freeQ = Math.max(0L, CAPACITY_Q - this.energyQ);
        return (freeQ + (QUARTERS_PER_EU - 1L)) / QUARTERS_PER_EU;
    }

    public void addManualCharge(long eu) {
        if (eu <= 0L) return;
        this.energyQ = Math.min(CAPACITY_Q, this.energyQ + eu * QUARTERS_PER_EU);
        this.markDirty();
        if (this.world != null) {
            this.world.updateComparators(this.pos, this.getCachedState().getBlock());
        }
    }

    public int getComparatorOutput() {
        if (CAPACITY_Q <= 0L || this.energyQ <= 0L) return 0;
        if (this.energyQ >= CAPACITY_Q) return 15;
        return Math.max(1, (int) ((this.energyQ * 15L) / CAPACITY_Q));
    }

    public static boolean isValidSupport(BlockView world, BlockPos luminatorPos, Direction facing) {
        BlockPos supportPos = luminatorPos.offset(facing.getOpposite());
        BlockState supportState = world.getBlockState(supportPos);
        if (supportState.isSideSolid(world, supportPos, facing, SideShapeType.FULL)) {
            return true;
        }

        BlockEntity supportBe = world.getBlockEntity(supportPos);

        // IC2 accepts any adjacent IEnergyEmitter, not only a specific output face.
        // In IL the closest equivalents are EU storages/machines/transformers and cables.
        if (supportBe instanceof IEuEnergyStorage) {
            return true;
        }

        return supportBe instanceof CableBlockEntity;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy_q", this.energyQ);
        nbt.putBoolean("invert", this.invertRedstone);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.energyQ = Math.max(0L, Math.min(CAPACITY_Q, nbt.getLong("energy_q")));
        this.invertRedstone = nbt.getBoolean("invert");
        this.pendingInitialSupportCheck = true;
    }

    @Override
    public long getEuStored() {
        return this.energyQ / QUARTERS_PER_EU;
    }

    @Override
    public long getEuCapacity() {
        return MANUAL_CHARGE_CAPACITY_EU;
    }

    @Override
    public int getSinkTier() {
        return DEFAULT_SINK_TIER;
    }

    @Override
    public int getSourceTier() {
        return 0;
    }

    @Override
    public double getDemandedEnergy() {
        return (double) getManualChargeFreeEu();
    }

    @Override
    public double getDemandedEnergy(Direction from) {
        return canInsert(from) ? getDemandedEnergy() : 0.0;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L || !canInsert(from)) return 0L;

        long freeQ = Math.max(0L, CAPACITY_Q - this.energyQ);
        long acceptedEu = Math.min(amount, freeQ / QUARTERS_PER_EU);
        if (!simulate && acceptedEu > 0L) {
            int oldComparator = this.getComparatorOutput();
            this.energyQ += acceptedEu * QUARTERS_PER_EU;
            this.markDirty();
            if (this.world != null && this.getComparatorOutput() != oldComparator) {
                this.world.updateComparators(this.pos, this.getCachedState().getBlock());
            }
        }
        return acceptedEu;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0L;
    }

    @Override
    public boolean canInsert(Direction from) {
        return from == this.getFacing().getOpposite();
    }

    @Override
    public boolean canExtract(Direction to) {
        return false;
    }
}
