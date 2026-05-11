package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

public class MaceratorRecipeSerializer implements RecipeSerializer<MaceratorRecipe> {

    @Override
    public MaceratorRecipe read(Identifier id, JsonObject json) {
        if (json.has("ingredient")) {
            return readModern(id, json);
        }

        if (json.has("input") && json.has("output")) {
            return readLegacy(id, json);
        }

        throw new JsonParseException("Macerator recipe " + id + " is missing ingredient/result or input/output");
    }

    private MaceratorRecipe readModern(Identifier id, JsonObject json) {
        Ingredient ing = Ingredient.fromJson(JsonHelper.getObject(json, "ingredient"));

        JsonObject res = JsonHelper.getObject(json, "result");
        Identifier itemId = new Identifier(JsonHelper.getString(res, "item"));
        int count = JsonHelper.getInt(res, "count", 1);
        ItemStack out = new ItemStack(Registries.ITEM.get(itemId), count);

        int ticks = JsonHelper.getInt(json, "ticks", 300);
        int inputCount = JsonHelper.getInt(json, "input_count", 1);
        return new MaceratorRecipe(id, ing, inputCount, out, ticks);
    }

    private MaceratorRecipe readLegacy(Identifier id, JsonObject json) {
        JsonObject in = JsonHelper.getObject(json, "input");
        JsonObject out = JsonHelper.getObject(json, "output");

        Ingredient ing = legacyIngredient(in);
        int inputCount = JsonHelper.getInt(in, "count", 1);

        Identifier outId = normalizeLegacyOutputId(new Identifier(JsonHelper.getString(out, "item")));
        int outCount = JsonHelper.getInt(out, "count", 1);
        ItemStack result = new ItemStack(Registries.ITEM.get(outId), outCount);

        int ticks = JsonHelper.getInt(json, "ticks", 300);
        return new MaceratorRecipe(id, ing, inputCount, result, ticks);
    }

