package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.entity.projectile.MiningLaserEntity;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IL Experimental mining laser with all original firing modes.
 */
public final class MiningLaserItem extends Item implements IElectricItem, IModeSwitchableItem {
    public static final long CAPACITY_EU = 300_000L;
    public static final long TRANSFER_LIMIT_EU_T = 512L;
    public static final int TIER = 3;

    private static final String NBT_ENERGY = "energy";
    private static final String NBT_MODE = "mode";

    private static final SoundEvent SOUND_MINING = SoundEvent.of(new Identifier("industrial_legacy", "item.mining_laser.mining"));
    private static final SoundEvent SOUND_LOW_FOCUS = SoundEvent.of(new Identifier("industrial_legacy", "item.mining_laser.low_focus"));
    private static final SoundEvent SOUND_LONG_RANGE = SoundEvent.of(new Identifier("industrial_legacy", "item.mining_laser.long_range"));
    private static final SoundEvent SOUND_SCATTER = SoundEvent.of(new Identifier("industrial_legacy", "item.mining_laser.scatter"));
    private static final SoundEvent SOUND_EXPLOSIVE = SoundEvent.of(new Identifier("industrial_legacy", "item.mining_laser.explosive"));

    public MiningLaserItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        Mode mode = Mode.byId(this.getMode(stack));

        if (mode.requiresBlockTarget()) {
            return TypedActionResult.pass(stack);
        }

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (!consumeEnergy(user, stack, mode.euCost)) {
            return TypedActionResult.fail(stack);
        }

        switch (mode) {
            case MINING -> {
                if (shootLaser(world, user, stack, mode, null, user.getRotationVec(1.0f).normalize())) {
                    playModeSound(world, user, mode);
                    return TypedActionResult.success(stack);
                }
            }
            case LOW_FOCUS -> {
                if (shootLaser(world, user, stack, mode, null, user.getRotationVec(1.0f).normalize())) {
                    playModeSound(world, user, mode);
                    return TypedActionResult.success(stack);
                }
            }
            case LONG_RANGE -> {
                if (shootLaser(world, user, stack, mode, null, user.getRotationVec(1.0f).normalize())) {
                    playModeSound(world, user, mode);
                    return TypedActionResult.success(stack);
                }
            }
            case SUPER_HEAT -> {
                if (shootLaser(world, user, stack, mode, null, user.getRotationVec(1.0f).normalize())) {
                    playModeSound(world, user, mode);
                    return TypedActionResult.success(stack);
                }
            }
            case SCATTER -> {
                shootScatter(world, user, stack);
                playModeSound(world, user, mode);
                return TypedActionResult.success(stack);
            }
            case EXPLOSIVE -> {
                if (shootLaser(world, user, stack, mode, null, user.getRotationVec(1.0f).normalize())) {
                    playModeSound(world, user, mode);
                    return TypedActionResult.success(stack);
                }
            }
            default -> {
            }
        }

        return TypedActionResult.fail(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        Mode mode = Mode.byId(this.getMode(stack));
        if (!mode.requiresBlockTarget()) {
            return ActionResult.PASS;
        }

        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }

        Vec3d look = player.getRotationVec(1.0f).normalize();
        double angle = look.dotProduct(new Vec3d(0.0, 1.0, 0.0));

        if (Math.abs(angle) < 1.0 / Math.sqrt(2.0)) {
            if (!consumeEnergy(player, stack, 3000L)) {
                return ActionResult.FAIL;
            }

            Vec3d dir = new Vec3d(look.x, 0.0, look.z).normalize();
            Vec3d start = player.getEyePos();
            start = new Vec3d(start.x, context.getBlockPos().getY() + 0.5, start.z);
            start = adjustStartPos(start, dir);

            boolean fired = shootLaser(context.getWorld(), player, stack, Mode.MINING, start, dir);
            if (!fired) {
                return ActionResult.FAIL;
            }

            if (mode == Mode.THREE_BY_THREE) {
                shootHorizontal3x3(context.getWorld(), player, stack, start, dir);
                playModeSound(context.getWorld(), player, mode);
            } else {
                playModeSound(context.getWorld(), player, Mode.HORIZONTAL);
            }
            return ActionResult.SUCCESS;
        }

