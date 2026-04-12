package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public final class ElectricMotorRecipeSerializer implements RecipeSerializer<ElectricMotorRecipe> {
    @Override
    public ElectricMotorRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            try {
                category = CraftingRecipeCategory.valueOf(JsonHelper.getString(json, "category").toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModItems.ELECTRIC_MOTOR.getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new ElectricMotorRecipe(id, category, result);
    }

    @Override
    public ElectricMotorRecipe read(Identifier id, PacketByteBuf buf) {
        return new ElectricMotorRecipe(id, buf.readEnumConstant(CraftingRecipeCategory.class), buf.readItemStack());
    }

    @Override
    public void write(PacketByteBuf buf, ElectricMotorRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
