package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.util.PlayerInputStateManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quantum boots jump boost.
 *
 * Server side mirrors the original energy trigger, client side mirrors the original motion logic.
 */
public final class QuantumBootsItem extends QuantumArmorItem {
    private static final String NBT_WAS_ON_GROUND = "wasOnGround";
    private static final Map<UUID, Float> CLIENT_JUMP_CHARGE = new HashMap<>();

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
        if (wasOnGround && !player.isOnGround() && jump && boost) {
            drainIgnoreLimit(stack, 4000, false);
        }
        if (player.isOnGround() != wasOnGround) {
            nbt.putBoolean(NBT_WAS_ON_GROUND, player.isOnGround());
        }
    }

    public static void tickClientPlayer(PlayerEntity player, boolean jump, boolean boost) {
        if (player == null) return;
        ItemStack stack = player.getEquippedStack(EquipmentSlot.FEET);
        if (!(stack.getItem() instanceof QuantumBootsItem boots)) {
            CLIENT_JUMP_CHARGE.remove(player.getUuid());
            return;
        }

        float jumpCharge = CLIENT_JUMP_CHARGE.getOrDefault(player.getUuid(), 0.0f);
        if (boots.canUse(stack, 4000) && player.isOnGround()) {
            jumpCharge = 1.0f;
        }

        if (player.getVelocity().y >= 0.0 && jumpCharge > 0.0f && !player.isTouchingWater()) {
            if (jump && boost) {
                if (jumpCharge == 1.0f) {
                    player.setVelocity(player.getVelocity().x * 3.5, player.getVelocity().y, player.getVelocity().z * 3.5);
                }
                player.setVelocity(player.getVelocity().x, player.getVelocity().y + jumpCharge * 0.3f, player.getVelocity().z);
                jumpCharge *= 0.75f;
            } else if (jumpCharge < 1.0f) {
                jumpCharge = 0.0f;
            }
        }

        if (jumpCharge <= 0.0f) {
            CLIENT_JUMP_CHARGE.remove(player.getUuid());
        } else {
            CLIENT_JUMP_CHARGE.put(player.getUuid(), jumpCharge);
        }
    }
}
