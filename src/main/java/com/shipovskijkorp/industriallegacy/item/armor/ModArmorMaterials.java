package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Utility armor materials.
 *
 * IC2 armor items are based on diamond armor, so these materials expose diamond-like
 * base protection/toughness while specific mechanics are handled in item logic and mixins.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    private static final Map<ArmorItem.Type, Integer> DIAMOND_PROTECTION = Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
    );

    public static final ArmorMaterial NIGHTVISION = utilityMaterial("nightvision", 27, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial JETPACK = utilityMaterial("jetpack", 27, SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial HAZMAT = utilityMaterial("hazmat", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial RUBBER_BOOTS = utilityMaterial("rubber_boots", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);

    private static ArmorMaterial utilityMaterial(String name, int durability, SoundEvent equipSound) {
        return new ArmorMaterial() {
            @Override
            public int getDurability(ArmorItem.Type type) {
                return durability;
            }

            @Override
            public int getProtection(ArmorItem.Type type) {
                return DIAMOND_PROTECTION.getOrDefault(type, 0);
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
                return 2.0F;
            }

            @Override
            public float getKnockbackResistance() {
                return 0.0F;
            }
        };
    }
}
