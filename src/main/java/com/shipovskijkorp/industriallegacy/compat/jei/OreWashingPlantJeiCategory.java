package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.OreWashingRecipe;
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

import java.util.List;

public final class OreWashingPlantJeiCategory implements IRecipeCategory<OreWashingRecipe> {
    private static final int WIDTH = IlJeiDraw.DYNAMIC_WIDTH;
    private static final int HEIGHT = 64;

    private final IDrawable icon;

    public OreWashingPlantJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<OreWashingRecipe> getRecipeType() {
        return IlJeiRecipeTypes.ORE_WASHING;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("jei.industrial_legacy.ore_washing_plant");
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
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OreWashingRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 105, 1)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getIngredient(), recipe.getInputCount()));

        List<ItemStack> results = recipe.getResults();
        int[] outputX = {87, 105, 123};
        for (int i = 0; i < results.size() && i < outputX.length; i++) {
            ItemStack result = results.get(i);
            if (!result.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, outputX[i], 46)
                        .addItemStack(result.copy());
            }
        }
    }

    @Override
    public void draw(OreWashingRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawOreWashingPlantFrame(ctx);
    }

    @Override
    public Identifier getRegistryName(OreWashingRecipe recipe) {
        return recipe.getId();
    }
}
