package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.util.PlayerInputStateManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * Quantum boots jump boost.
 */
public final class QuantumBootsItem extends QuantumArmorItem {
    private static final String NBT_WAS_ON_GROUND = "wasOnGround";
    private static final String NBT_JUMP_CHARGE = "jumpCharge";

    public QuantumBootsItem(Settings settings) {
        super(Type.BOOTS, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.FEET) != stack) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        boolean jump = PlayerInputStateManager.isJump(player);
        boolean boost = PlayerInputStateManager.isBoost(player);

        boolean wasOnGround = !nbt.contains(NBT_WAS_ON_GROUND) || nbt.getBoolean(NBT_WAS_ON_GROUND);
        if (wasOnGround && !player.isOnGround() && jump && boost && canUse(stack, 4000)) {
            drainIgnoreLimit(stack, 4000, false);
        }
        if (player.isOnGround() != wasOnGround) {
            nbt.putBoolean(NBT_WAS_ON_GROUND, player.isOnGround());
        }

        if (canUse(stack, 4000) && player.isOnGround()) {
            nbt.putFloat(NBT_JUMP_CHARGE, 1.0f);
        }

        float jumpCharge = nbt.getFloat(NBT_JUMP_CHARGE);
        if (player.getVelocity().y >= 0.0 && jumpCharge > 0.0f && !player.isTouchingWater()) {
            if (jump && boost) {
                if (jumpCharge == 1.0f) {
                    player.setVelocity(player.getVelocity().x * 3.5, player.getVelocity().y, player.getVelocity().z * 3.5);
                }
                player.setVelocity(player.getVelocity().x, player.getVelocity().y + jumpCharge * 0.3f, player.getVelocity().z);
                jumpCharge *= 0.75f;
                nbt.putFloat(NBT_JUMP_CHARGE, jumpCharge);
                player.velocityModified = true;
            } else if (jumpCharge < 1.0f) {
                nbt.putFloat(NBT_JUMP_CHARGE, 0.0f);
            }
        }
    }
}
