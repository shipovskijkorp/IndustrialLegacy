package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Parser for the compact IC2-style machine recipe .ini format used by IL. */
final class MachineRecipeIniLoader {
    private static final int DEFAULT_TICKS = 300;
    private static final int DEFAULT_METAL_FORMER_TICKS = 200;
    private static final int DEFAULT_CANNING_TICKS = 200;

    private MachineRecipeIniLoader() {}

    static List<MaceratorRecipe> loadMacerator(String resourcePath) {
        List<ParsedLine> lines = loadLines(resourcePath, DEFAULT_TICKS);
        List<MaceratorRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ParsedLine line = lines.get(i);
            try {
                ParsedInput input = parseInput(line.input());
                ParsedStack output = parseOutput(line.output());
                if (input == null || output == null) continue;

                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/macerator/" + sanitizeId(line.input()) + "_to_" + sanitizeId(line.output()) + "_" + i);
                recipes.add(new MaceratorRecipe(id, input.ingredient(), input.count(), output.stack(), line.ticks()));
            } catch (RuntimeException e) {
                IndustrialLegacy.LOGGER.warn("Skipping macerator ini recipe {}:{} -> {}: {}",
                        resourcePath, line.number(), line.raw(), e.getMessage());
            }
        }
        return recipes;
    }

    static List<CompressorRecipe> loadCompressor(String resourcePath) {
        List<ParsedLine> lines = loadLines(resourcePath, DEFAULT_TICKS);
        List<CompressorRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ParsedLine line = lines.get(i);
            try {
                ParsedInput input = parseInput(line.input());
                ParsedStack output = parseOutput(line.output());
                if (input == null || output == null) continue;

                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/compressor/" + sanitizeId(line.input()) + "_to_" + sanitizeId(line.output()) + "_" + i);
                recipes.add(new CompressorRecipe(id, input.ingredient(), input.count(), output.stack(), line.ticks(), input.requiredFluid()));
            } catch (RuntimeException e) {
                IndustrialLegacy.LOGGER.warn("Skipping compressor ini recipe {}:{} -> {}: {}",
                        resourcePath, line.number(), line.raw(), e.getMessage());
            }
        }
        return recipes;
    }

    static List<MetalFormerRecipe> loadMetalFormer(String resourcePath, RecipeType<?> type, RecipeSerializer<?> serializer, String modeId) {
        List<ParsedLine> lines = loadLines(resourcePath, DEFAULT_METAL_FORMER_TICKS);
        List<MetalFormerRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ParsedLine line = lines.get(i);
            try {
                ParsedInput input = parseInput(line.input());
                ParsedStack output = parseOutput(line.output());
                if (input == null || output == null) continue;

                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/metal_former/" + modeId + "/" + sanitizeId(line.input()) + "_to_" + sanitizeId(line.output()) + "_" + i);
                recipes.add(new MetalFormerRecipe(id, input.ingredient(), output.stack(), line.ticks(), input.count(), type, serializer));
            } catch (RuntimeException e) {
                IndustrialLegacy.LOGGER.warn("Skipping metal former {} ini recipe {}:{} -> {}: {}",
                        modeId, resourcePath, line.number(), line.raw(), e.getMessage());
            }
        }
        return recipes;
    }

    static List<CanningRecipe> loadCanning(String resourcePath) {
        List<ParsedLine> lines = loadLines(resourcePath, DEFAULT_CANNING_TICKS);
        List<CanningRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ParsedLine line = lines.get(i);
            try {
                String[] pair = splitCanningInputs(line.input());
                ParsedInput container = parseInput(pair[0]);
                ParsedInput fill = parseInput(pair[1]);
                ParsedStack output = parseOutput(line.output());
                if (container == null || fill == null || output == null) continue;

                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/canning/" + sanitizeId(pair[0]) + "_with_" + sanitizeId(pair[1]) + "_to_" + sanitizeId(line.output()) + "_" + i);
                recipes.add(new CanningRecipe(id, container.ingredient(), container.count(), fill.ingredient(), fill.count(), output.stack(), line.ticks()));
            } catch (RuntimeException e) {
                IndustrialLegacy.LOGGER.warn("Skipping canning ini recipe {}:{} -> {}: {}",
                        resourcePath, line.number(), line.raw(), e.getMessage());
            }
        }
        return recipes;
    }

    static List<CanningEnrichRecipe> loadCanningEnrich(String resourcePath) {
        List<ParsedLine> lines = loadLines(resourcePath, DEFAULT_CANNING_TICKS);
        List<CanningEnrichRecipe> recipes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ParsedLine line = lines.get(i);
            try {
                String[] pair = splitCanningInputs(line.input());
                ParsedFluid inputFluid = parseFluid(pair[0]);
                ParsedInput additive = parseInput(pair[1]);
                ParsedFluid outputFluid = parseFluid(line.output());
                if (inputFluid == null || additive == null || outputFluid == null) continue;

                Identifier id = new Identifier(IndustrialLegacy.MOD_ID, "ini/canning_enrich/" + sanitizeId(pair[0]) + "_with_" + sanitizeId(pair[1]) + "_to_" + sanitizeId(line.output()) + "_" + i);
                recipes.add(new CanningEnrichRecipe(id, inputFluid.fluid(), inputFluid.amount(), additive.ingredient(), additive.count(), outputFluid.fluid(), outputFluid.amount(), line.ticks()));
            } catch (RuntimeException e) {
                IndustrialLegacy.LOGGER.warn("Skipping canning enrich ini recipe {}:{} -> {}: {}",
                        resourcePath, line.number(), line.raw(), e.getMessage());
            }
        }
        return recipes;
    }

    private static List<ParsedLine> loadLines(String resourcePath, int defaultTicks) {
        List<ParsedLine> result = new ArrayList<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream = loader.getResourceAsStream(resourcePath);
        if (stream == null) {
            stream = MachineRecipeIniLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        }
        if (stream == null) {
            IndustrialLegacy.LOGGER.warn("Missing IC2-style recipe ini: {}", resourcePath);
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                number++;
                ParsedLine parsed = parseLine(line, number, defaultTicks);
                if (parsed != null) result.add(parsed);
            }
        } catch (IOException e) {
            IndustrialLegacy.LOGGER.warn("Failed to read IC2-style recipe ini {}", resourcePath, e);
        }
        return result;
    }

    private static ParsedLine parseLine(String raw, int number, int defaultTicks) {
        String noComment = raw;
        int comment = noComment.indexOf(';');
        if (comment >= 0) noComment = noComment.substring(0, comment);
        noComment = noComment.trim();
        if (noComment.isEmpty()) return null;

        int equals = noComment.indexOf('=');
        if (equals < 0) return null;

        String input = noComment.substring(0, equals).trim();
        String output = noComment.substring(equals + 1).trim();
        int ticks = defaultTicks;

        // Optional IL extension: append "| ticks=200" to a recipe line.
        int pipe = output.indexOf('|');
        if (pipe >= 0) {
            String meta = output.substring(pipe + 1).trim();
            output = output.substring(0, pipe).trim();
            for (String token : meta.split(",")) {
                String[] pair = token.trim().split("=", 2);
                if (pair.length == 2 && pair[0].trim().equalsIgnoreCase("ticks")) {
                    ticks = Math.max(1, Integer.parseInt(pair[1].trim()));
                }
            }
        }

        int comma = output.indexOf(',');
        if (comma >= 0 && !output.startsWith("industrial_legacy:cable#")) {
            output = output.substring(0, comma).trim();
        }
        if (input.isEmpty() || output.isEmpty()) return null;
        return new ParsedLine(raw, number, input, output, ticks);
    }

    private static String[] splitCanningInputs(String rawInput) {
        String[] parts = rawInput.split("\\s+\\+\\s+", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("Canning input must be '<container> + <fill>'");
        }
        return parts;
    }

    private static ParsedFluid parseFluid(String rawToken) {
        CountedToken counted = splitCount(rawToken.trim());
        String token = stripMetadata(counted.token());
        int amount = counted.count();

        String idText;
        if (token.startsWith("Fluid:")) {
            idText = token.substring("Fluid:".length()).trim();
        } else {
            throw new IllegalArgumentException("Fluid token must be 'Fluid:<id>[*mb]'");
        }

        String fluidId = normalizeFluidId(idText);
        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.CellFluid.byId(fluidId);
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            throw new IllegalArgumentException("Unknown or empty fluid id in canning enrich recipe: " + fluidId);
        }
        return new ParsedFluid(fluid, amount);
    }

    private static ParsedInput parseInput(String rawToken) {
        CountedToken counted = splitCount(rawToken.trim());
        String token = stripMetadata(counted.token());
        int count = counted.count();

        if (token.startsWith("Fluid:")) {
            String fluidId = normalizeFluidId(token.substring("Fluid:".length()).trim());
            return new ParsedInput(Ingredient.ofItems(ModItems.FLUID_CELL), count, fluidId);
        }
        if (token.startsWith("FluidCell:")) {
            String fluidId = normalizeFluidId(token.substring("FluidCell:".length()).trim());
            return new ParsedInput(Ingredient.ofItems(ModItems.FLUID_CELL), count, fluidId);
        }
        if (token.startsWith("Tag:")) {
            String tag = token.substring("Tag:".length()).trim();
            JsonObject json = new JsonObject();
            json.addProperty("tag", tag);
            return new ParsedInput(Ingredient.fromJson(json), count, null);
        }
        if (token.startsWith("Group:")) {
            return new ParsedInput(groupIngredient(token.substring("Group:".length()).trim()), count, null);
        }

        Item item = resolveItem(token);
        if (item == null) return null;
        return new ParsedInput(Ingredient.ofItems(item), count, null);
    }

    private static ParsedStack parseOutput(String rawToken) {
        CountedToken counted = splitCount(rawToken.trim());
        String token = stripMetadata(counted.token());
        int count = counted.count();

        if (token.startsWith("FluidCell:")) {
            String fluidId = normalizeFluidId(token.substring("FluidCell:".length()).trim());
            UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.CellFluid.byId(fluidId);
            ItemStack stack = UniversalFluidCellItem.createStack(fluid);
            stack.setCount(count);
            return new ParsedStack(stack);
        }
        if (token.startsWith("Cable:")) {
            return new ParsedStack(parseCableStack(token.substring("Cable:".length()).trim(), count));
        }
        if (token.startsWith("industrial_legacy:cable#")) {
            return new ParsedStack(parseCableStack(token.substring("industrial_legacy:cable#".length()).trim(), count));
        }

        Item item = resolveItem(token);
        if (item == null) return null;
        return new ParsedStack(new ItemStack(item, count));
    }

    private static ItemStack parseCableStack(String rawProperties, int count) {
        String kind = "copper";
        int insulation = 0;

        String raw = rawProperties.trim();
        if (!raw.contains("=") && !raw.contains(",") && raw.contains(":")) {
            String[] parts = raw.split(":");
            if (parts.length >= 1 && !parts[0].isBlank()) kind = parts[0].trim();
            if (parts.length >= 2 && !parts[1].isBlank()) insulation = Integer.parseInt(parts[1].trim());
        } else {
            for (String token : raw.split(",")) {
                String[] pair = token.trim().split("[:=]", 2);
                if (pair.length != 2) continue;
                String key = pair[0].trim();
                String value = pair[1].trim();
                if (key.equalsIgnoreCase("type") || key.equalsIgnoreCase("kind")) {
                    kind = value;
                } else if (key.equalsIgnoreCase("insulation")) {
                    insulation = Integer.parseInt(value);
                }
            }
        }

        ItemStack stack = CableItem.createStack(ModItems.CABLE, CableKind.fromId(kind), insulation);
        stack.setCount(count);
        return stack;
    }

    private static CountedToken splitCount(String raw) {
        String token = raw.trim();
        int count = 1;
        int star = token.lastIndexOf('*');
        if (star >= 0) {
            String maybeCount = token.substring(star + 1).trim();
            try {
                count = Math.max(1, Integer.parseInt(maybeCount));
                token = token.substring(0, star).trim();
            } catch (NumberFormatException ignored) {
                // Star can also be IC2 wildcard metadata in old configs; keep token unchanged.
            }
        }
        return new CountedToken(token, count);
    }

    private static String stripMetadata(String token) {
        int at = token.indexOf('@');
        if (at >= 0) token = token.substring(0, at);
        return token.trim();
    }

    private static String normalizeFluidId(String fluidId) {
        if (fluidId.equals("water")) return "minecraft:water";
        if (fluidId.equals("lava")) return "minecraft:lava";
        if (fluidId.equals("air") || fluidId.equals("ic2air")) return "industrial_legacy:air";
        if (fluidId.equals("empty")) return "empty";
        return fluidId;
    }

    private static Item resolveItem(String idText) {
        Identifier id = new Identifier(idText);
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR && !id.equals(new Identifier("minecraft", "air"))) {
            IndustrialLegacy.LOGGER.warn("Unknown item id in machine recipe ini: {}", id);
            return null;
        }
        return item;
    }

    private static Ingredient groupIngredient(String group) {
        return switch (group) {
            case "tree_leaves" -> Ingredient.ofItems(
                    Blocks.OAK_LEAVES.asItem(), Blocks.SPRUCE_LEAVES.asItem(), Blocks.BIRCH_LEAVES.asItem(),
                    Blocks.JUNGLE_LEAVES.asItem(), Blocks.ACACIA_LEAVES.asItem(), Blocks.DARK_OAK_LEAVES.asItem(),
                    Blocks.MANGROVE_LEAVES.asItem(), Blocks.CHERRY_LEAVES.asItem(), Blocks.AZALEA_LEAVES.asItem(),
                    Blocks.FLOWERING_AZALEA_LEAVES.asItem(), ModBlocks.RUBBER_LEAVES.asItem()
            );
            case "tree_saplings" -> Ingredient.ofItems(
                    Blocks.OAK_SAPLING.asItem(), Blocks.SPRUCE_SAPLING.asItem(), Blocks.BIRCH_SAPLING.asItem(),
                    Blocks.JUNGLE_SAPLING.asItem(), Blocks.ACACIA_SAPLING.asItem(), Blocks.DARK_OAK_SAPLING.asItem(),
                    Blocks.MANGROVE_PROPAGULE.asItem(), Blocks.CHERRY_SAPLING.asItem(), Blocks.AZALEA.asItem(),
                    Blocks.FLOWERING_AZALEA.asItem(), ModBlocks.RUBBER_SAPLING.asItem()
            );
            case "wool" -> Ingredient.ofItems(
                    Blocks.WHITE_WOOL.asItem(), Blocks.ORANGE_WOOL.asItem(), Blocks.MAGENTA_WOOL.asItem(),
                    Blocks.LIGHT_BLUE_WOOL.asItem(), Blocks.YELLOW_WOOL.asItem(), Blocks.LIME_WOOL.asItem(),
                    Blocks.PINK_WOOL.asItem(), Blocks.GRAY_WOOL.asItem(), Blocks.LIGHT_GRAY_WOOL.asItem(),
                    Blocks.CYAN_WOOL.asItem(), Blocks.PURPLE_WOOL.asItem(), Blocks.BLUE_WOOL.asItem(),
                    Blocks.BROWN_WOOL.asItem(), Blocks.GREEN_WOOL.asItem(), Blocks.RED_WOOL.asItem(),
                    Blocks.BLACK_WOOL.asItem()
            );
            case "sandstone" -> Ingredient.ofItems(
                    Blocks.SANDSTONE.asItem(), Blocks.CHISELED_SANDSTONE.asItem(), Blocks.CUT_SANDSTONE.asItem(),
                    Blocks.SMOOTH_SANDSTONE.asItem()
            );
            case "quartz_blocks" -> Ingredient.ofItems(
                    Blocks.QUARTZ_BLOCK.asItem(), Blocks.CHISELED_QUARTZ_BLOCK.asItem(), Blocks.QUARTZ_PILLAR.asItem(),
                    Blocks.SMOOTH_QUARTZ.asItem()
            );
            default -> throw new IllegalArgumentException("Unknown recipe ini group: " + group);
        };
    }

    private static String sanitizeId(String input) {
        String cleaned = input.toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replace('/', '_')
                .replace('#', '_')
                .replace('@', '_')
                .replace('*', '_')
                .replace(' ', '_')
                .replace('|', '_')
                .replace('=', '_')
                .replace('+', '_')
                .replace(',', '_');
        return cleaned.replaceAll("[^a-z0-9_./-]", "_");
    }

    private record ParsedLine(String raw, int number, String input, String output, int ticks) {}
    private record CountedToken(String token, int count) {}
    private record ParsedInput(Ingredient ingredient, int count, String requiredFluid) {}
    private record ParsedFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {}
    private record ParsedStack(ItemStack stack) {}
}
