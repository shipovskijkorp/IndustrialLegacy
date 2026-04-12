package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public final class CanningRecipeSerializer implements RecipeSerializer<CanningRecipe> {
    @Override
    public CanningRecipe read(Identifier id, JsonObject json) {
        JsonObject containerJson = JsonHelper.getObject(json, "container");
        JsonObject fillJson = JsonHelper.getObject(json, "fill");
        JsonObject resultJson = JsonHelper.getObject(json, "result");

        Ingredient container = Ingredient.fromJson(containerJson);
        Ingredient fill = Ingredient.fromJson(fillJson);
        int containerCount = JsonHelper.getInt(containerJson, "count", 1);
        int fillCount = JsonHelper.getInt(fillJson, "count", 1);
        int ticks = JsonHelper.getInt(json, "ticks", 200);

        Identifier itemId = new Identifier(JsonHelper.getString(resultJson, "item"));
        int count = JsonHelper.getInt(resultJson, "count", 1);
        ItemStack result = new ItemStack(Registries.ITEM.get(itemId), count);
        return new CanningRecipe(id, container, containerCount, fill, fillCount, result, ticks);
    }

    @Override
    public CanningRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient container = Ingredient.fromPacket(buf);
        int containerCount = buf.readVarInt();
        Ingredient fill = Ingredient.fromPacket(buf);
        int fillCount = buf.readVarInt();
        ItemStack result = buf.readItemStack();
        int ticks = buf.readVarInt();
        return new CanningRecipe(id, container, containerCount, fill, fillCount, result, ticks);
    }

    @Override
    public void write(PacketByteBuf buf, CanningRecipe recipe) {
        recipe.getContainer().write(buf);
        buf.writeVarInt(recipe.getContainerCount());
        recipe.getFill().write(buf);
        buf.writeVarInt(recipe.getFillCount());
        buf.writeItemStack(recipe.getResultStack());
        buf.writeVarInt(recipe.getTicks());
    }
}
