package com.shipovskijkorp.industriallegacy.item.tool;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * IC2 iridium drill: diamond drill tier with permanent Fortune III and a
 * mode switch that toggles Silk Touch while preserving Fortune III.
 */
public final class IridiumDrillItem extends ElectricDrillItem implements IModeSwitchableItem {
    public IridiumDrillItem(Settings settings) {
        super(settings, 800L, 100, 300_000L, 1_000L, 3, 24.0f);
    }

    @Override
    public void onCraft(ItemStack stack, net.minecraft.world.World world, net.minecraft.entity.player.PlayerEntity player) {
        super.onCraft(stack, world, player);
        applyEnchantments(stack, isSilkTouch(stack));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!world.isClient) {
            ensureFortune(stack);
        }
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        boolean silk = !isSilkTouch(stack);
        applyEnchantments(stack, silk);
        return silk ? 1 : 0;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable(isSilkTouch(stack)
                ? "ic2.tooltip.mode.silkTouch"
                : "ic2.tooltip.mode.normal");
    }

    private static boolean isSilkTouch(ItemStack stack) {
        return EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, stack) > 0;
    }

    private static void ensureFortune(ItemStack stack) {
        if (EnchantmentHelper.getLevel(Enchantments.FORTUNE, stack) != 3) {
            applyEnchantments(stack, isSilkTouch(stack));
        }
    }

    private static void applyEnchantments(ItemStack stack, boolean silkTouch) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        enchantments.put(Enchantments.FORTUNE, 3);
        if (silkTouch) {
            enchantments.put(Enchantments.SILK_TOUCH, 1);
        }
        EnchantmentHelper.set(enchantments, stack);
        stack.getOrCreateNbt().putInt("HideFlags", stack.getOrCreateNbt().getInt("HideFlags") | 1);
    }
}
