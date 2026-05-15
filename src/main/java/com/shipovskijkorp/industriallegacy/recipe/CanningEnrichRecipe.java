package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

public final class CanningEnrichRecipe {
    private final Identifier id;
    private final UniversalFluidCellItem.CellFluid inputFluid;
    private final int inputAmount;
    private final Ingredient additive;
    private final int additiveCount;
    private final UniversalFluidCellItem.CellFluid outputFluid;
    private final int outputAmount;
    private final int ticks;

    public CanningEnrichRecipe(Identifier id,
                               UniversalFluidCellItem.CellFluid inputFluid,
                               int inputAmount,
                               Ingredient additive,
                               int additiveCount,
                               UniversalFluidCellItem.CellFluid outputFluid,
                               int outputAmount,
                               int ticks) {
        this.id = id;
        this.inputFluid = inputFluid;
        this.inputAmount = Math.max(1, inputAmount);
        this.additive = additive;
        this.additiveCount = Math.max(1, additiveCount);
        this.outputFluid = outputFluid;
        this.outputAmount = Math.max(1, outputAmount);
        this.ticks = Math.max(1, ticks);
    }

    public Identifier getId() {
        return id;
    }

    public UniversalFluidCellItem.CellFluid getInputFluid() {
        return inputFluid;
    }

    public int getInputAmount() {
        return inputAmount;
    }

    public Ingredient getAdditive() {
        return additive;
    }

    public int getAdditiveCount() {
        return additiveCount;
    }

    public UniversalFluidCellItem.CellFluid getOutputFluid() {
        return outputFluid;
    }

    public int getOutputAmount() {
        return outputAmount;
    }

    public int getTicks() {
        return ticks;
    }

    public boolean matches(UniversalFluidCellItem.CellFluid fluid, int amount, ItemStack additiveStack) {
        return fluid == inputFluid
                && amount >= inputAmount
                && !additiveStack.isEmpty()
                && additiveStack.getCount() >= additiveCount
                && additive.test(additiveStack);
    }
}
