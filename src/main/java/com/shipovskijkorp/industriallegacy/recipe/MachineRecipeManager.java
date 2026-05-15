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

    private static List<MaceratorRecipe> maceratorRecipes = List.of();
    private static List<CompressorRecipe> compressorRecipes = List.of();

    private MachineRecipeManager() {}

    public static void reloadBuiltin() {
        maceratorRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadMacerator(MACERATOR_PATH)));
        compressorRecipes = Collections.unmodifiableList(new ArrayList<>(MachineRecipeIniLoader.loadCompressor(COMPRESSOR_PATH)));

        IndustrialLegacy.LOGGER.info("Loaded {} macerator and {} compressor IC2-style .ini recipes",
                maceratorRecipes.size(), compressorRecipes.size());
    }

    public static List<MaceratorRecipe> getMaceratorRecipes() {
        ensureLoaded();
        return maceratorRecipes;
    }

    public static List<CompressorRecipe> getCompressorRecipes() {
        ensureLoaded();
        return compressorRecipes;
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

    private static void ensureLoaded() {
        if (maceratorRecipes.isEmpty() && compressorRecipes.isEmpty()) {
            reloadBuiltin();
        }
    }
}
