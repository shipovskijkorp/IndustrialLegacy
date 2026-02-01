package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Serializer for {@link BatBoxRecipe}.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:batbox",
 *   "category": "misc",
 *   "result": { "item": "industrial_legacy:batbox" }
 * }
 */
public final class BatBoxRecipeSerializer implements RecipeSerializer<BatBoxRecipe> {

    @Override
    public BatBoxRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }
        ItemStack result = new ItemStack(net.minecraft.item.Items.AIR);
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }
        return new BatBoxRecipe(id, category, result);
    }

    @Override
    public BatBoxRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        return new BatBoxRecipe(id, category, result);
    }

    @Override
    public void write(PacketByteBuf buf, BatBoxRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
