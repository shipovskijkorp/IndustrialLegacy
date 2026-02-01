package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class CompressorRecipeSerializer implements RecipeSerializer<CompressorRecipe> {

    @Override
    public CompressorRecipe read(Identifier id, JsonObject json) {
        Ingredient ing = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));

        JsonObject res = JsonHelper.getObject(json, "result");
        Identifier itemId = new Identifier(JsonHelper.getString(res, "item"));
        int count = JsonHelper.getInt(res, "count", 1);
        ItemStack out = new ItemStack(Registries.ITEM.get(itemId), count);

        int ticks = JsonHelper.getInt(json, "ticks", 300);
        return new CompressorRecipe(id, ing, out, ticks);
    }

    @Override
    public CompressorRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient ing = Ingredient.fromPacket(buf);
        ItemStack out = buf.readItemStack();
        int ticks = buf.readVarInt();
        return new CompressorRecipe(id, ing, out, ticks);
    }

    @Override
    public void write(PacketByteBuf buf, CompressorRecipe recipe) {
        recipe.getIngredient().write(buf);
        buf.writeItemStack(recipe.getOutputStack());
        buf.writeVarInt(recipe.getTicks());
    }
}
