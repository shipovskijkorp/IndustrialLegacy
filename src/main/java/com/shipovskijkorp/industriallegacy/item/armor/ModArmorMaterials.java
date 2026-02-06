package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

/**
 * Minimal armor materials for utility gear.
 * Nightvision goggles have no protection; material exists mainly to point to the armor texture.
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {}

    public static final ArmorMaterial NIGHTVISION = new ArmorMaterial() {
        @Override
        public int getDurability(net.minecraft.item.ArmorItem.Type type) {
            // IC2 goggles are fragile; keep small.
            return 27;
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
            return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return IndustrialLegacy.MOD_ID + ":nightvision";
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
