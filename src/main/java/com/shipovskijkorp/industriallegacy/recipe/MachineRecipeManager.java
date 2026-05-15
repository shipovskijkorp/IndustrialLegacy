package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class MachineRecipeManager {
    private static final String MACERATOR_PATH = "data/industrial_legacy/il_recipes/macerator.ini";
    private static final String COMPRESSOR_PATH = "data/industrial_legacy/il_recipes/compressor.ini";
    private static final String METAL_FORMER_EXTRUDING_PATH = "data/industrial_legacy/il_recipes/metal_former_extruding.ini";
    private static final String METAL_FORMER_ROLLING_PATH = "data/industrial_legacy/il_recipes/metal_former_rolling.ini";
    private static final String METAL_FORMER_CUTTING_PATH = "data/industrial_legacy/il_recipes/metal_former_cutting.ini";
    private static final String CANNING_PATH = "data/industrial_legacy/il_recipes/canning.ini";

    private static List<MaceratorRecipe> maceratorRecipes = List.of();
    private static List<CompressorRecipe> compressorRecipes = List.of();
    private static List<MetalFormerRecipe> metalFormerExtrudingRecipes = List.of();
    private static List<MetalFormerRecipe> metalFormerRollingRecipes = List.of();
    private static List<MetalFormerRecipe> metalFormerCuttingRecipes = List.of();
    private static List<CanningRecipe> canningRecipes = List.of();

    private MachineRecipeManager() {}

    public static void reloadBuiltin() {
        maceratorRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadMacerator(MACERATOR_PATH)));
        compressorRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadCompressor(COMPRESSOR_PATH)));
        metalFormerExtrudingRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadMetalFormer(
                METAL_FORMER_EXTRUDING_PATH, com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_EXTRUDING_TYPE,
                com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_EXTRUDING_SERIALIZER, "extruding")));
        metalFormerRollingRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadMetalFormer(
                METAL_FORMER_ROLLING_PATH, com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_ROLLING_TYPE,
                com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_ROLLING_SERIALIZER, "rolling")));
        metalFormerCuttingRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadMetalFormer(
                METAL_FORMER_CUTTING_PATH, com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_CUTTING_TYPE,
                com.shipovskijkorp.industriallegacy.registry.ModRecipes.METAL_FORMER_CUTTING_SERIALIZER, "cutting")));
        canningRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadCanning(CANNING_PATH)));

        IndustrialLegacy.LOGGER.info(
                "Loaded IC2-style .ini recipes: {} macerator, {} compressor, {} metal former extruding, {} rolling, {} cutting, {} canning",
                maceratorRecipes.size(), compressorRecipes.size(),
                metalFormerExtrudingRecipes.size(), metalFormerRollingRecipes.size(), metalFormerCuttingRecipes.size(),
                canningRecipes.size());
    }

    public static List<MaceratorRecipe> getMaceratorRecipes() {
        ensureLoaded();
        return maceratorRecipes;
    }

    public static List<CompressorRecipe> getCompressorRecipes() {
        ensureLoaded();
        return compressorRecipes;
    }

    public static List<MetalFormerRecipe> getMetalFormerExtrudingRecipes() {
        ensureLoaded();
        return metalFormerExtrudingRecipes;
    }

    public static List<MetalFormerRecipe> getMetalFormerRollingRecipes() {
        ensureLoaded();
        return metalFormerRollingRecipes;
    }

    public static List<MetalFormerRecipe> getMetalFormerCuttingRecipes() {
        ensureLoaded();
        return metalFormerCuttingRecipes;
    }

    public static List<CanningRecipe> getCanningRecipes() {
        ensureLoaded();
        return canningRecipes;
    }

    public static Optional<MaceratorRecipe> findMaceratorRecipe(Inventory inv) {
        ensureLoaded();
        for (MaceratorRecipe recipe : maceratorRecipes) {
            if (recipe.matches(inv, null)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static Optional<CompressorRecipe> findCompressorRecipe(Inventory inv) {
        ensureLoaded();
        for (CompressorRecipe recipe : compressorRecipes) {
            if (recipe.matches(inv, null)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static Optional<MetalFormerRecipe> findMetalFormerRecipe(Inventory inv, com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity.Mode mode) {
        ensureLoaded();
        for (MetalFormerRecipe recipe : metalFormerRecipesForMode(mode)) {
            if (recipe.matches(inv, null)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    public static Optional<CanningRecipe> findCanningRecipe(Inventory inv) {
        ensureLoaded();
        for (CanningRecipe recipe : canningRecipes) {
            if (recipe.matches(inv, null)) {
                return Optional.of(recipe);
            }
        }
        return Optional.empty();
    }

    private static List<MetalFormerRecipe> metalFormerRecipesForMode(com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity.Mode mode) {
        return switch (mode) {
            case ROLLING -> metalFormerRollingRecipes;
            case CUTTING -> metalFormerCuttingRecipes;
            default -> metalFormerExtrudingRecipes;
        };
    }

    private static void ensureLoaded() {
        if (maceratorRecipes.isEmpty() && compressorRecipes.isEmpty()
                && metalFormerExtrudingRecipes.isEmpty() && metalFormerRollingRecipes.isEmpty()
                && metalFormerCuttingRecipes.isEmpty() && canningRecipes.isEmpty()) {
            reloadBuiltin();
        }
    }
}
