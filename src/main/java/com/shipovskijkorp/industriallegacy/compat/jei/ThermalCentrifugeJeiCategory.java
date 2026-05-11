package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.ThermalCentrifugeRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ThermalCentrifugeJeiCategory implements IRecipeCategory<ThermalCentrifugeRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public ThermalCentrifugeJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(150, 70);
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<ThermalCentrifugeRecipe> getRecipeType() {
        return IlJeiRecipeTypes.THERMAL_CENTRIFUGE;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.thermal_centrifuge");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalCentrifugeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 26)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getIngredient(), recipe.getInputCount()));

        List<ItemStack> results = recipe.getResults();
        int[][] slots = new int[][]{{100, 6}, {118, 26}, {100, 46}};
        for (int i = 0; i < results.size() && i < slots.length; i++) {
            ItemStack result = results.get(i);
            if (!result.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, slots[i][0], slots[i][1])
                        .addItemStack(result.copy());
            }
        }
    }

    @Override
    public Identifier getRegistryName(ThermalCentrifugeRecipe recipe) {
        return recipe.getId();
    }
}
