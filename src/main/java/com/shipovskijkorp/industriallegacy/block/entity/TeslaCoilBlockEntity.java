package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.armor.HazmatArmorItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** IC2 Experimental 2.8.222 Tesla coil logic port. */
public class TeslaCoilBlockEntity extends BlockEntity implements IEuEnergyStorage {
    public static final int SCAN_RATE = 32;
    public static final int RANGE = 4;
    public static final long IDLE_ENERGY_PER_TICK = 1L;
    public static final int ENERGY_PER_DAMAGE = 400;
    public static final long ENERGY_CAPACITY = 10_000L;
    public static final int SINK_TIER = 2;

    private long energy;
    private int ticker = ThreadLocalRandom.current().nextInt(SCAN_RATE);

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TESLA_COIL, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TeslaCoilBlockEntity be) {
        if (world.isClient) return;
        be.tickServer(world, pos);
    }

    private void tickServer(World world, BlockPos pos) {
        if (!world.isReceivingRedstonePower(pos)) {
            return;
        }

        if (!useEnergy(IDLE_ENERGY_PER_TICK)) {
            return;
        }

        if (++ticker % SCAN_RATE != 0) {
            markDirty();
            return;
        }

        int damage = (int) (energy / ENERGY_PER_DAMAGE);
        if (damage > 0 && shock(world, pos, damage)) {
            useEnergy((long) damage * ENERGY_PER_DAMAGE);
        }
        markDirty();
    }

    private boolean shock(World world, BlockPos pos, int damage) {
        Box box = new Box(
                pos.getX() - RANGE,
                pos.getY() - RANGE,
                pos.getZ() - RANGE,
                pos.getX() + RANGE + 1,
                pos.getY() + RANGE + 1,
                pos.getZ() + RANGE + 1
        );

        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, entity -> true);
        for (LivingEntity entity : entities) {
            if (HazmatArmorItem.hasCompleteHazmat(entity)) {
                continue;
            }
            if (!entity.damage(world.getDamageSources().lightningBolt(), (float) damage)) {
                continue;
            }

            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(
                        ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        damage,
                        0.5D,
                        1.0D,
                        0.5D,
                        0.1D
                );
            }
            return true;
        }
        return false;
    }

    private boolean useEnergy(long amount) {
        if (amount <= 0L) return true;
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy", energy);
        nbt.putInt("ticker", ticker);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energy = Math.max(0L, Math.min(ENERGY_CAPACITY, nbt.getLong("energy")));
        ticker = nbt.contains("ticker") ? nbt.getInt("ticker") : ThreadLocalRandom.current().nextInt(SCAN_RATE);
    }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return ENERGY_CAPACITY; }
    @Override public int getSinkTier() { return SINK_TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, Math.max(0L, ENERGY_CAPACITY - energy));
        if (!simulate && accepted > 0L) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0L;
    }

    @Override
    public void setStoredEnergyFromItem(long amount) {
        energy = Math.max(0L, Math.min(ENERGY_CAPACITY, amount));
        markDirty();
    }
}
