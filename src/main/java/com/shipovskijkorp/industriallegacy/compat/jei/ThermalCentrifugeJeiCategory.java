package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.recipe.ThermalCentrifugeRecipe;
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

public final class ThermalCentrifugeJeiCategory implements IRecipeCategory<ThermalCentrifugeRecipe> {
    private final IDrawable icon;

    public ThermalCentrifugeJeiCategory(IGuiHelper guiHelper, ItemStack iconStack) {
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
    public void setRecipe(IRecipeLayoutBuilder builder, ThermalCentrifugeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 1)
                .addItemStacks(IlJeiUtil.ingredient(recipe.getIngredient(), recipe.getInputCount()));

        List<ItemStack> results = recipe.getResults();
        int[] ys = {1, 19, 37};
        for (int i = 0; i < results.size() && i < ys.length; i++) {
            ItemStack result = results.get(i);
            if (!result.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, 123, ys[i])
                        .addItemStack(result.copy());
            }
        }
    }

    @Override
    public void draw(ThermalCentrifugeRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawThermalCentrifugeFrame(ctx);
    }

    @Override
    public Identifier getRegistryName(ThermalCentrifugeRecipe recipe) {
        return recipe.getId();
    }
}
