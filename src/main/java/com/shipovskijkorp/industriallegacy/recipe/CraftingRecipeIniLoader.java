package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.book.CookingRecipeCategory;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Loads IL-style shaped/shapeless/furnace .ini recipes into vanilla recipe tables. */
public final class CraftingRecipeIniLoader {
    private static final String SHAPED_PATH = "data/industrial_legacy/il_recipes/shaped_recipes.ini";
    private static final String SHAPELESS_PATH = "data/industrial_legacy/il_recipes/shapeless_recipes.ini";
    private static final String FURNACE_PATH = "data/industrial_legacy/il_recipes/furnace.ini";
    private static final int FURNACE_COOK_TIME = 200;
    /**
     * The bundled files are intentionally copied from IL and still contain recipes for
     * blocks/items that Industrial Legacy has not ported yet. Those lines are not
     * recipe load failures for the current mod state, so keep them out of discovered/failed
     * stats until the corresponding content exists. Real parse failures for supported
     * recipes are still reported by RecipeLoadTracker.
     */
    private static final Set<String> UNPORTED_RECIPE_TOKENS = Set.of(
            "advanced_miner",
            "advanced_scanner",
            "barrel",
            "batch_crafter",
            "blast_furnace",
            "block_cutter",
            "block_cutting_blade_diamond",
            "block_cutting_blade_iron",
            "block_cutting_blade_steel",
            "broken_rubber_boat",
            "bronze_tank",
            "carbon_boat",
            "chunk_loader",
            "coffee",
            "coin",
            "coke_kiln",
            "coke_kiln_grate",
            "coke_kiln_hatch",
            "cold_coffee",
            "condenser",
            "copper_boiler",
            "cover_pump_lv",
            "cover_pump_mv",
            "crop_harvester",
            "crop_stick",
            "cropmatron",
            "cropnalyzer",
            "crowbar",
            "crystal_memory",
            "dark_coffee",
            "dynamite",
            "dynamite_sticky",
            "electric_boat",
            "electric_heat_generator",
            "electric_kinetic_generator",
            "electrolyzer",
            "emerald_dust",
            "empty",
            "energy_o_mat",
            "fermenter",
            "fluid_distributor",
            "fluid_heat_generator",
            "fluid_regulator",
            "industrial_diamond",
            "industrial_workbench",
            "iodine_tablet",
            "iridium_tank",
            "iron_tank",
            "item_buffer",
            "item_buffer_2",
            "itnt",
            "jetpack_attachment_plate",
            "liquid_heat_exchanger",
            "manual_kinetic_generator",
            "matter_generator",
            "meter",
            "miner",
            "nuke",
            "obscurator",
            "pattern_storage",
            "personal_chest",
            "raw_crystal_memory",
            "rci_lzh",
            "rci_rsh",
            "reactor_access_hatch",
            "reactor_fluid_port",
            "reactor_redstone_port",
            "reactor_vessel",
            "refractory_bricks",
            "reinforced_door",
            "remote",
            "replicator",
            "rt_heat_generator",
            "rubber_boat",
            "scanner",
            "single_use_battery",
            "small_diamond_dust",
            "solid_heat_generator",
            "sorting_machine",
            "steam_generator",
            "steam_kinetic_generator",
            "steam_repressurizer",
            "steam_turbine",
            "steam_turbine_blade",
            "steel_tank",
            "stirling_generator",
            "stirling_kinetic_generator",
            "tank",
            "terraformer",
            "tfbp",
            "tfbp_blank",
            "tfbp_chilling",
            "tfbp_cultivation",
            "tfbp_desertification",
            "tfbp_flatification",
            "tfbp_irrigation",
            "tfbp_mushroom",
            "trade_o_mat",
            "water_generator",
            "weeding_trowel",
            "weighted_fluid_distributor",
            "weighted_item_distributor",
            "wind_generator"
    );


    private CraftingRecipeIniLoader() {}

    private static String category(String resourcePath) {
        return RecipeLoadTracker.categoryName(resourcePath);
    }

    private static String recipeName(String resourcePath, Line line) {
        return category(resourcePath) + ":" + line.number() + " -> " + line.text();
    }

    private static String recipeName(String resourcePath, int number, String raw) {
        return category(resourcePath) + ":" + number + " -> " + raw.trim();
    }

    public static List<Recipe<?>> loadBuiltinRecipes() {
        List<Recipe<?>> recipes = new ArrayList<>();
        recipes.addAll(loadShaped(SHAPED_PATH));
        recipes.addAll(loadShapeless(SHAPELESS_PATH));
        recipes.addAll(loadFurnace(FURNACE_PATH));
        IndustrialLegacy.LOGGER.info("Loaded IL-style crafting .ini recipes: {} total", recipes.size());
        RecipeLoadTracker.logFailuresIfAny();
        return recipes;
    }

