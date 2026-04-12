package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class ThermalCentrifugeRecipeSerializer implements RecipeSerializer<ThermalCentrifugeRecipe> {
    @Override
    public ThermalCentrifugeRecipe read(Identifier id, JsonObject json) {
        Ingredient ingredient = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));
        int inputCount = JsonHelper.getInt(json, "input_count", 1);
        int ticks = JsonHelper.getInt(json, "ticks", 500);
        int heat = JsonHelper.getInt(json, "heat", 0);

        List<ItemStack> results = new ArrayList<>();
        if (json.has("results")) {
            JsonArray array = JsonHelper.getArray(json, "results");
            for (int i = 0; i < array.size(); i++) {
                results.add(readStack(JsonHelper.asObject(array.get(i), "result")));
            }
        } else if (json.has("result")) {
            results.add(readStack(JsonHelper.getObject(json, "result")));
        }

        if (results.isEmpty()) {
            throw new JsonParseException("Thermal centrifuge recipe " + id + " has no results");
        }

        return new ThermalCentrifugeRecipe(id, ingredient, inputCount, results, ticks, heat);
    }

    private static ItemStack readStack(JsonObject obj) {
        Identifier itemId = new Identifier(JsonHelper.getString(obj, "item"));
        int count = JsonHelper.getInt(obj, "count", 1);
        return new ItemStack(Registries.ITEM.get(itemId), count);
    }

    @Override
    public ThermalCentrifugeRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient ingredient = Ingredient.fromPacket(buf);
        int inputCount = buf.readVarInt();
        int resultCount = buf.readVarInt();
        List<ItemStack> results = new ArrayList<>(resultCount);
        for (int i = 0; i < resultCount; i++) {
            results.add(buf.readItemStack());
        }
        int ticks = buf.readVarInt();
        int heat = buf.readVarInt();
        return new ThermalCentrifugeRecipe(id, ingredient, inputCount, results, ticks, heat);
    }

    @Override
    public void write(PacketByteBuf buf, ThermalCentrifugeRecipe recipe) {
        recipe.getIngredient().write(buf);
        buf.writeVarInt(recipe.getInputCount());
        List<ItemStack> results = recipe.getResults();
        buf.writeVarInt(results.size());
        for (ItemStack stack : results) {
            buf.writeItemStack(stack);
        }
        buf.writeVarInt(recipe.getTicks());
        buf.writeVarInt(recipe.getHeat());
    }
}
