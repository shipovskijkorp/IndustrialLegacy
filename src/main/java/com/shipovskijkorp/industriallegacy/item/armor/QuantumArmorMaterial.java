package com.shipovskijkorp.industriallegacy.item.armor;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Quantum armor uses EU instead of vanilla durability/armor values.
 */
public final class QuantumArmorMaterial implements ArmorMaterial {
    public static final QuantumArmorMaterial INSTANCE = new QuantumArmorMaterial();

    private static final Map<ArmorItem.Type, Integer> PROTECTION = Map.of(
            ArmorItem.Type.HELMET, 0,
            ArmorItem.Type.CHESTPLATE, 0,
            ArmorItem.Type.LEGGINGS, 0,
            ArmorItem.Type.BOOTS, 0
    );

    private static final Map<ArmorItem.Type, Integer> DURABILITY = Map.of(
            ArmorItem.Type.HELMET, 0,
            ArmorItem.Type.CHESTPLATE, 0,
            ArmorItem.Type.LEGGINGS, 0,
            ArmorItem.Type.BOOTS, 0
    );

    private QuantumArmorMaterial() {}

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
        return "quantum";
    }

    @Override
    public float getToughness() {
        return 0.0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0f;
    }
}
