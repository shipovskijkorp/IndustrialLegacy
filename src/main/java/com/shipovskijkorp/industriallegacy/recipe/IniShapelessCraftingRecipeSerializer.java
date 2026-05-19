package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class IniShapelessCraftingRecipeSerializer implements RecipeSerializer<IniShapelessCraftingRecipe> {
    @Override
    public IniShapelessCraftingRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = readCategory(json);
        JsonArray inputArray = JsonHelper.getArray(json, "inputs");
        List<IlCraftingIngredient> inputs = new ArrayList<>(inputArray.size());
        for (int i = 0; i < inputArray.size(); i++) {
            inputs.add(IlCraftingIngredient.fromJson(inputArray.get(i)));
        }
        ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        return new IniShapelessCraftingRecipe(id, category, inputs, result);
    }

    @Override
    public IniShapelessCraftingRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        int size = buf.readVarInt();
        List<IlCraftingIngredient> inputs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            inputs.add(IlCraftingIngredient.read(buf));
        }
        ItemStack result = buf.readItemStack();
        return new IniShapelessCraftingRecipe(id, category, inputs, result);
    }

    @Override
    public void write(PacketByteBuf buf, IniShapelessCraftingRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeVarInt(recipe.inputs().size());
        for (IlCraftingIngredient input : recipe.inputs()) {
            input.write(buf);
        }
        buf.writeItemStack(recipe.resultStack());
    }

    private static CraftingRecipeCategory readCategory(JsonObject json) {
        if (!json.has("category")) return CraftingRecipeCategory.MISC;
        try {
            return CraftingRecipeCategory.valueOf(JsonHelper.getString(json, "category").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return CraftingRecipeCategory.MISC;
        }
    }
}
