package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Serializer for IC2 transformer special recipes.
 *
 * JSON format:
 * {
 *   "type": "industrial_legacy:transformer",
 *   "variant": "lv",
 *   "category": "redstone",
 *   "result": { "item": "industrial_legacy:lv_transformer" }
 * }
 */
public final class TransformerRecipeSerializer implements RecipeSerializer<TransformerRecipe> {
    @Override
    public TransformerRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.REDSTONE;
        if (json.has("category")) {
            String rawCategory = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(rawCategory.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.REDSTONE;
            }
        }

        TransformerRecipe.Variant variant = TransformerRecipe.Variant.fromId(JsonHelper.getString(json, "variant"));

        ItemStack result = defaultResult(variant);
        if (json.has("result")) {
            result = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
        }

        return new TransformerRecipe(id, category, variant, result);
    }

    @Override
    public TransformerRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        TransformerRecipe.Variant variant = buf.readEnumConstant(TransformerRecipe.Variant.class);
        ItemStack result = buf.readItemStack();
        return new TransformerRecipe(id, category, variant, result);
    }

    @Override
    public void write(PacketByteBuf buf, TransformerRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        buf.writeEnumConstant(recipe.variant());
        buf.writeItemStack(recipe.resultStack());
    }

    private static ItemStack defaultResult(TransformerRecipe.Variant variant) {
        return switch (variant) {
            case LV -> ModBlocks.LV_TRANSFORMER.asItem().getDefaultStack();
            case MV -> ModBlocks.MV_TRANSFORMER.asItem().getDefaultStack();
            case HV -> ModBlocks.HV_TRANSFORMER.asItem().getDefaultStack();
            case EV -> ModBlocks.EV_TRANSFORMER.asItem().getDefaultStack();
        };
    }
}
