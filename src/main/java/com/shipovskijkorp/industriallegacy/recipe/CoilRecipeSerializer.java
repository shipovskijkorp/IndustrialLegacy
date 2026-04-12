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

public final class CoilRecipeSerializer implements RecipeSerializer<CoilRecipe> {
    @Override
    public CoilRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            try {
                category = CraftingRecipeCategory.valueOf(JsonHelper.getString(json, "category").toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModItems.COIL.getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new CoilRecipe(id, category, result);
    }

    @Override
    public CoilRecipe read(Identifier id, PacketByteBuf buf) {
        return new CoilRecipe(id, buf.readEnumConstant(CraftingRecipeCategory.class), buf.readItemStack());
    }

    @Override
    public void write(PacketByteBuf buf, CoilRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
