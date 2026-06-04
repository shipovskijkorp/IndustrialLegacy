package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.TeleporterBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** IC2 Experimental 2.8.222 teleporter behavior port. */
public class TeleporterBlockEntity extends BlockEntity {
    private static final int TARGET_CHECK_PERIOD = 1024;
    private static final int TELEPORT_EVENT_PARTICLES = 20;
    private static final int CLIENT_IDLE_PARTICLES = 2;

    @Nullable private BlockPos target;
    private int targetCheckTicker = ThreadLocalRandom.current().nextInt(TARGET_CHECK_PERIOD);
    private int cooldown;

    public TeleporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORTER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, TeleporterBlockEntity be) {
        if (world.isClient) {
            be.tickClient(world, pos, state);
        } else {
            be.tickServer(world, pos, state);
        }
    }

    private void tickServer(World world, BlockPos pos, BlockState state) {
        boolean coolingDown = cooldown > 0;
        if (coolingDown) {
            cooldown--;
            markDirtyAndSync();
        }

        if (world.isReceivingRedstonePower(pos) && target != null) {
            setActive(state, true);
            List<Entity> entitiesNearby = coolingDown ? Collections.emptyList() : world.getOtherEntities(null,
                    new Box(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                            pos.getX() + 2.0D, pos.getY() + 3.0D, pos.getZ() + 2.0D),
                    entity -> entity.getVehicle() == null);

            if (!entitiesNearby.isEmpty() && verifyTarget()) {
                Entity closest = entitiesNearby.stream()
                        .min(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D)))
                        .orElse(null);
                if (closest != null && target != null) {
                    teleport(closest, Math.sqrt(pos.getSquaredDistance(target)));
                }
            } else if (++targetCheckTicker % TARGET_CHECK_PERIOD == 0) {
                verifyTarget();
            }
        } else {
            setActive(state, false);
        }
    }

    private void tickClient(World world, BlockPos pos, BlockState state) {
        if (!state.contains(TeleporterBlock.LIT) || !state.get(TeleporterBlock.LIT)) return;
        if (cooldown > 0) spawnGreenParticles(world, CLIENT_IDLE_PARTICLES, pos);
        else spawnBlueParticles(world, CLIENT_IDLE_PARTICLES, pos);
    }

    private void setActive(BlockState state, boolean active) {
        if (world == null || !state.contains(TeleporterBlock.LIT) || state.get(TeleporterBlock.LIT) == active) return;
        world.setBlockState(pos, state.with(TeleporterBlock.LIT, active), Block.NOTIFY_ALL);
        markDirtyAndSync();
    }

    private boolean verifyTarget() {
        if (world != null && target != null && world.getBlockEntity(target) instanceof TeleporterBlockEntity) {
            return true;
        }
        target = null;
        updateComparatorLevel();
        if (world != null) setActive(getCachedState(), false);
        return false;
    }

    public void teleport(Entity user, double distance) {
        if (world == null || target == null) return;

        int weight = getWeightOf(user);
        if (weight == 0) return;

        int energyCost = (int) ((double) weight * Math.pow(distance + 10.0D, 0.7D) * 5.0D);
        if (energyCost > getAvailableEnergy()) return;

        consumeEnergy(energyCost);

        double x = target.getX() + 0.5D;
        double y = target.getY() + 1.5D;
        double z = target.getZ() + 0.5D;
        if (user instanceof ServerPlayerEntity player) {
            player.teleport((ServerWorld) world, x, y, z, user.getYaw(), user.getPitch());
        } else {
            user.requestTeleport(x, y, z);
        }
        user.setVelocity(Vec3d.ZERO);
        user.fallDistance = 0.0F;

        BlockEntity be = world.getBlockEntity(target);
        if (be instanceof TeleporterBlockEntity teleporter) {
            teleporter.onTeleportTo(this, user);
        }

        if (world instanceof ServerWorld serverWorld) {
            spawnBlueParticles(serverWorld, TELEPORT_EVENT_PARTICLES, pos);
            spawnBlueParticles(serverWorld, TELEPORT_EVENT_PARTICLES, target);
        }
        markDirtyAndSync();
    }

    private void onTeleportTo(TeleporterBlockEntity from, Entity entity) {
        cooldown = 20;
        markDirtyAndSync();
    }

    private void spawnBlueParticles(World world, int n, BlockPos pos) {
        spawnParticles(world, n, pos, -1.0F, 0.0F, 1.0F);
    }

    private void spawnGreenParticles(World world, int n, BlockPos pos) {
        spawnParticles(world, n, pos, -1.0F, 1.0F, 0.0F);
    }

    private void spawnParticles(World world, int n, BlockPos pos, float red, float green, float blue) {
        DustParticleEffect effect = new DustParticleEffect(new Vector3f(Math.max(0.0F, red), Math.max(0.0F, green), Math.max(0.0F, blue)), 1.0F);
        for (int i = 0; i < n; i++) {
            double x = pos.getX() + world.random.nextFloat();
            double z = pos.getZ() + world.random.nextFloat();
            double y1 = pos.getY() + 1 + world.random.nextFloat();
            double y2 = pos.getY() + 2 + world.random.nextFloat();
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(effect, x, y1, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverWorld.spawnParticles(effect, x, y2, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            } else {
                world.addParticle(effect, x, y1, z, red, green, blue);
                world.addParticle(effect, x, y2, z, red, green, blue);
            }
        }
    }

    public int getAvailableEnergy() {
        if (world == null) return 0;
        long energy = 0L;
        for (Direction dir : Direction.values()) {
            BlockEntity be = world.getBlockEntity(pos.offset(dir));
            if (isCompatibleStorage(be)) {
                energy += ((IEuEnergyStorage) be).getEuStored();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, energy);
    }

    public void consumeEnergy(int amount) {
        if (world == null || amount <= 0) return;
        List<IEuEnergyStorage> energySources = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            BlockEntity be = world.getBlockEntity(pos.offset(dir));
            if (isCompatibleStorage(be)) {
                IEuEnergyStorage storage = (IEuEnergyStorage) be;
                if (storage.getEuStored() > 0L) energySources.add(storage);
            }
        }

        long remaining = amount;
        while (remaining > 0L && !energySources.isEmpty()) {
            long drain = (remaining + energySources.size() - 1L) / energySources.size();
            Iterator<IEuEnergyStorage> iterator = energySources.iterator();
            while (iterator.hasNext() && remaining > 0L) {
                IEuEnergyStorage source = iterator.next();
                long want = Math.min(drain, remaining);
                long extracted = extractFromStorageAnySide(source, want, false);
                if (extracted <= 0L || source.getEuStored() <= 0L) {
                    iterator.remove();
                }
                remaining -= Math.max(0L, extracted);
            }
        }
    }

    private long extractFromStorageAnySide(IEuEnergyStorage storage, long amount, boolean simulate) {
        long remaining = amount;
        long extracted = 0L;
        for (Direction side : Direction.values()) {
            if (remaining <= 0L) break;
            long pulled = storage.extractEu(remaining, side, simulate);
            extracted += pulled;
            remaining -= pulled;
        }
        return extracted;
    }

    private boolean isCompatibleStorage(@Nullable BlockEntity be) {
        return be instanceof BatBoxBlockEntity
                || be instanceof CesuBlockEntity
                || be instanceof MfeBlockEntity
                || be instanceof MfsuBlockEntity;
    }

    public int getWeightOf(Entity user) {
        boolean useInventoryWeight = ILConfig.getBool("balance/teleporterUseInventoryWeight", false);
        int weight = 0;

        if (user instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            weight += getStackCost(stack);
        } else if (user instanceof AnimalEntity || user instanceof AbstractMinecartEntity || user instanceof BoatEntity) {
            weight += 100;
        } else if (user instanceof PlayerEntity player) {
            weight += 1000;
            if (useInventoryWeight) {
                PlayerInventory inv = player.getInventory();
                for (ItemStack stack : inv.main) {
                    weight += getStackCost(stack);
                }
            }
        } else if (user instanceof GhastEntity) {
            weight += 2500;
        } else if (user instanceof WitherEntity) {
            weight += 5000;
        } else if (user instanceof EnderDragonEntity) {
            weight += 10000;
        } else if (user instanceof PathAwareEntity || user instanceof ArmorStandEntity) {
            weight += 500;
        }

        if (useInventoryWeight && user instanceof LivingEntity living) {
            for (ItemStack stack : living.getArmorItems()) {
                weight += getStackCost(stack);
            }
            for (ItemStack stack : living.getHandItems()) {
                weight += getStackCost(stack);
            }
            if (user instanceof PlayerEntity player) {
                weight -= getStackCost(player.getMainHandStack());
            }
        }

        for (Entity passenger : user.getPassengerList()) {
            weight += getWeightOf(passenger);
        }
        return weight;
    }

    private static int getStackCost(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return 100 * stack.getCount() / stack.getMaxCount();
    }

    public boolean hasTarget() {
        return target != null;
    }

    public @Nullable BlockPos getTarget() {
        return target;
    }

    public void setTarget(@Nullable BlockPos pos) {
        target = pos;
        updateComparatorLevel();
        markDirtyAndSync();
    }

    private void updateComparatorLevel() {
        if (world != null) {
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (target != null) {
            nbt.putInt("targetX", target.getX());
            nbt.putInt("targetY", target.getY());
            nbt.putInt("targetZ", target.getZ());
        }
        nbt.putInt("targetCheckTicker", targetCheckTicker);
        nbt.putInt("cooldown", cooldown);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("targetX")) {
            target = new BlockPos(nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ"));
        } else {
            target = null;
        }
        targetCheckTicker = nbt.contains("targetCheckTicker") ? nbt.getInt("targetCheckTicker") : ThreadLocalRandom.current().nextInt(TARGET_CHECK_PERIOD);
        cooldown = nbt.getInt("cooldown");
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
