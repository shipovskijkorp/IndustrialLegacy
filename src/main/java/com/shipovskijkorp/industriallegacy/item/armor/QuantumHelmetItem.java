package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.item.FilledTinCanItem;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Quantum helmet: auto-breathing, auto-feeding, potion cleansing and night vision.
 */
public final class QuantumHelmetItem extends QuantumArmorItem implements INightVisionModule {
    private static final String NBT_NIGHTVISION = "Nightvision";
    private static final Map<net.minecraft.entity.effect.StatusEffect, Integer> POTION_REMOVAL_COST = new HashMap<>();

    static {
        POTION_REMOVAL_COST.put(StatusEffects.POISON, 10_000);
        POTION_REMOVAL_COST.put(StatusEffects.WITHER, 25_000);
    }

    public QuantumHelmetItem(Settings settings) {
        super(Type.HELMET, settings);
    }

    @Override
    public boolean isNightVisionActive(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().getBoolean(NBT_NIGHTVISION);
    }

    @Override
    public void setNightVisionActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean(NBT_NIGHTVISION, active);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.HEAD) != stack) return;

        int air = player.getAir();
        if (canUse(stack, 1000) && air < 100) {
            player.setAir(Math.min(300, air + 200));
            drainIgnoreLimit(stack, 1000, false);
        }

        if (canUse(stack, 1000) && player.getHungerManager().getFoodLevel() < 20) {
            for (int i = 0; i < player.getInventory().main.size(); i++) {
                ItemStack inv = player.getInventory().main.get(i);
                if (!inv.isEmpty() && inv.isOf(ModItems.FILLED_TIN_CAN)) {
                    ItemStack result = FilledTinCanItem.consumeFromStack(player, inv);
                    if (result != inv) {
                        player.getInventory().main.set(i, result);
                        drainIgnoreLimit(stack, 1000, false);
                        break;
                    }
                }
            }
        }

        for (var entry : POTION_REMOVAL_COST.entrySet()) {
            if (player.hasStatusEffect(entry.getKey())) {
                StatusEffectInstance effect = player.getStatusEffect(entry.getKey());
                int cost = entry.getValue() * (effect == null ? 1 : (effect.getAmplifier() + 1));
                if (canUse(stack, cost)) {
                    drainIgnoreLimit(stack, cost, false);
                    player.removeStatusEffect(entry.getKey());
                }
            }
        }

        if (isNightVisionActive(stack) && canUse(stack, 1)) {
            drainIgnoreLimit(stack, 1, false);
            BlockPos pos = player.getBlockPos();
            int skylight = player.getWorld().getLightLevel(pos);
            if (skylight > 8) {
                player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, true, true));
            } else {
                player.removeStatusEffect(StatusEffects.BLINDNESS);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, true));
            }
        }
    }
}
