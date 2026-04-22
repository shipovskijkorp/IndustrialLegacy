package com.shipovskijkorp.industriallegacy.entity.projectile;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * IC2-style mining laser projectile.
 *
 * Behaviour intentionally mirrors the old EntityMiningLaser closely:
 * - flies as a visible projectile
 * - loses 0.5 power per tick of travel
 * - can punch through multiple blocks
 * - mining mode uses power 5 and effectively infinite range
 */
public class MiningLaserEntity extends Entity {
    private static final String NBT_RANGE = "Range";
    private static final String NBT_POWER = "Power";
    private static final String NBT_BREAKS = "BlockBreaks";
    private static final String NBT_OWNER = "Owner";

    private float range = Float.POSITIVE_INFINITY;
    private float power = 0.0f;
    private int blockBreaks = Integer.MAX_VALUE;
    private UUID ownerUuid;
    private Entity cachedOwner;

    public MiningLaserEntity(EntityType<? extends MiningLaserEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public MiningLaserEntity(World world, LivingEntity owner, Vec3d start, Vec3d direction,
                             float range, float power, int blockBreaks) {
        this(ModEntities.MINING_LASER, world);
        this.cachedOwner = owner;
        this.ownerUuid = owner.getUuid();
        this.range = range;
        this.power = power;
        this.blockBreaks = blockBreaks;
        this.refreshPositionAndAngles(start.x, start.y, start.z, owner.getYaw(), owner.getPitch());
        this.setLaserHeading(direction.x, direction.y, direction.z, 1.0);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.range = nbt.getFloat(NBT_RANGE);
        this.power = nbt.getFloat(NBT_POWER);
        this.blockBreaks = nbt.getInt(NBT_BREAKS);
        if (nbt.containsUuid(NBT_OWNER)) {
            this.ownerUuid = nbt.getUuid(NBT_OWNER);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat(NBT_RANGE, this.range);
        nbt.putFloat(NBT_POWER, this.power);
        nbt.putInt(NBT_BREAKS, this.blockBreaks);
        if (this.ownerUuid != null) {
            nbt.putUuid(NBT_OWNER, this.ownerUuid);
        }
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        Vec3d velocity = this.getVelocity();
        if (velocity.lengthSquared() <= 1.0E-7) {
            this.discard();
            return;
        }

        this.updateOrientationFromVelocity(velocity);

        if (this.isTouchingWater()) {
            this.discard();
            return;
        }

        if (!this.getWorld().isClient) {
            if (this.range < 1.0f || this.power <= 0.0f || this.blockBreaks <= 0) {
                this.discard();
                return;
            }

            Vec3d oldPos = this.getPos();
            Vec3d newPos = oldPos.add(velocity);

            HitResult blockHit = this.getWorld().raycast(new RaycastContext(
                    oldPos,
                    newPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    this
            ));

            EntityHitResult entityHit = this.getEntityCollision(oldPos, newPos);
            HitResult hit = pickCloser(oldPos, blockHit, entityHit);

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                switch (hit.getType()) {
                    case ENTITY -> {
                        Entity target = ((EntityHitResult) hit).getEntity();
                        this.hitEntity(target);
                        this.discard();
                        return;
                    }
                    case BLOCK -> this.hitBlock((BlockHitResult) hit);
                    default -> {
                    }
                }
            } else {
                this.power -= 0.5f;
            }
        }

        this.setPosition(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        this.range -= (float) velocity.length();
    }

    private HitResult pickCloser(Vec3d start, HitResult blockHit, EntityHitResult entityHit) {
        if (blockHit == null || blockHit.getType() == HitResult.Type.MISS) {
            return entityHit;
        }
        if (entityHit == null) {
            return blockHit;
        }

        double blockDist = start.squaredDistanceTo(blockHit.getPos());
        double entityDist = start.squaredDistanceTo(entityHit.getPos());
        return entityDist < blockDist ? entityHit : blockHit;
    }

    private EntityHitResult getEntityCollision(Vec3d start, Vec3d end) {
        Box searchBox = this.getBoundingBox().stretch(this.getVelocity()).expand(1.0);
        List<Entity> entities = this.getWorld().getOtherEntities(this, searchBox, this::canHitEntity);

        Entity bestEntity = null;
        Vec3d bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : entities) {
            Box hitBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> intercept = hitBox.raycast(start, end);
            if (intercept.isEmpty()) {
                continue;
            }
            double distance = start.squaredDistanceTo(intercept.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestEntity = entity;
                bestPos = intercept.get();
            }
        }

        return bestEntity != null ? new EntityHitResult(bestEntity, bestPos) : null;
    }

    private boolean canHitEntity(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator() || !entity.isAttackable()) {
            return false;
        }
        Entity owner = this.getOwnerEntity();
        return entity != owner || this.age >= 5;
    }

    private void hitEntity(Entity entity) {
        int damage = Math.max(1, MathHelper.floor(this.power));
        DamageSource source = this.getWorld().getDamageSources().magic();
        entity.setOnFireFor(damage);
        entity.damage(source, damage);
    }

    private void hitBlock(BlockHitResult hit) {
        World world = this.getWorld();
        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isAir()) {
            this.power -= 0.5f;
            return;
        }

        if (state.isOf(Blocks.GLASS) || state.isOf(Blocks.GLASS_PANE) || state.isOf(ModBlocks.REINFORCED_GLASS)) {
            this.power -= 0.5f;
            return;
        }

        float hardness = state.getHardness(world, pos);
        if (hardness < 0.0f) {
            this.discard();
            return;
        }

        this.power -= hardness / 1.5f;
        if (this.power < 0.0f) {
            return;
        }

        Entity owner = this.getOwnerEntity();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.breakBlock(pos, true, owner);
        }
        this.blockBreaks--;
    }

    private void setLaserHeading(double x, double y, double z, double speed) {
        Vec3d dir = new Vec3d(x, y, z).normalize().multiply(speed);
        this.setVelocity(dir);
        this.updateOrientationFromVelocity(dir);
    }

    private void updateOrientationFromVelocity(Vec3d velocity) {
        double motionX = velocity.x;
        double motionY = velocity.y;
        double motionZ = velocity.z;

        float yaw = (float) Math.toDegrees(Math.atan2(motionX, motionZ));
        float pitch = (float) Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));

        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
        this.setYaw(yaw);
        this.setPitch(pitch);
    }

    public Entity getOwnerEntity() {
        if (this.cachedOwner != null && this.cachedOwner.isAlive()) {
            return this.cachedOwner;
        }
        if (this.ownerUuid == null) {
            return null;
        }
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            Entity owner = serverWorld.getEntity(this.ownerUuid);
            if (owner != null) {
                this.cachedOwner = owner;
            }
            return owner;
        }
        return null;
    }
}
