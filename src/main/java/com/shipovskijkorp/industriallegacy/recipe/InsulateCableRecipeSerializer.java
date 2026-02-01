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
 * Serializer for {@link InsulateCableRecipe}.
 *
 * JSON format:
 * <pre>
 * {
 *   "type": "industrial_legacy:insulate_cable",
 *   "category": "misc",
 *   "material": {"item":"industrial_legacy:rubber"},
 *   "example": {
 *     "item": "industrial_legacy:cable",
 *     "count": 1,
 *     "kind": "copper",
 *     "insulation": 1
 *   }
 * }
 * </pre>
 *
 * <p>Note: actual output depends on the input cable NBT (kind/insulation). The "example" is for recipe book/JEI.</p>
 */
public final class InsulateCableRecipeSerializer implements RecipeSerializer<InsulateCableRecipe> {

    @Override
    public InsulateCableRecipe read(Identifier id, JsonObject json) {
        CraftingRecipeCategory category = CraftingRecipeCategory.MISC;
        if (json.has("category")) {
            String c = JsonHelper.getString(json, "category");
            try {
                category = CraftingRecipeCategory.valueOf(c.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                category = CraftingRecipeCategory.MISC;
            }
        }

        Ingredient material = json.has("material")
                ? Ingredient.fromJson(JsonHelper.getObject(json, "material"))
                : Ingredient.ofItems(ModItems.RUBBER);

        ItemStack example = defaultExample();
        if (json.has("example")) {
            JsonObject exObj = JsonHelper.getObject(json, "example");
            ItemStack base = ShapedRecipe.outputFromJson(exObj);
            String kindId = JsonHelper.getString(exObj, "kind", "copper");
            int insulation = JsonHelper.getInt(exObj, "insulation", 1);
            example = buildCableResult(base, kindId, insulation);
        }

        return new InsulateCableRecipe(id, category, material, example);
    }

    @Override
    public InsulateCableRecipe read(Identifier id, PacketByteBuf buf) {
        CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
        Ingredient material = Ingredient.fromPacket(buf);
        ItemStack example = buf.readItemStack();
        return new InsulateCableRecipe(id, category, material, example);
    }

    @Override
    public void write(PacketByteBuf buf, InsulateCableRecipe recipe) {
        buf.writeEnumConstant(recipe.getCategory());
        recipe.material().write(buf);
        buf.writeItemStack(recipe.exampleOutput());
    }

    private static ItemStack defaultExample() {
        return CableItem.createStack(ModItems.CABLE, CableKind.COPPER, 1);
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
