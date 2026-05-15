package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.CanningEnrichRecipe;
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

public final class CanningEnrichJeiCategory implements IRecipeCategory<CanningEnrichRecipe> {
    private static final int WIDTH = 96;
    private static final int HEIGHT = 81;

    private final IDrawable icon;

    public CanningEnrichJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<CanningEnrichRecipe> getRecipeType() {
        return IlJeiRecipeTypes.CANNING_ENRICH;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.canning");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CanningEnrichRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 28)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getAdditive(), recipe.getAdditiveCount()));
    }

    @Override
    public void draw(CanningEnrichRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawCannerFrame(ctx, com.shipovskijkorp.industriallegacy.block.entity.CannerBlockEntity.Mode.ENRICH_LIQUID);
        IlJeiDraw.drawCannerTank(ctx, -1, 26, recipe.getInputFluid(), recipe.getInputAmount(), 8000);
        IlJeiDraw.drawCannerTank(ctx, 77, 26, recipe.getOutputFluid(), recipe.getOutputAmount(), 8000);
    }

    @Override
    public Identifier getRegistryName(CanningEnrichRecipe recipe) {
        return recipe.getId();
    }
}
