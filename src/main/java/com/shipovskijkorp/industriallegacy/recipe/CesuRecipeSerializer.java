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
 * Serializer for {@link CesuRecipe}.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:cesu",
 *   "category": "misc",
 *   "result": { "item": "industrial_legacy:cesu" }
 * }
 */
public final class CesuRecipeSerializer implements RecipeSerializer<CesuRecipe> {

    @Override
    public CesuRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModBlocks.CESU.asItem().getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new CesuRecipe(id, category, result);
    }

    @Override
    public CesuRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        return new CesuRecipe(id, category, result);
    }

    @Override
    public void write(PacketByteBuf buf, CesuRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
