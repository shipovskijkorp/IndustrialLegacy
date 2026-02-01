package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Serializer for {@link CutterCableRecipe}.
 *
 * JSON format:
 * <pre>
 * {
 *   "type": "industrial_legacy:cutter_cable",
 *   "category": "misc",
 *   "tool": {"item":"industrial_legacy:cutter"},
 *   "material": {"item":"industrial_legacy:copper_plate"},
 *   "result": {
 *     "item": "industrial_legacy:cable",
 *     "count": 2,
 *     "kind": "copper",
 *     "insulation": 0
 *   }
 * }
 * </pre>
 */
public final class CutterCableRecipeSerializer implements RecipeSerializer<CutterCableRecipe> {

    @Override
    public CutterCableRecipe read(Identifier id, JsonObject json) {
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
                : Ingredient.ofItems(ModItems.CUTTER);

        Ingredient material = Ingredient.fromJson(JsonHelper.getObject(json, "material"));

        JsonObject resultObj = JsonHelper.getObject(json, "result");
        ItemStack base = ShapedRecipe.outputFromJson(resultObj);

        // Cable variant params are stored next to the standard "item"/"count" fields.
        String kindId = JsonHelper.getString(resultObj, "kind");
        int insulation = JsonHelper.getInt(resultObj, "insulation", 0);

        ItemStack result = buildCableResult(base, kindId, insulation);
        return new CutterCableRecipe(id, category, tool, material, result);
    }

    @Override
    public CutterCableRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        Ingredient tool = Ingredient.fromPacket(buf);
        Ingredient material = Ingredient.fromPacket(buf);
        ItemStack result = buf.readItemStack();
        return new CutterCableRecipe(id, category, tool, material, result);
    }

    @Override
    public void write(PacketByteBuf buf, CutterCableRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        recipe.tool().write(buf);
        recipe.material().write(buf);
        buf.writeItemStack(recipe.resultStack());
    }

    private static ItemStack buildCableResult(ItemStack base, String kindId, int insulation) {
        Item item = base.getItem();
        if (item instanceof CableItem) {
            CableKind kind = CableKind.fromId(kindId);
            ItemStack out = CableItem.createStack(item, kind, insulation);
            out.setCount(base.getCount());
            return out;
        }
        return base;
    }
}
