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

/**
 * Serializer for {@link ElectronicCircuitRecipe}.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:electronic_circuit",
 *   "category": "misc",
 *   "result": { "item": "industrial_legacy:electronic_circuit" }
 * }
 */
public final class ElectronicCircuitRecipeSerializer implements RecipeSerializer<ElectronicCircuitRecipe> {

    @Override
    public ElectronicCircuitRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModItems.ELECTRONIC_CIRCUIT.getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new ElectronicCircuitRecipe(id, category, result);
    }

    @Override
    public ElectronicCircuitRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        return new ElectronicCircuitRecipe(id, category, result);
    }

    @Override
    public void write(PacketByteBuf buf, ElectronicCircuitRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
