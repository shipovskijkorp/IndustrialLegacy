package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Minimal armor materials for utility gear.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final ArmorMaterial NIGHTVISION = utilityMaterial("nightvision", 27, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial JETPACK = utilityMaterial("jetpack", 27, SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial HAZMAT = utilityMaterial("hazmat", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial RUBBER_BOOTS = utilityMaterial("rubber_boots", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);

    private static ArmorMaterial utilityMaterial(String name, int durability, SoundEvent equipSound) {
        return new ArmorMaterial() {
            @Override
            public int getDurability(net.minecraft.item.ArmorItem.Type type) {
                return durability;
            }

            @Override
            public int getProtection(net.minecraft.item.ArmorItem.Type type) {
                return 0;
            }

            @Override
            public int getEnchantability() {
                return 0;
            }

            @Override
            public SoundEvent getEquipSound() {
                return equipSound;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return Ingredient.EMPTY;
            }

            @Override
            public String getName() {
                return IndustrialLegacy.MOD_ID + ":" + name;
            }

            @Override
            public float getToughness() {
                return 0.0F;
            }

            @Override
            public float getKnockbackResistance() {
                return 0.0F;
            }
        };
    }
}
