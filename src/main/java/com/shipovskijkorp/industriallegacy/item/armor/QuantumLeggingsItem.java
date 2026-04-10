package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.util.PlayerInputStateManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Quantum leggings speed boost.
 */
public final class QuantumLeggingsItem extends QuantumArmorItem {
    private static final String NBT_SPEED_TICKER = "speedTicker";

    public QuantumLeggingsItem(Settings settings) {
        super(Type.LEGGINGS, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.LEGS) != stack) return;

        boolean forward = PlayerInputStateManager.isForward(player);
        boolean boost = PlayerInputStateManager.isBoost(player) || player.isSprinting();
        if (!canUse(stack, 1000) || !(player.isOnGround() || player.isTouchingWater()) || !forward || !boost) {
            return;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        int speedTicker = nbt.getByte(NBT_SPEED_TICKER) & 0xFF;
        speedTicker++;
        if (speedTicker >= 10) {
            speedTicker = 0;
            drainIgnoreLimit(stack, 1000, false);
        }
        nbt.putByte(NBT_SPEED_TICKER, (byte) speedTicker);

        float speed = 0.22f;
        if (player.isTouchingWater()) {
            speed = 0.1f;
            if (PlayerInputStateManager.isJump(player)) {
                player.setVelocity(player.getVelocity().x, player.getVelocity().y + 0.10000000149011612, player.getVelocity().z);
            }
        }
        player.updateVelocity(speed, new Vec3d(0.0, 0.0, 1.0));
        player.velocityModified = true;
    }
}
