package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class MetalFormerRecipeSerializer implements RecipeSerializer<MetalFormerRecipe> {
    private final net.minecraft.recipe.RecipeType<?> type;

    public MetalFormerRecipeSerializer(net.minecraft.recipe.RecipeType<?> type) {
        this.type = type;
    }

    @Override
    public MetalFormerRecipe read(Identifier id, JsonObject json) {
        Ingredient ing = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));
        JsonObject res = JsonHelper.getObject(json, "result");
        Identifier itemId = new Identifier(JsonHelper.getString(res, "item"));
        int count = JsonHelper.getInt(res, "count", 1);
        int ticks = JsonHelper.getInt(json, "ticks", 200);
        int inputCount = JsonHelper.getInt(json, "input_count", 1);
        ItemStack out = new ItemStack(Registries.ITEM.get(itemId), count);
        return new MetalFormerRecipe(id, ing, out, ticks, inputCount, type, this);
    }

    @Override
    public MetalFormerRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient ing = Ingredient.fromPacket(buf);
        ItemStack out = buf.readItemStack();
        int ticks = buf.readVarInt();
        int inputCount = buf.readVarInt();
        return new MetalFormerRecipe(id, ing, out, ticks, inputCount, type, this);
    }

    @Override
    public void write(PacketByteBuf buf, MetalFormerRecipe recipe) {
        recipe.getIngredient().write(buf);
        buf.writeItemStack(recipe.getOutputStack());
        buf.writeVarInt(recipe.getTicks());
        buf.writeVarInt(recipe.getInputCount());
    }
}