    private static List<Recipe<?>> loadShaped(String resourcePath) {
        List<Recipe<?>> recipes = new ArrayList<>();
        List<Line> lines = loadLines(resourcePath);
        String category = category(resourcePath);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            try {
                int equals = line.text.indexOf('=');
                if (equals < 0) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "missing '=' separator");
                    continue;
                }
                String left = line.text.substring(0, equals).trim();
                String right = stripAttributes(line.text.substring(equals + 1).trim());
                Shape shape = parseShape(left);
                ItemStack output = parseOutput(right);
                if (shape == null || output.isEmpty()) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "empty shaped input or output after parsing");
                    continue;
                }
                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/shaped/" + sanitize(output) + "_" + i);
                recipes.add(new IniShapedCraftingRecipe(id, CraftingRecipeCategory.MISC, shape.width, shape.height, shape.inputs, output));
                RecipeLoadTracker.loaded(category);
            } catch (RuntimeException e) {
                RecipeLoadTracker.failed(category, recipeName(resourcePath, line), e);
                IndustrialLegacy.LOGGER.debug("Skipping shaped ini recipe {}:{} -> {}", resourcePath, line.number, e.getMessage());
            }
        }
        return recipes;
    }

    private static List<Recipe<?>> loadShapeless(String resourcePath) {
        List<Recipe<?>> recipes = new ArrayList<>();
        List<Line> lines = loadLines(resourcePath);
        String category = category(resourcePath);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            try {
                if (line.text.contains("@filler")) {
                    Recipe<?> fillerRecipe = parseFillerRepairRecipe(resourcePath, line, i);
                    recipes.add(fillerRecipe);
                    RecipeLoadTracker.loaded(category);
                    continue;
                }
                int equals = line.text.indexOf('=');
                if (equals < 0) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "missing '=' separator");
                    continue;
                }
                String left = stripAttributes(line.text.substring(0, equals).trim());
                String right = stripAttributes(line.text.substring(equals + 1).trim());
                List<IlCraftingIngredient> inputs = parseShapelessInputs(left);
                ItemStack output = parseOutput(right);
                if (inputs.isEmpty() || output.isEmpty()) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "empty shapeless input or output after parsing");
                    continue;
                }
                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/shapeless/" + sanitize(output) + "_" + i);
                recipes.add(new IniShapelessCraftingRecipe(id, CraftingRecipeCategory.MISC, inputs, output));
                RecipeLoadTracker.loaded(category);
            } catch (RuntimeException e) {
                RecipeLoadTracker.failed(category, recipeName(resourcePath, line), e);
                IndustrialLegacy.LOGGER.debug("Skipping shapeless ini recipe {}:{} -> {}", resourcePath, line.number, e.getMessage());
            }
        }
        return recipes;
    }

    private static List<Recipe<?>> loadFurnace(String resourcePath) {
        List<Recipe<?>> recipes = new ArrayList<>();
        List<Line> lines = loadLines(resourcePath);
        String category = category(resourcePath);
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            try {
                int equals = line.text.indexOf('=');
                if (equals < 0) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "missing '=' separator");
                    continue;
                }
                String left = line.text.substring(0, equals).trim();
                String rightRaw = line.text.substring(equals + 1).trim();
                float xp = parseFloatAttribute(rightRaw, "xp", 0.0f);
                String right = stripAttributes(rightRaw);
                IlCraftingIngredient input = parseIngredient(left);
                if (input.isEmpty() || !input.isVanillaOnly()) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "furnace input is empty or requires IL-only matching");
                    continue;
                }
                ItemStack output = parseOutput(right);
                if (output.isEmpty()) {
                    RecipeLoadTracker.failed(category, recipeName(resourcePath, line), "empty furnace output after parsing");
                    continue;
                }
                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/furnace/" + sanitize(left) + "_to_" + sanitize(output) + "_" + i);
                recipes.add(new SmeltingRecipe(id, "", CookingRecipeCategory.MISC, input.asVanillaIngredient(), output, xp, FURNACE_COOK_TIME));
                RecipeLoadTracker.loaded(category);
            } catch (RuntimeException e) {
                RecipeLoadTracker.failed(category, recipeName(resourcePath, line), e);
                IndustrialLegacy.LOGGER.debug("Skipping furnace ini recipe {}:{} -> {}", resourcePath, line.number, e.getMessage());
            }
        }
        return recipes;
    }

    private static List<Line> loadLines(String resourcePath) {
        String category = category(resourcePath);
        RecipeLoadTracker.beginCategory(category);

        List<Line> lines = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(resourcePath);
        if (stream == null) stream = CraftingRecipeIniLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            RecipeLoadTracker.failed(category, resourcePath, "missing ini resource");
            IndustrialLegacy.LOGGER.warn("Missing IL-style crafting recipe ini: {}", resourcePath);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String raw;
            int number = 0;
            while ((raw = reader.readLine()) != null) {
                number++;
                String text = stripComment(raw).trim();
                if (text.isEmpty()) continue;
                RecipeLoadTracker.discovered(category);
                if (referencesUnportedRecipeToken(text)) {
                    String name = recipeName(resourcePath, number, text);
                    RecipeLoadTracker.skipped(category, name, "references unported IL content");
                    IndustrialLegacy.LOGGER.debug("Skipping unported IL recipe {}:{} -> {}", resourcePath, number, text);
                    continue;
                }
                lines.add(new Line(number, text));
            }
        } catch (IOException e) {
            RecipeLoadTracker.failed(category, resourcePath, e);
            IndustrialLegacy.LOGGER.warn("Failed to read IL-style crafting recipe ini {}", resourcePath, e);
        }
        return lines;
    }

    private static Recipe<?> parseFillerRepairRecipe(String resourcePath, Line line, int index) {
        int equals = line.text.indexOf('=');
        if (equals < 0) throw new IllegalArgumentException("missing '=' separator");

        String leftRaw = line.text.substring(0, equals).trim();
        int filler = leftRaw.indexOf("@filler");
        if (filler < 0) throw new IllegalArgumentException("missing @filler marker");

        int repairAmount = parseFillerAmount(leftRaw.substring(filler));
        String left = leftRaw.substring(0, filler).trim();
        String right = stripAttributes(line.text.substring(equals + 1).trim());
        List<IlCraftingIngredient> repairItems = parseShapelessInputs(left);
        ItemStack target = parseOutput(right);
        if (repairItems.isEmpty() || target.isEmpty()) {
            throw new IllegalArgumentException("empty @filler repair input or output after parsing");
        }

        Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/filler/" + sanitize(target) + "_" + index);
        return new IniFillerRepairRecipe(id, CraftingRecipeCategory.MISC, repairItems, target, repairAmount);
    }

    private static int parseFillerAmount(String fillerText) {
        int star = fillerText.indexOf('*');
        if (star < 0) return 1;
        int end = star + 1;
        while (end < fillerText.length() && Character.isDigit(fillerText.charAt(end))) {
            end++;
        }
        try {
            return Math.max(1, Integer.parseInt(fillerText.substring(star + 1, end)));
        } catch (RuntimeException ignored) {
            return 1;
        }
    }

    private static Shape parseShape(String left) {
        int firstQuote = left.indexOf('"');
        int secondQuote = firstQuote < 0 ? -1 : left.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) throw new IllegalArgumentException("missing shaped pattern");

        String patternText = left.substring(firstQuote + 1, secondQuote);
        String defsText = left.substring(secondQuote + 1).trim();
        String[] rows = patternText.split("\\|", -1);
        int height = rows.length;
        int width = 0;
        for (String row : rows) width = Math.max(width, row.length());
        if (width <= 0 || height <= 0 || width > 3 || height > 3) throw new IllegalArgumentException("bad shaped pattern size");

        Map<Character, IlCraftingIngredient> keys = new HashMap<>();
        for (String def : tokenize(defsText)) {
            if (def.startsWith("@")) continue;
            int colon = def.indexOf(':');
            if (colon <= 0) continue;
            char key = def.charAt(0);
            String token = def.substring(colon + 1);
            keys.put(key, parseIngredient(token));
        }

        List<IlCraftingIngredient> inputs = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            String row = rows[y];
            for (int x = 0; x < width; x++) {
                char c = x < row.length() ? row.charAt(x) : ' ';
                if (c == ' ') {
                    inputs.add(IlCraftingIngredient.empty());
                } else {
                    IlCraftingIngredient ingredient = keys.get(c);
                    if (ingredient == null || ingredient.isEmpty()) {
                        throw new IllegalArgumentException("missing or empty ingredient for shaped key '" + c + "'");
                    }
                    inputs.add(ingredient);
                }
            }
        }
        return new Shape(width, height, inputs);
    }

    private static List<IlCraftingIngredient> parseShapelessInputs(String inputText) {
        List<IlCraftingIngredient> inputs = new ArrayList<>();
        for (String token : tokenize(inputText)) {
            if (token.startsWith("@")) continue;
            IlCraftingIngredient ingredient = parseIngredient(token);
            if (!ingredient.isEmpty()) inputs.add(ingredient);
        }
        return inputs;
    }

    private static IlCraftingIngredient parseIngredient(String rawToken) {
        String token = normalizeToken(stripMetadata(rawToken.trim()));
        if (token.isEmpty()) return IlCraftingIngredient.empty();

        if (token.contains("|")) {
            List<IlCraftingIngredient> alternatives = new ArrayList<>();
            for (String part : token.split("\\|")) alternatives.add(parseIngredient(part));
            return IlCraftingIngredient.ofAlternatives(alternatives);
        }

        if (token.startsWith("OreDict:")) return oreDictIngredient(token.substring("OreDict:".length()));
        if (token.startsWith("Tag:")) return tagIngredient(token.substring("Tag:".length()));
        if (token.startsWith("Group:")) return groupIngredient(token.substring("Group:".length()));
        if (token.startsWith("Fluid:")) return fluidIngredient(token.substring("Fluid:".length()));
        if (token.startsWith("FluidCell:")) return IlCraftingIngredient.fluidCell(UniversalFluidCellItem.CellFluid.byId(token.substring("FluidCell:".length())));
        if (token.startsWith("industrial_legacy:cable#")) return cableIngredient(token.substring("industrial_legacy:cable#".length()));
        if (token.startsWith("industrial_legacy:cable#")) return cableIngredient(token.substring("industrial_legacy:cable#".length()));

        Item item = resolveItem(token);
        if (item == null) return IlCraftingIngredient.empty();
        return IlCraftingIngredient.of(Ingredient.ofItems(item));
    }

    private static ItemStack parseOutput(String rawToken) {
        CountedToken counted = splitCount(stripAttributes(rawToken).trim());
        String token = normalizeToken(stripMetadata(counted.token));
        int count = counted.count;
        if (token.startsWith("FluidCell:")) {
            ItemStack stack = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.byId(token.substring("FluidCell:".length())));
            stack.setCount(count);
            return stack;
        }
        if (token.startsWith("industrial_legacy:cable#")) {
            ItemStack stack = parseCableStack(token.substring("industrial_legacy:cable#".length()));
            stack.setCount(count);
            return stack;
        }
        if (token.startsWith("industrial_legacy:cable#")) {
            ItemStack stack = parseCableStack(token.substring("industrial_legacy:cable#".length()));
            stack.setCount(count);
            return stack;
        }

        Item item = resolveItem(token);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static IlCraftingIngredient cableIngredient(String rawProperties) {
        CableProperties properties = parseCableProperties(rawProperties);
        return IlCraftingIngredient.cable(properties.kind, properties.insulation);
    }

    private static ItemStack parseCableStack(String rawProperties) {
        CableProperties properties = parseCableProperties(rawProperties);
        return CableItem.createStack(ModItems.CABLE, properties.kind, properties.insulation);
    }

    private static CableProperties parseCableProperties(String rawProperties) {
        String kind = "copper";
        int insulation = 0;
        for (String token : rawProperties.split(",")) {
            String[] pair = token.trim().split("[:=]", 2);
            if (pair.length != 2) continue;
            String key = pair[0].trim();
            String value = pair[1].trim();
            if (key.equalsIgnoreCase("type") || key.equalsIgnoreCase("kind")) kind = value;
            if (key.equalsIgnoreCase("insulation")) insulation = Integer.parseInt(value);
        }
        CableKind cableKind = CableKind.fromId(kind);
        return new CableProperties(cableKind, cableKind.clampInsulation(insulation));
    }

    private static IlCraftingIngredient fluidIngredient(String fluidText) {
        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.CellFluid.byId(fluidText);
        Ingredient fallback = switch (fluid) {
            case WATER -> Ingredient.ofItems(Items.WATER_BUCKET);
            case LAVA -> Ingredient.ofItems(Items.LAVA_BUCKET);
            case MILK -> Ingredient.ofItems(Items.MILK_BUCKET);
            default -> Ingredient.EMPTY;
        };
        return IlCraftingIngredient.fluidContainer(fluid, fallback);
    }

    private static IlCraftingIngredient tagIngredient(String tagId) {
        JsonObject json = new JsonObject();
        json.addProperty("tag", tagId);
        return IlCraftingIngredient.of(Ingredient.fromJson(json));
    }

    private static IlCraftingIngredient oreDictIngredient(String oreDict) {
        return switch (oreDict) {
            case "plankWood" -> IlCraftingIngredient.of(Ingredient.ofItems(
                    Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
                    Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS,
                    Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS));
            case "stickWood" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.STICK));
            case "logWood" -> IlCraftingIngredient.of(Ingredient.ofItems(
                    Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG,
                    Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG,
                    Blocks.CRIMSON_STEM, Blocks.WARPED_STEM));
            case "chestWood" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.CHEST, Items.TRAPPED_CHEST));
            case "treeLeaves" -> groupIngredient("tree_leaves");
            case "treeSapling" -> groupIngredient("tree_saplings");
            case "itemRubber" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.RUBBER));
            case "craftingToolForgeHammer" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.FORGE_HAMMER));
            case "craftingToolWireCutter" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.CUTTER));
            case "circuitBasic" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.ELECTRONIC_CIRCUIT));
            case "circuitAdvanced" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.ADVANCED_CIRCUIT));
            case "ingotCopper" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.COPPER_INGOT));
            case "ingotIron" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.IRON_INGOT));
            case "ingotTin" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.TIN_INGOT));
            case "ingotLead" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.LEAD_INGOT));
            case "ingotSilver" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SILVER_INGOT));
            case "ingotBronze" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.BRONZE_INGOT));
            case "ingotSteel", "ingotRefinedIron" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.STEEL_INGOT));
            case "ingotUranium" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.URANIUM));
            case "ingotPlutonium" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.PLUTONIUM));
            case "nuggetUranium235" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SMALL_URANIUM_235));
            case "nuggetIridium" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.IRIDIUM_SHARD));
            case "gemDiamond" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.DIAMOND));
            case "gemIridium" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.IRIDIUM));
            case "plateCopper" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.COPPER_PLATE));
            case "plateTin" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.TIN_PLATE));
            case "plateBronze" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.BRONZE_PLATE));
            case "plateGold" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.GOLD_PLATE));
            case "plateIron" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.IRON_PLATE));
            case "plateLead" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.LEAD_PLATE));
            case "plateLapis" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.LAPIS_PLATE));
            case "plateSteel" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.STEEL_PLATE));
            case "plateDenseCopper" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.DENSE_COPPER_PLATE));
            case "plateDenseIron" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.DENSE_IRON_PLATE));
            case "plateDenseLead" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.DENSE_LEAD_PLATE));
            case "plateDenseTin" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.DENSE_TIN_PLATE));
            case "dustCoal" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.COAL_DUST));
            case "dustCopper" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.COPPER_DUST));
            case "dustTin" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.TIN_DUST));
            case "dustLead" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.LEAD_DUST));
            case "dustGold" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.GOLD_DUST));
            case "dustIron" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.IRON_DUST));
            case "dustBronze" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.BRONZE_DUST));
            case "dustDiamond" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.DIAMOND_DUST));
            case "dustHydratedCoal" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.COAL_FUEL_DUST));
            case "dustLapis" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.LAPIS_DUST));
            case "dustObsidian" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.OBSIDIAN_DUST));
            case "dustSiliconDioxide" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SILICON_DIOXIDE));
            case "dustSilver" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SILVER_DUST));
            case "dustStone" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.STONE_DUST));
            case "dustSulfur" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SULFUR));
            case "materialScrap" -> IlCraftingIngredient.of(Ingredient.ofItems(ModItems.SCRAP));
            case "dyeBlack" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.BLACK_DYE));
            case "dyeBlue" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.BLUE_DYE));
            case "dyeBrown" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.BROWN_DYE));
            case "dyeLightBlue" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.LIGHT_BLUE_DYE));
            case "dyeCyan" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.CYAN_DYE));
            case "dyeGray" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.GRAY_DYE));
            case "dyeGreen" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.GREEN_DYE));
            case "dyeLightGray" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.LIGHT_GRAY_DYE));
            case "dyeLime" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.LIME_DYE));
            case "dyeMagenta" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.MAGENTA_DYE));
            case "dyeOrange" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.ORANGE_DYE));
            case "dyePink" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.PINK_DYE));
            case "dyePurple" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.PURPLE_DYE));
            case "dyeRed" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.RED_DYE));
            case "dyeWhite" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.WHITE_DYE));
            case "dyeYellow" -> IlCraftingIngredient.of(Ingredient.ofItems(Items.YELLOW_DYE));
            default -> tagIngredient("c:" + camelToTag(oreDict));
        };
    }

    private static IlCraftingIngredient groupIngredient(String group) {
        return switch (group) {
            case "tree_leaves" -> IlCraftingIngredient.of(Ingredient.ofItems(
                    Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES,
                    Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.CHERRY_LEAVES));
            case "tree_saplings" -> IlCraftingIngredient.of(Ingredient.ofItems(
                    Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING,
                    Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING, Blocks.MANGROVE_PROPAGULE, Blocks.CHERRY_SAPLING));
            default -> throw new IllegalArgumentException("Unknown group " + group);
        };
    }

    private static Item resolveItem(String idText) {
        Identifier id = Identifier.tryParse(idText);
        if (id == null) throw new IllegalArgumentException("Bad item id " + idText);
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(new Identifier("minecraft", "air"))) {
            throw new IllegalArgumentException("Unknown item id " + idText);
        }
        return item;
    }

    private static String normalizeToken(String token) {
        String mapped = mapLegacyMinecraftToken(token);
        if (!mapped.equals(token)) return mapped;
        mapped = mapIndustrialLegacyAlias(token);
        if (!mapped.equals(token)) return mapped;
        if (!token.startsWith("industrial_legacy:")) return token;

        if (token.startsWith("industrial_legacy:cable#")) return "industrial_legacy:cable#" + token.substring("industrial_legacy:cable#".length());
        if (token.startsWith("industrial_legacy:fluid_cell#")) return "FluidCell:" + token.substring("industrial_legacy:fluid_cell#".length());
        if (token.equals("industrial_legacy:fluid_cell")) return "industrial_legacy:fluid_cell";

        int hash = token.indexOf('#');
        if (hash < 0) return mapIndustrialLegacyAlias(token.replace("industrial_legacy:", "industrial_legacy:"));
        String base = token.substring(4, hash);
        String variant = token.substring(hash + 1);
        String id = switch (base) {
            case "ingot" -> switch (variant) {
                case "copper" -> "minecraft:copper_ingot";
                case "tin" -> "industrial_legacy:tin_ingot";
                case "lead" -> "industrial_legacy:lead_ingot";
                case "silver" -> "industrial_legacy:silver_ingot";
                case "bronze" -> "industrial_legacy:bronze_ingot";
                case "steel" -> "industrial_legacy:steel_ingot";
                default -> null;
            };
            case "dust" -> switch (variant) {
                case "coal" -> "industrial_legacy:coal_dust";
                case "coal_fuel" -> "industrial_legacy:coal_fuel_dust";
                case "tin_hydrated" -> "industrial_legacy:tin_hydrated_dust";
                default -> "industrial_legacy:" + variant + "_dust";
            };
            case "crushed" -> "industrial_legacy:crushed_" + variant + "_ore";
            case "purified" -> switch (variant) {
                case "copper" -> "industrial_legacy:copper_purified_crushed_ore";
                case "iron" -> "industrial_legacy:iron_purified_crushed_ore";
                case "gold" -> "industrial_legacy:gold_purified_crushed_ore";
                case "tin" -> "industrial_legacy:tin_purified_crushed_ore";
                case "lead" -> "industrial_legacy:lead_purified_crushed_ore";
                case "silver" -> "industrial_legacy:silver_purified_crushed_ore";
                case "uranium" -> "industrial_legacy:purified_uranium_ore";
                default -> null;
            };
            case "plate" -> "industrial_legacy:" + variant + "_plate";
            case "casing" -> "industrial_legacy:" + variant + "_casing";
            case "resource" -> mapResource(variant);
            case "crafting" -> mapCrafting(variant);
            case "te" -> "industrial_legacy:" + variant;
            case "glass" -> variant.equals("reinforced") ? "industrial_legacy:reinforced_glass" : null;
            case "misc_resource" -> mapMiscResource(variant);
            case "nuclear" -> mapNuclear(variant);
            case "upgrade" -> "industrial_legacy:" + variant + "_upgrade";
            case "reactor_vent" -> "industrial_legacy:" + variant + "_heat_vent";
            case "reactor_heat_exchanger" -> "industrial_legacy:" + variant + "_heat_exchanger";
            case "plating" -> variant.equals("heat") ? "industrial_legacy:heat_plating" : "industrial_legacy:reactor_plating";
            case "heat_storage" -> mapHeatStorage(variant);
            case "mug" -> "industrial_legacy:" + variant;
            case "crop_res" -> "industrial_legacy:" + variant;
            case "boat" -> "industrial_legacy:" + variant + "_boat";
            case "upgrade_kit" -> "industrial_legacy:" + variant + "_upgrade_kit";
            default -> "industrial_legacy:" + base + "_" + variant;
        };
        String mappedId = id == null ? token.replace("industrial_legacy:", "industrial_legacy:").replace('#', '_') : id;
        return mapIndustrialLegacyAlias(mappedId);
    }

    private static String mapIndustrialLegacyAlias(String token) {
        if (!token.startsWith("industrial_legacy:")) return token;
        if (token.startsWith("industrial_legacy:cable#")) return token;
        String local = token.substring("industrial_legacy:".length());
        String mapped = switch (local) {
            case "alloy_ingot" -> "mixed_metal_ingot";
            case "centrifuge" -> "thermal_centrifuge";
            case "circuit" -> "electronic_circuit";
            case "copper_block" -> "minecraft:copper_block";
            case "crushed_silver_ore" -> "silver_crushed_ore";
            case "fence_iron" -> "iron_fence";
            case "luminator_flat" -> "luminator";
            case "mining_pipe_pipe" -> "mining_pipe";
            case "bronze_shaft" -> "bronze_rod";
            case "steel_shaft" -> "steel_rod";
            case "plating" -> "reactor_plating";
            case "rubber_wood" -> "rubber_log";
            case "scaffold_iron" -> "iron_scaffold";
            case "scaffold_wood" -> "scaffold";
            case "sheet_resin" -> "resin_sheet";
            case "sheet_rubber" -> "rubber_sheet";
            case "sheet_wool" -> "wool_sheet";
            case "solar_generator" -> "solar_panel";
            case "sulfur_dust" -> "sulfur";
            case "wrench_new" -> "wrench";
            default -> local;
        };
        if (mapped.startsWith("minecraft:")) return mapped;
        return "industrial_legacy:" + mapped;
    }

    private static String mapLegacyMinecraftToken(String token) {
        int at = token.indexOf('@');
        String base = at >= 0 ? token.substring(0, at) : token;
        String meta = at >= 0 ? token.substring(at + 1) : "";
        if (!base.startsWith("minecraft:")) return token;
        return switch (base) {
            case "minecraft:log" -> switch (meta) {
                case "1" -> "minecraft:spruce_log";
                case "2" -> "minecraft:birch_log";
                case "3" -> "minecraft:jungle_log";
                default -> "minecraft:oak_log";
            };
            case "minecraft:planks" -> "minecraft:oak_planks";
            case "minecraft:trapdoor" -> "minecraft:oak_trapdoor";
            case "minecraft:stonebrick" -> switch (meta) {
                case "1" -> "minecraft:mossy_stone_bricks";
                case "2" -> "minecraft:cracked_stone_bricks";
                case "3" -> "minecraft:chiseled_stone_bricks";
                default -> "minecraft:stone_bricks";
            };
            case "minecraft:dye" -> switch (meta) {
                case "0" -> "minecraft:ink_sac";
                case "1" -> "minecraft:red_dye";
                case "2" -> "minecraft:green_dye";
                case "3" -> "minecraft:cocoa_beans";
                case "4" -> "minecraft:lapis_lazuli";
                case "15" -> "minecraft:bone_meal";
                default -> "minecraft:white_dye";
            };
            case "minecraft:reeds" -> "minecraft:sugar_cane";
            case "minecraft:waterlily" -> "minecraft:lily_pad";
            case "minecraft:brick_block" -> "minecraft:bricks";
            case "minecraft:wool" -> "minecraft:white_wool";
            case "minecraft:carpet" -> "minecraft:white_carpet";
            case "minecraft:tallgrass" -> "minecraft:grass";
            case "minecraft:wooden_slab" -> "minecraft:oak_slab";
            default -> base;
        };
    }

    private static String mapResource(String variant) {
        return switch (variant) {
            case "machine" -> "industrial_legacy:machine_casing";
            case "advanced_machine" -> "industrial_legacy:advanced_machine";
            case "alloy" -> "industrial_legacy:advanced_alloy";
            case "copper_ore" -> "minecraft:copper_ore";
            case "tin_ore" -> "industrial_legacy:tin_ore";
            case "lead_ore" -> "industrial_legacy:lead_ore";
            case "silver_ore" -> "industrial_legacy:silver_ore";
            case "uranium_ore" -> "industrial_legacy:uranium_ore";
            case "uranium_block" -> "industrial_legacy:uranium_block";
            case "bronze_block" -> "industrial_legacy:bronze_block";
            case "tin_block" -> "industrial_legacy:tin_block";
            case "lead_block" -> "industrial_legacy:lead_block";
            case "silver_block" -> "industrial_legacy:silver_block";
            case "steel_block" -> "industrial_legacy:steel_block";
            case "reactor_vessel" -> "industrial_legacy:reactor_vessel";
            case "reinforced_stone" -> "industrial_legacy:reinforced_stone";
            default -> "industrial_legacy:" + variant;
        };
    }

    private static String mapCrafting(String variant) {
        return switch (variant) {
            case "alloy" -> "industrial_legacy:advanced_alloy";
            case "rubber" -> "industrial_legacy:rubber";
            case "coil" -> "industrial_legacy:coil";
            case "carbon_fibre" -> "industrial_legacy:carbon_fibre";
            case "carbon_mesh" -> "industrial_legacy:carbon_mesh";
            case "carbon_plate" -> "industrial_legacy:carbon_plate";
            case "mixed_metal_ingot" -> "industrial_legacy:mixed_metal_ingot";
            case "iridium" -> "industrial_legacy:iridium";
            case "small_power_unit" -> "industrial_legacy:power_unit";
            case "electric_motor" -> "industrial_legacy:electric_motor";
            case "heat_conductor" -> "industrial_legacy:heat_exchanger";
            case "iron_shaft" -> "industrial_legacy:iron_rod";
            default -> "industrial_legacy:" + variant;
        };
    }

    private static String mapMiscResource(String variant) {
        return switch (variant) {
            case "resin" -> "industrial_legacy:sticky_resin";
            case "iridium_ore" -> "industrial_legacy:iridium_shard";
            case "matter" -> "industrial_legacy:uu_matter";
            case "water_sheet" -> "industrial_legacy:water_sheet";
            case "lava_sheet" -> "industrial_legacy:lava_sheet";
            case "iodine" -> "industrial_legacy:iodine";
            default -> "industrial_legacy:" + variant;
        };
    }

    private static String mapNuclear(String variant) {
        return switch (variant) {
            case "uranium" -> "industrial_legacy:uranium";
            case "mox" -> "industrial_legacy:mox";
            case "plutonium" -> "industrial_legacy:plutonium";
            case "small_plutonium" -> "industrial_legacy:small_plutonium";
            case "uranium_235" -> "industrial_legacy:uranium_235";
            case "small_uranium_235" -> "industrial_legacy:small_uranium_235";
            case "uranium_238" -> "industrial_legacy:uranium_238";
            default -> "industrial_legacy:" + variant;
        };
    }

    private static String mapHeatStorage(String variant) {
        return switch (variant) {
            case "10k" -> "industrial_legacy:heat_storage";
            case "30k" -> "industrial_legacy:tri_heat_storage";
            case "60k" -> "industrial_legacy:hex_heat_storage";
            default -> "industrial_legacy:heat_storage";
        };
    }

    private static boolean referencesUnportedRecipeToken(String text) {
        int from = 0;
        while (true) {
            int idx = text.indexOf("industrial_legacy:", from);
            if (idx < 0) return false;
            int start = idx + "industrial_legacy:".length();
            int end = start;
            while (end < text.length()) {
                char c = text.charAt(end);
                if (Character.isWhitespace(c) || c == '|' || c == '=') break;
                end++;
            }
            String local = text.substring(start, end);
            int at = local.indexOf('@');
            if (at >= 0) local = local.substring(0, at);
            int star = local.lastIndexOf('*');
            if (star >= 0) local = local.substring(0, star);
            if (local.startsWith("pipe_type:")) return true;
            if (UNPORTED_RECIPE_TOKENS.contains(local)) return true;
            from = end;
        }
    }

    private static String camelToTag(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c) && i > 0) out.append('/');
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String token : text.trim().split("\\s+")) {
            if (!token.isBlank()) tokens.add(token.trim());
        }
        return tokens;
    }

    private static String stripComment(String raw) {
        int comment = raw.indexOf(';');
        return comment >= 0 ? raw.substring(0, comment) : raw;
    }

    private static String stripAttributes(String text) {
        String result = text;
        int at = result.indexOf(" @");
        if (at >= 0) result = result.substring(0, at);
        if (result.startsWith("@")) return "";
        return result.trim();
    }

    private static String stripMetadata(String token) {
        int at = token.indexOf('@');
        if (at >= 0) token = token.substring(0, at);
        return token.trim();
    }

    private static CountedToken splitCount(String raw) {
        String token = raw.trim();
        int count = 1;
        int star = token.lastIndexOf('*');
        if (star >= 0) {
            try {
                count = Math.max(1, Integer.parseInt(token.substring(star + 1).trim()));
                token = token.substring(0, star).trim();
            } catch (NumberFormatException ignored) {
                // Metadata wildcard, not a stack count.
            }
        }
        return new CountedToken(token, count);
    }

    private static float parseFloatAttribute(String raw, String name, float fallback) {
        String needle = "@" + name + ":";
        int start = raw.indexOf(needle);
        if (start < 0) return fallback;
        start += needle.length();
        int end = start;
        while (end < raw.length()) {
            char c = raw.charAt(end);
            if (!Character.isDigit(c) && c != '.') break;
            end++;
        }
        try {
            return Float.parseFloat(raw.substring(start, end));
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String sanitize(ItemStack stack) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        return sanitize(id.toString() + "_x" + stack.getCount());
    }

    private static String sanitize(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('#', '_')
                .replace('@', '_')
                .replace('*', '_')
                .replace(' ', '_')
                .replace('|', '_')
                .replace('=', '_')
                .replace(',', '_')
                .replaceAll("[^a-z0-9_./-]", "_");
    }

    private record Line(int number, String text) {}
    private record Shape(int width, int height, List<IlCraftingIngredient> inputs) {}
    private record CableProperties(CableKind kind, int insulation) {}
    private record CountedToken(String token, int count) {}
}
