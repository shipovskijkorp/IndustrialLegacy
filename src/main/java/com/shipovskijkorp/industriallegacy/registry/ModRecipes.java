package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.recipe.HammerPlateRecipe;
import com.shipovskijkorp.industriallegacy.recipe.HammerPlateRecipeSerializer;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Recipe types/serializers.
 */
public final class ModRecipes {
    private ModRecipes() {}

    public static final RecipeType<HammerPlateRecipe> HAMMER_PLATE_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "hammer_plate"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":hammer_plate";
                }
            }
    );

    public static final RecipeSerializer<HammerPlateRecipe> HAMMER_PLATE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "hammer_plate"),
            new HammerPlateRecipeSerializer()
    );

    public static void register() {
        // classload triggers static init
    }
}
