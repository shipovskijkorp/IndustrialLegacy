package com.shipovskijkorp.industriallegacy.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class SimpleMachineJeiCategory<T> implements IRecipeCategory<T> {
    private final RecipeType<T> recipeType;
    private final Text title;
    private final IDrawable background;
    private final IDrawable icon;
    private final Function<T, Ingredient> inputGetter;
    private final ToIntFunction<T> inputCountGetter;
    private final Function<T, ItemStack> outputGetter;

    public SimpleMachineJeiCategory(IGuiHelper guiHelper,
                                    RecipeType<T> recipeType,
                                    String titleKey,
                                    ItemStack iconStack,
                                    Function<T, Ingredient> inputGetter,
                                    ToIntFunction<T> inputCountGetter,
                                    Function<T, ItemStack> outputGetter) {
        this.recipeType = recipeType;
        this.title = Text.translatable(titleKey);
        this.background = guiHelper.createBlankDrawable(128, 54);
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
        this.inputGetter = inputGetter;
        this.inputCountGetter = inputCountGetter;
        this.outputGetter = outputGetter;
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public Text getTitle() {
        return title;
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
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 18, 19)
                .addItemStacks(IlJeiUtil.ingredient(inputGetter.apply(recipe), inputCountGetter.applyAsInt(recipe)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 19)
                .addItemStack(outputGetter.apply(recipe).copy());
    }

    @Override
    public Identifier getRegistryName(T recipe) {
        if (recipe instanceof net.minecraft.recipe.Recipe<?> mcRecipe) {
            return mcRecipe.getId();
        }
        return null;
    }
}
