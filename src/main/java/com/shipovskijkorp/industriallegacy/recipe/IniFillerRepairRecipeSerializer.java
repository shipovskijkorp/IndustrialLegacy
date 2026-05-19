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

public final class IniFillerRepairRecipeSerializer implements RecipeSerializer<IniFillerRepairRecipe> {
    @Override
    public IniFillerRepairRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = readCategory(json);
        JsonArray inputArray = JsonHelper.getArray(json, "repair_items");
        List<IlCraftingIngredient> repairItems = new ArrayList<>(inputArray.size());
        for (int i = 0; i < inputArray.size(); i++) {
            repairItems.add(IlCraftingIngredient.fromJson(inputArray.get(i)));
        }
        ItemStack target = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "target"));
        int repairAmount = JsonHelper.getInt(json, "repair_amount", 1);
        return new IniFillerRepairRecipe(id, category, repairItems, target, repairAmount);
    }

    @Override
    public IniFillerRepairRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        int size = buf.readVarInt();
        List<IlCraftingIngredient> repairItems = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            repairItems.add(IlCraftingIngredient.read(buf));
        }
        ItemStack target = buf.readItemStack();
        int repairAmount = buf.readVarInt();
        return new IniFillerRepairRecipe(id, category, repairItems, target, repairAmount);
    }

    @Override
    public void write(PacketByteBuf buf, IniFillerRepairRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeVarInt(recipe.repairItems().size());
        for (IlCraftingIngredient input : recipe.repairItems()) {
            input.write(buf);
        }
        buf.writeItemStack(recipe.targetStack());
        buf.writeVarInt(recipe.repairAmount());
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
