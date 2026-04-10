package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IC2 Experimental Electric Jetpack.
 *
 * Semantics intentionally follow the original IC2 implementation:
 * - 30,000 EU capacity
 * - 60 EU/t transfer limit, tier 1
 * - power 0.7, drop percentage 0.05
 * - hover multiplier 0.1 up/down
 * - world height divisor 1.28
 * - energy cost via discharge(amount + 6): 8 EU normal, 7 EU hover
 */
public final class ElectricJetpackItem extends ArmorItem implements IElectricItem {
    public static final long CAPACITY_EU = 30_000L;
    public static final long TRANSFER_LIMIT_EU_T = 60L;
    public static final int TIER = 1;

    private static final float POWER = 0.7f;
    private static final float DROP_PERCENTAGE = 0.05f;
    private static final float HOVER_UP = 0.1f;
    private static final float HOVER_DOWN = 0.1f;
    private static final float WORLD_HEIGHT_DIVISOR = 1.28f;

    // IC2 uses drainEnergy(amount) -> discharge(amount + 6).
    private static final long EU_HOVER = 7L;
    private static final long EU_NORMAL = 8L;

    private static final String NBT_ENERGY = "energy";
    private static final String NBT_HOVER = "hoverMode";
    private static final String NBT_TOGGLE_TIMER = "toggleTimer";

    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private static final class InputState {
        boolean jump;
        boolean sneak;
        boolean forward;
        int ttl;
    }

    public ElectricJetpackItem(Settings settings) {
        super(ModArmorMaterials.JETPACK, Type.CHESTPLATE, settings.maxCount(1));
    }