        if (mode == Mode.THREE_BY_THREE) {
            if (!consumeEnergy(player, stack, 3000L)) {
                return ActionResult.FAIL;
            }

            Vec3d dir = new Vec3d(0.0, look.y, 0.0).normalize();
            Vec3d start = player.getEyePos();
            start = new Vec3d(context.getBlockPos().getX() + 0.5, start.y, context.getBlockPos().getZ() + 0.5);
            start = adjustStartPos(start, dir);

            boolean fired = shootLaser(context.getWorld(), player, stack, Mode.MINING, start, dir);
            if (!fired) {
                return ActionResult.FAIL;
            }
            shootVertical3x3(context.getWorld(), player, stack, start, dir);
            playModeSound(context.getWorld(), player, mode);
            return ActionResult.SUCCESS;
        }

        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.sendMessage(Text.translatable("message.industrial_legacy.mining_laser.angle_too_steep"), true);
        }
        return ActionResult.FAIL;
    }

    private void shootScatter(World world, PlayerEntity user, ItemStack stack) {
        Vec3d look = user.getRotationVec(1.0f).normalize();
        Vec3d right = look.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (right.lengthSquared() < 1.0E-4) {
            double angle = Math.toRadians(user.getYaw()) - Math.PI / 2.0;
            right = new Vec3d(Math.sin(angle), 0.0, -Math.cos(angle));
        } else {
            right = right.normalize();
        }
        Vec3d up = right.crossProduct(look).normalize();
        Vec3d base = look.multiply(8.0);

        for (int r = -2; r <= 2; r++) {
            for (int u = -2; u <= 2; u++) {
                Vec3d dir = base.add(right.multiply(r)).add(up.multiply(u)).normalize();
                shootLaser(world, user, stack, Mode.SCATTER, null, dir);
            }
        }
    }

    private void shootHorizontal3x3(World world, PlayerEntity player, ItemStack stack, Vec3d start, Vec3d dir) {
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y - 1.0, start.z), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y + 1.0, start.z), dir);

        Direction facing = player.getHorizontalFacing();
        if (facing == Direction.SOUTH || facing == Direction.NORTH) {
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y, start.z), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y, start.z), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y - 1.0, start.z), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y - 1.0, start.z), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y + 1.0, start.z), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y + 1.0, start.z), dir);
        }
        if (facing == Direction.EAST || facing == Direction.WEST) {
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y, start.z - 1.0), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y, start.z + 1.0), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y - 1.0, start.z - 1.0), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y - 1.0, start.z + 1.0), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y + 1.0, start.z - 1.0), dir);
            shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y + 1.0, start.z + 1.0), dir);
        }
    }

    private void shootVertical3x3(World world, PlayerEntity player, ItemStack stack, Vec3d start, Vec3d dir) {
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y, start.z), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y, start.z), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y, start.z + 1.0), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y, start.z - 1.0), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x + 1.0, start.y, start.z - 1.0), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x - 1.0, start.y, start.z + 1.0), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y, start.z + 1.0), dir);
        shootLaser(world, player, stack, Mode.MINING, new Vec3d(start.x, start.y, start.z - 1.0), dir);
    }

    private boolean shootLaser(World world, PlayerEntity user, ItemStack stack, Mode mode, @Nullable Vec3d startOverride, Vec3d direction) {
        Vec3d dir = direction.normalize();
        Vec3d start = startOverride != null ? startOverride : adjustStartPos(user.getEyePos(), dir);
        MiningLaserEntity laser = new MiningLaserEntity(world, user, start, dir, mode.range, mode.power, mode.blockBreaks, mode.explosive, mode.smelt);
        return world.spawnEntity(laser);
    }

    private static Vec3d adjustStartPos(Vec3d pos, Vec3d dir) {
        return pos.add(dir.multiply(0.2));
    }

    private boolean consumeEnergy(PlayerEntity user, ItemStack stack, long amount) {
        if (user.getAbilities().creativeMode) {
            return true;
        }
        long energy = this.getEnergy(stack);
        if (energy < amount) {
            return false;
        }
        this.setEnergy(stack, energy - amount);
        return true;
    }

    private void playModeSound(World world, PlayerEntity user, Mode mode) {
        SoundEvent sound = switch (mode) {
            case LOW_FOCUS -> SOUND_LOW_FOCUS;
            case LONG_RANGE -> SOUND_LONG_RANGE;
            case SCATTER, THREE_BY_THREE -> SOUND_SCATTER;
            case EXPLOSIVE -> SOUND_EXPLOSIVE;
            default -> SOUND_MINING;
        };
        world.playSound(null, user.getX(), user.getY(), user.getZ(), sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.industrial_legacy.mode", this.getModeName(stack)).formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        tooltip.add(Text.translatable("item.modifiers.mainhand").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("attribute.modifier.equals.0", "1", Text.translatable("attribute.name.generic.attack_speed")).formatted(Formatting.DARK_GREEN));
        tooltip.add(Text.translatable("attribute.modifier.equals.0", "5", Text.translatable("attribute.name.generic.attack_damage")).formatted(Formatting.DARK_GREEN));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, 3)).formatted(Formatting.GRAY));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < CAPACITY_EU;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(((float) getEnergy(stack) / (float) CAPACITY_EU) * 13.0f);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) {
            return 0L;
        }
        return Math.max(0L, Math.min(CAPACITY_EU, nbt.getLong(NBT_ENERGY)));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long clamped = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (clamped == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, clamped);
    }

    @Override
    public long getCapacity(ItemStack stack) {
        return CAPACITY_EU;
    }

    @Override
    public long getTransferLimit(ItemStack stack) {
        return TRANSFER_LIMIT_EU_T;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        int next = (Mode.byId(this.getMode(stack)).id + 1) % Mode.values().length;
        this.setMode(stack, next);
        return next;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Mode.byId(this.getMode(stack)).displayName();
    }

    public int getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_MODE)) {
            return 0;
        }
        return nbt.getInt(NBT_MODE);
    }

    private void setMode(ItemStack stack, int mode) {
        stack.getOrCreateNbt().putInt(NBT_MODE, mode);
    }

    private enum Mode {
        MINING(0, 1250L, Float.POSITIVE_INFINITY, 5.0f, Integer.MAX_VALUE, false, false),
        LOW_FOCUS(1, 100L, 4.0f, 5.0f, 1, false, false),
        LONG_RANGE(2, 5000L, Float.POSITIVE_INFINITY, 20.0f, Integer.MAX_VALUE, false, false),
        HORIZONTAL(3, 0L, Float.POSITIVE_INFINITY, 5.0f, Integer.MAX_VALUE, false, false),
        SUPER_HEAT(4, 2500L, Float.POSITIVE_INFINITY, 8.0f, Integer.MAX_VALUE, false, true),
        SCATTER(5, 10000L, Float.POSITIVE_INFINITY, 12.0f, Integer.MAX_VALUE, false, false),
        EXPLOSIVE(6, 5000L, Float.POSITIVE_INFINITY, 12.0f, Integer.MAX_VALUE, true, false),
        THREE_BY_THREE(7, 7500L, Float.POSITIVE_INFINITY, 5.0f, Integer.MAX_VALUE, false, false);

        final int id;
        final long euCost;
        final float range;
        final float power;
        final int blockBreaks;
        final boolean explosive;
        final boolean smelt;

        Mode(int id, long euCost, float range, float power, int blockBreaks, boolean explosive, boolean smelt) {
            this.id = id;
            this.euCost = euCost;
            this.range = range;
            this.power = power;
            this.blockBreaks = blockBreaks;
            this.explosive = explosive;
            this.smelt = smelt;
        }

        static Mode byId(int id) {
            for (Mode mode : values()) {
                if (mode.id == id) {
                    return mode;
                }
            }
            return MINING;
        }

        boolean requiresBlockTarget() {
            return this == HORIZONTAL || this == THREE_BY_THREE;
        }

        Text displayName() {
            return switch (this) {
                case MINING -> Text.translatable("tooltip.industrial_legacy.mode.mining");
                case LOW_FOCUS -> Text.translatable("tooltip.industrial_legacy.mode.low_focus");
                case LONG_RANGE -> Text.translatable("tooltip.industrial_legacy.mode.long_range");
                case HORIZONTAL -> Text.translatable("tooltip.industrial_legacy.mode.horizontal");
                case SUPER_HEAT -> Text.translatable("tooltip.industrial_legacy.mode.super_heat");
                case SCATTER -> Text.translatable("tooltip.industrial_legacy.mode.scatter");
                case EXPLOSIVE -> Text.translatable("tooltip.industrial_legacy.mode.explosive");
                case THREE_BY_THREE -> Text.translatable("tooltip.industrial_legacy.mode.three_by_three");
            };
        }
    }
}