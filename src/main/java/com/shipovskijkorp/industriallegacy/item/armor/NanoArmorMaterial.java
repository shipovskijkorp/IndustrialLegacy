package com.shipovskijkorp.industriallegacy.item.armor;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Nano armor uses EU instead of durability. Vanilla protection is handled by our damage mixin (IC2-like),
 * so this material has 0 protection values and no durability/repair.
 *
 * Armor textures are resolved by {@link #getName()}:
 *  assets/industrial_legacy/textures/models/armor/nano_layer_1.png
 *  assets/industrial_legacy/textures/models/armor/nano_layer_2.png
 */
public final class NanoArmorMaterial implements ArmorMaterial {

    public static final NanoArmorMaterial INSTANCE = new NanoArmorMaterial();

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

    private NanoArmorMaterial() {}

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
        return "nano";
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
