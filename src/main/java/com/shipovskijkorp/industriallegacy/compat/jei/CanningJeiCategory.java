package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
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

public final class CanningJeiCategory implements IRecipeCategory<CanningRecipe> {
    private final IDrawable background;
    private final IDrawable icon;

    public CanningJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.background = guiHelper.createBlankDrawable(144, 64);
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<CanningRecipe> getRecipeType() {
        return IlJeiRecipeTypes.CANNING;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.canning");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CanningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 10)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getContainer(), recipe.getContainerCount()));
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 38)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getFill(), recipe.getFillCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 24)
                .addItemStack(recipe.getResultStack().copy());
    }

    @Override
    public Identifier getRegistryName(CanningRecipe recipe) {
        return recipe.getId();
    }
}
