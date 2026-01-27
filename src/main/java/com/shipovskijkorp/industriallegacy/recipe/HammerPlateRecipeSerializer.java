package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Serializer for {@link HammerPlateRecipe}.
 *
 * JSON format:
 * <pre>
 * {
 *   "type": "industrial_legacy:hammer_plate",
 *   "category": "misc",
 *   "tool": {"item":"industrial_legacy:forge_hammer"},
 *   "material": {"item":"minecraft:iron_ingot"},
 *   "result": {"item":"industrial_legacy:iron_plate","count":1}
 * }
 * </pre>
 */
public final class HammerPlateRecipeSerializer implements RecipeSerializer<HammerPlateRecipe> {

    @Override
    public HammerPlateRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        Ingredient tool = json.has("tool")
                ? Ingredient.fromJson(JsonHelper.getObject(json, "tool"))
                : Ingredient.ofItems(ModItems.FORGE_HAMMER);

        Ingredient material = Ingredient.fromJson(JsonHelper.getObject(json, "material"));
        ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));

        return new HammerPlateRecipe(id, category, tool, material, result);
    }

    @Override
    public HammerPlateRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        Ingredient tool = Ingredient.fromPacket(buf);
        Ingredient material = Ingredient.fromPacket(buf);
        ItemStack result = buf.readItemStack();
        return new HammerPlateRecipe(id, category, tool, material, result);
    }

    @Override
    public void write(PacketByteBuf buf, HammerPlateRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        recipe.tool().write(buf);
        recipe.material().write(buf);
        buf.writeItemStack(recipe.resultStack());
    }
}
