package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Map;

/**
 * Utility armor materials.
 *
 * Armor values are kept explicit so IL can mirror IL item stats on 1.20.1.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    private static final Map<ArmorItem.Type, Integer> DIAMOND_PROTECTION = Map.of(
            ArmorItem.Type.HELMET, 3,
            ArmorItem.Type.CHESTPLATE, 8,
            ArmorItem.Type.LEGGINGS, 6,
            ArmorItem.Type.BOOTS, 3
    );

    private static final Map<ArmorItem.Type, Integer> ZERO_PROTECTION = Map.of(
            ArmorItem.Type.HELMET, 0,
            ArmorItem.Type.CHESTPLATE, 0,
            ArmorItem.Type.LEGGINGS, 0,
            ArmorItem.Type.BOOTS, 0
    );

    public static final ArmorMaterial NIGHTVISION = utilityMaterial("nightvision", 27, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial JETPACK = zeroUtilityMaterial("jetpack", SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial HAZMAT = utilityMaterial("hazmat", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial RUBBER_BOOTS = utilityMaterial("rubber_boots", 64, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial SOLAR = zeroUtilityMaterial("solar", SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial STATIC_BOOTS = zeroUtilityMaterial("rubber", SoundEvents.ITEM_ARMOR_EQUIP_LEATHER);
    public static final ArmorMaterial BATPACK = zeroUtilityMaterial("batpack", SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial ADVANCED_BATPACK = zeroUtilityMaterial("advbatpack", SoundEvents.ITEM_ARMOR_EQUIP_IRON);
    public static final ArmorMaterial ENERGYPACK = zeroUtilityMaterial("energypack", SoundEvents.ITEM_ARMOR_EQUIP_IRON);

    /** IL bronze armor: factor 15, reductions {boots=2, legs=5, chest=6, helmet=2}, enchantability 9. */
    public static final ArmorMaterial BRONZE = exactMaterial(
            "bronze",
            Map.of(
                    ArmorItem.Type.HELMET, 165,
                    ArmorItem.Type.CHESTPLATE, 240,
                    ArmorItem.Type.LEGGINGS, 225,
                    ArmorItem.Type.BOOTS, 195
            ),
            Map.of(
                    ArmorItem.Type.HELMET, 2,
                    ArmorItem.Type.CHESTPLATE, 6,
                    ArmorItem.Type.LEGGINGS, 5,
                    ArmorItem.Type.BOOTS, 2
            ),
            9,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            0.0f,
            0.0f,
            () -> Ingredient.ofItems(ModItems.BRONZE_INGOT)
    );

    /** IL composite vest (alloy chestplate): factor 50, reductions {boots=4, legs=7, chest=9, helmet=4}, enchantability 12, toughness 2. */
    public static final ArmorMaterial ALLOY = exactMaterial(
            "alloy",
            Map.of(
                    ArmorItem.Type.HELMET, 550,
                    ArmorItem.Type.CHESTPLATE, 800,
                    ArmorItem.Type.LEGGINGS, 750,
                    ArmorItem.Type.BOOTS, 650
            ),
            Map.of(
                    ArmorItem.Type.HELMET, 4,
                    ArmorItem.Type.CHESTPLATE, 9,
                    ArmorItem.Type.LEGGINGS, 7,
                    ArmorItem.Type.BOOTS, 4
            ),
            12,
            SoundEvents.ITEM_ARMOR_EQUIP_IRON,
            2.0f,
            0.0f,
            () -> Ingredient.ofItems(ModItems.ADVANCED_ALLOY)
    );

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

    private static ArmorMaterial zeroUtilityMaterial(String name, SoundEvent equipSound) {
        return new ArmorMaterial() {
            @Override
            public int getDurability(ArmorItem.Type type) {
                return 0;
            }

            @Override
            public int getProtection(ArmorItem.Type type) {
                return ZERO_PROTECTION.getOrDefault(type, 0);
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

    private interface IngredientSupplier {
        Ingredient get();
    }

    private static ArmorMaterial exactMaterial(
            String name,
            Map<ArmorItem.Type, Integer> durability,
            Map<ArmorItem.Type, Integer> protection,
            int enchantability,
            SoundEvent equipSound,
            float toughness,
            float knockbackResistance,
            IngredientSupplier repairIngredient
    ) {
        return new ArmorMaterial() {
            @Override
            public int getDurability(ArmorItem.Type type) {
                return durability.getOrDefault(type, 0);
            }

            @Override
            public int getProtection(ArmorItem.Type type) {
                return protection.getOrDefault(type, 0);
            }

            @Override
            public int getEnchantability() {
                return enchantability;
            }

            @Override
            public SoundEvent getEquipSound() {
                return equipSound;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return repairIngredient.get();
            }

            @Override
            public String getName() {
                return IndustrialLegacy.MOD_ID + ":" + name;
            }

            @Override
            public float getToughness() {
                return toughness;
            }

            @Override
            public float getKnockbackResistance() {
                return knockbackResistance;
            }
        };
    }
}