    @Override
    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > CAPACITY_EU) e = CAPACITY_EU;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long e = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (e == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, e);
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

    public boolean isHoverModeActive(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().getBoolean(NBT_HOVER);
    }

    public void setHoverModeActive(ItemStack stack, boolean active) {
        if (!active && !stack.hasNbt()) return;
        stack.getOrCreateNbt().putBoolean(NBT_HOVER, active);
    }

    public static void toggleHoverMode(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ElectricJetpackItem jetpack)) {
            player.sendMessage(Text.translatable("message.industrial_legacy.jetpack.no_module").formatted(Formatting.GRAY), true);
            return;
        }

        NbtCompound nbt = chest.getOrCreateNbt();
        int toggleTimer = nbt.getByte(NBT_TOGGLE_TIMER) & 0xFF;
        if (toggleTimer > 0) {
            return;
        }

        boolean active = !jetpack.isHoverModeActive(chest);
        jetpack.setHoverModeActive(chest, active);
        nbt.putByte(NBT_TOGGLE_TIMER, (byte) 10);
        player.sendMessage(Text.translatable(active
                ? "message.industrial_legacy.jetpack.hover_enabled"
                : "message.industrial_legacy.jetpack.hover_disabled").formatted(Formatting.GRAY), true);
    }

    /**
     * Client packets only update the current key-state. Actual thrust is applied during the server world tick,
     * mirroring the old IC2 armor-tick flow much more closely than trying to mutate motion directly inside the packet handler.
     */
    public static void handleInput(ServerPlayerEntity player, boolean jump, boolean sneak, boolean forward) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ElectricJetpackItem)) {
            INPUTS.remove(player.getUuid());
            return;
        }

        InputState state = INPUTS.computeIfAbsent(player.getUuid(), id -> new InputState());
        state.jump = jump;
        state.sneak = sneak;
        state.forward = forward;
        state.ttl = 3;
    }

    public static void tickPlayer(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ElectricJetpackItem jetpack)) {
            INPUTS.remove(player.getUuid());
            return;
        }

        if (chest.hasNbt()) {
            NbtCompound nbt = chest.getNbt();
            int toggleTimer = nbt.getByte(NBT_TOGGLE_TIMER) & 0xFF;
            if (toggleTimer > 0) {
                nbt.putByte(NBT_TOGGLE_TIMER, (byte) (toggleTimer - 1));
            }
        }

        InputState state = INPUTS.get(player.getUuid());
        boolean jump = state != null && state.jump;
        boolean sneak = state != null && state.sneak;
        boolean forward = state != null && state.forward;

        if (state != null) {
            if (state.ttl > 0) {
                state.ttl--;
            }
            if (state.ttl <= 0) {
                state.jump = false;
                state.sneak = false;
                state.forward = false;
            }
        }

        boolean hoverMode = jetpack.isHoverModeActive(chest);
        if (!jump && !hoverMode) {
            return;
        }

        jetpack.useJetpack(player, chest, jump, sneak, forward, hoverMode);

        if (player.isOnGround() && hoverMode) {
            jetpack.setHoverModeActive(chest, false);
            player.sendMessage(Text.translatable("message.industrial_legacy.jetpack.hover_disabled").formatted(Formatting.GRAY), true);
        }
    }

    private boolean useJetpack(ServerPlayerEntity player, ItemStack stack, boolean jump, boolean sneak, boolean forward, boolean hoverMode) {
        double chargeLevel = getChargeLevel(stack);
        if (chargeLevel <= 0.0) return false;

        float power = POWER;
        if (chargeLevel <= DROP_PERCENTAGE) {
            power *= (float) (chargeLevel / DROP_PERCENTAGE);
        }

        if (forward) {
            // IC2: retruster = hover ? 1.0 : 0.15, forwardpower = power * retruster * 2.0,
            // moveRelative(..., 0.4 * forwardpower, 0.02). This reproduces the same scale directly as velocity.
            float forwardPower = power * (hoverMode ? 1.0f : 0.15f) * 2.0f;
            float amount = 0.4f * forwardPower;
            float yawRad = player.getYaw() * 0.017453292f;
            double addX = -MathHelper.sin(yawRad) * amount * 0.02f;
            double addZ = MathHelper.cos(yawRad) * amount * 0.02f;
            player.addVelocity(addX, 0.0, addZ);
        }

        int worldHeight = player.getWorld().getTopY();
        int maxFlightHeight = (int) (worldHeight / WORLD_HEIGHT_DIVISOR);
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power *= (float) ((maxFlightHeight - y) / 25.0);
        }

        Vec3d velocity = player.getVelocity();
        double prevMotionY = velocity.y;
        double motionY = Math.min(velocity.y + power * 0.2f, 0.6000000238418579D);

        if (hoverMode) {
            double maxHoverY = 0.0D;
            if (jump) maxHoverY += HOVER_UP;
            if (sneak) maxHoverY -= HOVER_DOWN;

            if (motionY > maxHoverY) {
                motionY = maxHoverY;
                if (prevMotionY > motionY) {
                    motionY = prevMotionY;
                }
            }
        }

        if (!player.isOnGround()) {
            long cost = hoverMode ? EU_HOVER : EU_NORMAL;
            if (drainIgnoreLimit(stack, cost, true) < cost) {
                return false;
            }
            drainIgnoreLimit(stack, cost, false);
        }

        player.setVelocity(player.getVelocity().x, motionY, player.getVelocity().z);
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));
        player.fallDistance = 0.0f;
        return true;
    }

    private double getChargeLevel(ItemStack stack) {
        return (double) getEnergy(stack) / (double) CAPACITY_EU;
    }

    private static long drainIgnoreLimit(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        long cap = getCapacity(stack);
        if (cap <= 0L) return false;
        return getEnergy(stack) < cap;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        double r = (double) getEnergy(stack) / (double) CAPACITY_EU;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, 3)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(isHoverModeActive(stack)
                ? "message.industrial_legacy.jetpack.state_hover_on"
                : "message.industrial_legacy.jetpack.state_hover_off").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", TIER).formatted(Formatting.DARK_GRAY));
    }
}
