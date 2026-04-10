package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
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

        int inputCount = JsonHelper.getInt(json, "input_count", 1);

        JsonObject res = JsonHelper.getObject(json, "result");
        Identifier itemId = new Identifier(JsonHelper.getString(res, "item"));
        int count = JsonHelper.getInt(res, "count", 1);
        ItemStack out = new ItemStack(Registries.ITEM.get(itemId), count);
        if (res.has("nbt")) {
            try {
                out.setNbt(StringNbtReader.parse(JsonHelper.getString(res, "nbt")));
            } catch (CommandSyntaxException e) {
                throw new JsonParseException("Invalid result.nbt in compressor recipe " + id, e);
            }
        }

        int ticks = JsonHelper.getInt(json, "ticks", 300);
        String requiredFluid = json.has("required_fluid") ? JsonHelper.getString(json, "required_fluid") : null;
        return new CompressorRecipe(id, ing, inputCount, out, ticks, requiredFluid);
    }

    @Override
    public CompressorRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient ing = Ingredient.fromPacket(buf);
        int inputCount = buf.readVarInt();
        ItemStack out = buf.readItemStack();
        int ticks = buf.readVarInt();
        String requiredFluid = buf.readBoolean() ? buf.readString() : null;
        return new CompressorRecipe(id, ing, inputCount, out, ticks, requiredFluid);
    }

    @Override
    public void write(PacketByteBuf buf, CompressorRecipe recipe) {
        recipe.getIngredient().write(buf);
        buf.writeVarInt(recipe.getIngredientCount());
        buf.writeItemStack(recipe.getOutputStack());
        buf.writeVarInt(recipe.getTicks());
        buf.writeBoolean(recipe.getRequiredFluid() != null);
        if (recipe.getRequiredFluid() != null) buf.writeString(recipe.getRequiredFluid());
    }
}
