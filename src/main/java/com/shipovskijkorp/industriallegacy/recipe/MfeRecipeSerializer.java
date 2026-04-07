package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Serializer for {@link MfeRecipe}.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:mfe",
 *   "category": "misc",
 *   "result": { "item": "industrial_legacy:mfe" }
 * }
 */
public final class MfeRecipeSerializer implements RecipeSerializer<MfeRecipe> {

    @Override
    public MfeRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String rawCategory = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(rawCategory.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModBlocks.MFE.asItem().getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new MfeRecipe(id, category, result);
    }

    @Override
    public MfeRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        return new MfeRecipe(id, category, result);
    }

    @Override
    public void write(PacketByteBuf buf, MfeRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
