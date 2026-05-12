package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public final class CableVariantCraftingRecipeSerializer implements RecipeSerializer<CableVariantCraftingRecipe> {
    @Override
    public CableVariantCraftingRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.REDSTONE;
        if (json.has("category")) {
            try {
                category = CraftingRecipeCategory.valueOf(JsonHelper.getString(json, "category").toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.REDSTONE;
            }
        }

        CableKind kind = CableKind.fromId(JsonHelper.getString(json, "kind"));
        return new CableVariantCraftingRecipe(id, category, kind);
    }

    @Override
    public CableVariantCraftingRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        CableKind kind = buf.readEnumConstant(CableKind.class);
        return new CableVariantCraftingRecipe(id, category, kind);
    }

    @Override
    public void write(PacketByteBuf buf, CableVariantCraftingRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeEnumConstant(recipe.resultKind());
    }
}
