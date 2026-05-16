package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.block.entity.CannerBlockEntity;
import com.shipovskijkorp.industriallegacy.recipe.CanningFluidRecipe;
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

public final class CanningFluidJeiCategory implements IRecipeCategory<CanningFluidRecipe> {
    public enum Kind {
        EMPTY_LIQUID,
        BOTTLE_LIQUID
    }

    private static final int WIDTH = 96;
    private static final int HEIGHT = 81;

    private final Kind kind;
    private final IDrawable icon;

    public CanningFluidJeiCategory(IGuiHelper guiHelper, Kind kind, ItemStack iconStack) {
        this.kind = kind;
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<CanningFluidRecipe> getRecipeType() {
        return kind == Kind.EMPTY_LIQUID ? IlJeiRecipeTypes.CANNING_EMPTY_LIQUID : IlJeiRecipeTypes.CANNING_BOTTLE_LIQUID;
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
    public void setRecipe(IRecipeLayoutBuilder builder, CanningFluidRecipe recipe, IFocusGroup focuses) {
        if (kind == Kind.EMPTY_LIQUID) {
            builder.addSlot(RecipeIngredientRole.INPUT, 2, 2)
                    .addItemStack(recipe.getFilledCell());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 2)
                    .addItemStack(recipe.getEmptyCell());
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
                    .addItemStack(IlJeiUtil.fluidSheet(recipe.getFluid()));
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 2, 2)
                    .addItemStack(recipe.getEmptyCell());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 2)
                    .addItemStack(recipe.getFilledCell());
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                    .addItemStack(IlJeiUtil.fluidSheet(recipe.getFluid()));
        }
    }

    @Override
    public void draw(CanningFluidRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        if (kind == Kind.EMPTY_LIQUID) {
            IlJeiDraw.drawCannerFrame(ctx, CannerBlockEntity.Mode.EMPTY_LIQUID);
        } else {
            IlJeiDraw.drawCannerFrame(ctx, CannerBlockEntity.Mode.BOTTLE_LIQUID);
        }
        IlJeiDraw.drawCannerTank(ctx, -1, 26, recipe.getFluid(), recipe.getAmount(), 8000);
    }


    @Override
    public List<Text> getTooltipStrings(CanningFluidRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return IlJeiDraw.isCannerTankHovered(-1, 26, mouseX, mouseY)
                ? IlJeiDraw.cannerTankTooltip(recipe.getFluid(), recipe.getAmount(), 8000)
                : List.of();
    }

    @Override
    public Identifier getRegistryName(CanningFluidRecipe recipe) {
        return recipe.getId();
    }
}
