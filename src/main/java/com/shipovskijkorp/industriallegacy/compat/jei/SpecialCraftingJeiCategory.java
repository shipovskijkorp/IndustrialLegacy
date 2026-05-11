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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class SpecialCraftingJeiCategory implements IRecipeCategory<IlSpecialCraftingRecipe> {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 74;
    private final IDrawable icon;

    public SpecialCraftingJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new net.minecraft.item.ItemStack(net.minecraft.item.Items.CRAFTING_TABLE));
    }

    @Override
    public RecipeType<IlSpecialCraftingRecipe> getRecipeType() {
        return IlJeiRecipeTypes.SPECIAL_CRAFTING;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.special_crafting");
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
    public void setRecipe(IRecipeLayoutBuilder builder, IlSpecialCraftingRecipe recipe, IFocusGroup focuses) {
        List<List<net.minecraft.item.ItemStack>> inputs = recipe.inputs();
        for (int i = 0; i < 9; i++) {
            List<net.minecraft.item.ItemStack> stacks = inputs.get(i);
            if (stacks == null || stacks.isEmpty()) continue;
            int x = 4 + (i % 3) * 18;
            int y = 10 + (i / 3) * 18;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y).addItemStacks(stacks);
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 28)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(IlSpecialCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        for (int i = 0; i < 9; i++) {
            IlJeiDraw.slot(ctx, 4 + (i % 3) * 18, 10 + (i / 3) * 18);
        }
        IlJeiDraw.largeSlot(ctx, 116, 24);
    }

    @Override
    public Identifier getRegistryName(IlSpecialCraftingRecipe recipe) {
        return recipe.id();
    }
}
