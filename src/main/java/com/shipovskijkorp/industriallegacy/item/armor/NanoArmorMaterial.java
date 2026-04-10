package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Nano armor should have diamond-like base armor stats, while EU absorption is handled separately.
 */
public final class NanoArmorMaterial implements ArmorMaterial {
    public static final NanoArmorMaterial INSTANCE = new NanoArmorMaterial();

    private static final Map<ArmorItem.Type, Integer> PROTECTION = Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
    );

    private static final Map<ArmorItem.Type, Integer> DURABILITY = Map.of(
            ArmorItem.Type.HELMET, 363,
            ArmorItem.Type.CHESTPLATE, 528,
            ArmorItem.Type.LEGGINGS, 495,
            ArmorItem.Type.BOOTS, 429
    );

    private NanoArmorMaterial() {
    }

    @Override
    public int getDurability(ArmorItem.Type type) {
        return DURABILITY.getOrDefault(type, 0);
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION.getOrDefault(type, 0);
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return IndustrialLegacy.MOD_ID + ":nano";
    }

    @Override
    public float getToughness() {
        return 2.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}
