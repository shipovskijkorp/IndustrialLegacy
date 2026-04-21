package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

/**
 * IC2 bronze tool material.
 *
 * Harvest level 2, durability 350, mining speed 6, material attack damage 2, enchantability 13.
 */
public final class BronzeToolMaterial implements ToolMaterial {
    public static final BronzeToolMaterial INSTANCE = new BronzeToolMaterial();

    private BronzeToolMaterial() {
    }

    @Override
    public int getDurability() {
        return 350;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 6.0f;
    }

    @Override
    public float getAttackDamage() {
        return 2.0f;
    }

    @Override
    public int getMiningLevel() {
        return 2;
    }

    @Override
    public int getEnchantability() {
        return 13;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.BRONZE_INGOT);
    }
}
