package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
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

public final class SolidCanningJeiCategory implements IRecipeCategory<CanningRecipe> {
    private final IDrawable icon;

    public SolidCanningJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<CanningRecipe> getRecipeType() {
        return IlJeiRecipeTypes.SOLID_CANNING;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.solid_canning");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CanningRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 20)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getFill(), recipe.getFillCount()));
        builder.addSlot(RecipeIngredientRole.INPUT, 67, 20)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getContainer(), recipe.getContainerCount()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 20)
                .addItemStack(recipe.getResultStack().copy());
    }

    @Override
    public void draw(CanningRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawSolidCannerFrame(ctx);
    }

    @Override
    public Identifier getRegistryName(CanningRecipe recipe) {
        return IlJeiUtil.suffix(recipe.getId(), "_solid_canner");
    }
}
