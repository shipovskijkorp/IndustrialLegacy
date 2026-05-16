package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** JEI-visible recipes for the Canner liquid emptying/bottling modes. */
public final class CanningFluidRecipe {
    public enum Mode {
        EMPTY_LIQUID,
        BOTTLE_LIQUID
    }

    private static final int CELL_MB = 1000;

    private final Identifier id;
    private final Mode mode;
    private final UniversalFluidCellItem.CellFluid fluid;
    private final int amount;
    private final int ticks;

    private CanningFluidRecipe(Identifier id, Mode mode, UniversalFluidCellItem.CellFluid fluid, int amount, int ticks) {
        this.id = id;
        this.mode = mode;
        this.fluid = fluid;
        this.amount = Math.max(1, amount);
        this.ticks = Math.max(1, ticks);
    }

    public static List<CanningFluidRecipe> createEmptyLiquidRecipes() {
        List<CanningFluidRecipe> recipes = new ArrayList<>();
        for (UniversalFluidCellItem.CellFluid fluid : UniversalFluidCellItem.CellFluid.values()) {
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) continue;
            recipes.add(new CanningFluidRecipe(
                    new Identifier(IndustrialLegacy.MOD_ID, "ini/canning_empty_liquid/" + fluid.langPath()),
                    Mode.EMPTY_LIQUID,
                    fluid,
                    CELL_MB,
                    200));
        }
        return recipes;
    }

    public static List<CanningFluidRecipe> createBottleLiquidRecipes() {
        List<CanningFluidRecipe> recipes = new ArrayList<>();
        for (UniversalFluidCellItem.CellFluid fluid : UniversalFluidCellItem.CellFluid.values()) {
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) continue;
            recipes.add(new CanningFluidRecipe(
                    new Identifier(IndustrialLegacy.MOD_ID, "ini/canning_bottle_liquid/" + fluid.langPath()),
                    Mode.BOTTLE_LIQUID,
                    fluid,
                    CELL_MB,
                    200));
        }
        return recipes;
    }

    public Identifier getId() {
        return id;
    }

    public Mode getMode() {
        return mode;
    }

    public UniversalFluidCellItem.CellFluid getFluid() {
        return fluid;
    }

    public int getAmount() {
        return amount;
    }

    public int getTicks() {
        return ticks;
    }

    public ItemStack getFilledCell() {
        return UniversalFluidCellItem.createStack(fluid);
    }

    public ItemStack getEmptyCell() {
        return UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
    }
}
