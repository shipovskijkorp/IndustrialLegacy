package com.shipovskijkorp.industriallegacy.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class SimpleMachineJeiCategory<T> implements IRecipeCategory<T> {
    public enum Progress {
        CRUSH,
        TRIANGLE,
        RECYCLER,
        DROP,
        ARROW
    }

    private final RecipeType<T> recipeType;
    private final Text title;
    private final IDrawable icon;
    private final Function<T, Ingredient> inputGetter;
    private final ToIntFunction<T> inputCountGetter;
    private final Function<T, ItemStack> outputGetter;
    private final Progress progress;

    public SimpleMachineJeiCategory(IGuiHelper guiHelper,
                                    RecipeType<T> recipeType,
                                    String titleKey,
                                    ItemStack iconStack,
                                    Function<T, Ingredient> inputGetter,
                                    ToIntFunction<T> inputCountGetter,
                                    Function<T, ItemStack> outputGetter,
                                    Progress progress) {
        this.recipeType = recipeType;
        this.title = Text.translatable(titleKey);
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
        this.inputGetter = inputGetter;
        this.inputCountGetter = inputCountGetter;
        this.outputGetter = outputGetter;
        this.progress = progress;
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
    public int getWidth() {
        return IlJeiDraw.DYNAMIC_WIDTH;
    }

    @Override
    public int getHeight() {
        return IlJeiDraw.DYNAMIC_HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 1)
                .addItemStacks(IlJeiUtil.ingredient(inputGetter.apply(recipe), inputCountGetter.applyAsInt(recipe)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 19)
                .addItemStack(outputGetter.apply(recipe).copy());
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawSimpleMachineFrame(ctx, progress);
    }

    @Override
    public Identifier getRegistryName(T recipe) {
        if (recipe instanceof net.minecraft.recipe.Recipe<?> mcRecipe) {
            return mcRecipe.getId();
        }
        return null;
    }
}
