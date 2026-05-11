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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RecyclerJeiCategory implements IRecipeCategory<RecyclerJeiRecipe> {
    private final IDrawable icon;

    public RecyclerJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<RecyclerJeiRecipe> getRecipeType() {
        return IlJeiRecipeTypes.RECYCLER;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.recycler");
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecyclerJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 0).addItemStacks(recipe.inputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 115, 18).addItemStack(recipe.output());
    }

    @Override
    public void draw(RecyclerJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawSimpleMachineFrame(ctx, SimpleMachineJeiCategory.Progress.RECYCLER);
    }

    @Override
    public Identifier getRegistryName(RecyclerJeiRecipe recipe) {
        return IlJeiUtil.id("recycler_scrap");
    }
}