    private Identifier normalizeLegacyOutputId(Identifier id) {
        if (!"industrial_legacy".equals(id.getNamespace())) {
            return id;
        }

        return switch (id.getPath()) {
            case "copper_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_copper_ore");
            case "gold_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_gold_ore");
            case "iron_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_iron_ore");
            case "lead_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_lead_ore");
            case "tin_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_tin_ore");
            case "uranium_crushed_ore" -> new Identifier(id.getNamespace(), "crushed_uranium_ore");
            default -> id;
        };
    }

    private Ingredient legacyIngredient(JsonObject input) {
        String kind = JsonHelper.getString(input, "kind", "item");
        return switch (kind) {
            case "item" -> {
                Identifier id = new Identifier(JsonHelper.getString(input, "item"));
                Item item = Registries.ITEM.get(id);
                if (item == Items.AIR) {
                    throw new JsonParseException("Unknown item in legacy macerator recipe: " + id);
                }
                yield Ingredient.ofItems(item);
            }
            case "oredict" -> oreDictIngredient(JsonHelper.getString(input, "name"));
            default -> throw new JsonParseException("Unsupported legacy macerator input kind: " + kind);
        };
    }

    private Ingredient oreDictIngredient(String name) {
        return switch (name) {
            case "gemDiamond" -> Ingredient.ofItems(Items.DIAMOND);

            case "treeLeaves" -> Ingredient.ofItems(
                    Blocks.OAK_LEAVES.asItem(),
                    Blocks.SPRUCE_LEAVES.asItem(),
                    Blocks.BIRCH_LEAVES.asItem(),
                    Blocks.JUNGLE_LEAVES.asItem(),
                    Blocks.ACACIA_LEAVES.asItem(),
                    Blocks.DARK_OAK_LEAVES.asItem(),
                    Blocks.MANGROVE_LEAVES.asItem(),
                    Blocks.CHERRY_LEAVES.asItem(),
                    Blocks.AZALEA_LEAVES.asItem(),
                    Blocks.FLOWERING_AZALEA_LEAVES.asItem(),
                    ModBlocks.RUBBER_LEAVES.asItem()
            );
            case "treeSapling" -> Ingredient.ofItems(
                    Blocks.OAK_SAPLING.asItem(),
                    Blocks.SPRUCE_SAPLING.asItem(),
                    Blocks.BIRCH_SAPLING.asItem(),
                    Blocks.JUNGLE_SAPLING.asItem(),
                    Blocks.ACACIA_SAPLING.asItem(),
                    Blocks.DARK_OAK_SAPLING.asItem(),
                    Blocks.CHERRY_SAPLING.asItem(),
                    Blocks.MANGROVE_PROPAGULE.asItem(),
                    Blocks.AZALEA.asItem(),
                    Blocks.FLOWERING_AZALEA.asItem(),
                    ModBlocks.RUBBER_SAPLING.asItem()
            );

            case "ingotCopper" -> Ingredient.ofItems(Items.COPPER_INGOT);
            case "ingotTin" -> Ingredient.ofItems(ModItems.TIN_INGOT);
            case "ingotLead" -> Ingredient.ofItems(ModItems.LEAD_INGOT);
            case "ingotSilver" -> Ingredient.ofItems(ModItems.SILVER_INGOT);
            case "ingotBronze" -> Ingredient.ofItems(ModItems.BRONZE_INGOT);
            case "ingotSteel" -> Ingredient.ofItems(ModItems.STEEL_INGOT);

            case "oreCopper" -> Ingredient.ofItems(Blocks.COPPER_ORE.asItem(), Blocks.DEEPSLATE_COPPER_ORE.asItem());
            case "oreTin" -> Ingredient.ofItems(ModBlocks.TIN_ORE.asItem(), ModBlocks.DEEPSLATE_TIN_ORE.asItem());
            case "oreLead" -> Ingredient.ofItems(ModBlocks.LEAD_ORE.asItem(), ModBlocks.DEEPSLATE_LEAD_ORE.asItem());
            case "oreSilver" -> Ingredient.ofItems(ModBlocks.SILVER_ORE.asItem(), ModBlocks.DEEPSLATE_SILVER_ORE.asItem());
            case "oreUranium" -> Ingredient.ofItems(ModBlocks.URANIUM_ORE.asItem(), ModBlocks.DEEPSLATE_URANIUM_ORE.asItem());
            case "oreIron" -> Ingredient.ofItems(Blocks.IRON_ORE.asItem(), Blocks.DEEPSLATE_IRON_ORE.asItem());
            case "oreGold" -> Ingredient.ofItems(Blocks.GOLD_ORE.asItem(), Blocks.DEEPSLATE_GOLD_ORE.asItem());

            case "plateIron" -> Ingredient.ofItems(ModItems.IRON_PLATE);
            case "plateCopper" -> Ingredient.ofItems(ModItems.COPPER_PLATE);
            case "plateTin" -> Ingredient.ofItems(ModItems.TIN_PLATE);
            case "plateLead" -> Ingredient.ofItems(ModItems.LEAD_PLATE);
            case "plateGold" -> Ingredient.ofItems(ModItems.GOLD_PLATE);
            case "plateBronze" -> Ingredient.ofItems(ModItems.BRONZE_PLATE);
            case "plateLapis" -> Ingredient.ofItems(ModItems.LAPIS_PLATE);
            case "plateObsidian" -> Ingredient.ofItems(ModItems.OBSIDIAN_PLATE);

            case "plateDenseBronze" -> Ingredient.ofItems(ModItems.DENSE_BRONZE_PLATE);
            case "plateDenseCopper" -> Ingredient.ofItems(ModItems.DENSE_COPPER_PLATE);
            case "plateDenseGold" -> Ingredient.ofItems(ModItems.DENSE_GOLD_PLATE);
            case "plateDenseIron" -> Ingredient.ofItems(ModItems.DENSE_IRON_PLATE);
            case "plateDenseLapis" -> Ingredient.ofItems(ModItems.DENSE_LAPIS_PLATE);
            case "plateDenseLead" -> Ingredient.ofItems(ModItems.DENSE_LEAD_PLATE);
            case "plateDenseObsidian" -> Ingredient.ofItems(ModItems.DENSE_OBSIDIAN_PLATE);
            case "plateDenseSteel" -> Ingredient.ofItems(ModItems.DENSE_STEEL_PLATE);
            case "plateDenseTin" -> Ingredient.ofItems(ModItems.DENSE_TIN_PLATE);

            default -> throw new JsonParseException("Unsupported IC2 OreDict mapping in macerator recipe: " + name);
        };
    }

    @Override
    public MaceratorRecipe read(Identifier id, PacketByteBuf buf) {
        Ingredient ing = Ingredient.fromPacket(buf);
        int inputCount = buf.readVarInt();
        ItemStack out = buf.readItemStack();
        int ticks = buf.readVarInt();
        return new MaceratorRecipe(id, ing, inputCount, out, ticks);
    }

    @Override
    public void write(PacketByteBuf buf, MaceratorRecipe recipe) {
        recipe.getIngredient().write(buf);
        buf.writeVarInt(recipe.getIngredientCount());
        buf.writeItemStack(recipe.getOutputStack());
        buf.writeVarInt(recipe.getTicks());
    }
}
