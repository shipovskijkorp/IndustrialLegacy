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
 * Serializer for {@link AdvancedReBatteryRecipe}.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:advanced_re_battery",
 *   "category": "misc",
 *   "result": { "item": "industrial_legacy:advanced_re_battery" }
 * }
 */
public final class AdvancedReBatteryRecipeSerializer implements RecipeSerializer<AdvancedReBatteryRecipe> {

    @Override
    public AdvancedReBatteryRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        ItemStack result = ModItems.ADVANCED_RE_BATTERY.getDefaultStack();
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new AdvancedReBatteryRecipe(id, category, result);
    }

    @Override
    public AdvancedReBatteryRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        ItemStack result = buf.readItemStack();
        return new AdvancedReBatteryRecipe(id, category, result);
    }

    @Override
    public void write(PacketByteBuf buf, AdvancedReBatteryRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeItemStack(recipe.resultStack());
    }
}
