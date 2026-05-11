package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.recipe.MetalFormerRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
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

public final class MetalFormerJeiCategory implements IRecipeCategory<MetalFormerRecipe> {
    public enum Mode {
        EXTRUDING(0, IlJeiRecipeTypes.METAL_FORMER_EXTRUDING, "jei.industrial_legacy.metal_former_extruding"),
        ROLLING(1, IlJeiRecipeTypes.METAL_FORMER_ROLLING, "jei.industrial_legacy.metal_former_rolling"),
        CUTTING(2, IlJeiRecipeTypes.METAL_FORMER_CUTTING, "jei.industrial_legacy.metal_former_cutting");

        final int index;
        final RecipeType<MetalFormerRecipe> type;
        final String titleKey;

        Mode(int index, RecipeType<MetalFormerRecipe> type, String titleKey) {
            this.index = index;
            this.type = type;
            this.titleKey = titleKey;
        }
    }

    private final Mode mode;
    private final IDrawable icon;
    private final Text title;
    private final Function<MetalFormerRecipe, Ingredient> inputGetter;
    private final ToIntFunction<MetalFormerRecipe> inputCountGetter;
    private final Function<MetalFormerRecipe, ItemStack> outputGetter;

    public MetalFormerJeiCategory(IGuiHelper guiHelper,
                                  Mode mode,
                                  ItemStack iconStack,
                                  Function<MetalFormerRecipe, Ingredient> inputGetter,
                                  ToIntFunction<MetalFormerRecipe> inputCountGetter,
                                  Function<MetalFormerRecipe, ItemStack> outputGetter) {
        this.mode = mode;
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, iconStack);
        this.title = Text.translatable(mode.titleKey);
        this.inputGetter = inputGetter;
        this.inputCountGetter = inputCountGetter;
        this.outputGetter = outputGetter;
    }

    @Override
    public RecipeType<MetalFormerRecipe> getRecipeType() {
        return mode.type;
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
    public void setRecipe(IRecipeLayoutBuilder builder, MetalFormerRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 17, 1)
                .addItemStacks(IlJeiUtil.ingredient(inputGetter.apply(recipe), inputCountGetter.applyAsInt(recipe)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 19)
                .addItemStack(outputGetter.apply(recipe).copy());
    }

    @Override
    public void draw(MetalFormerRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawMetalFormerFrame(ctx);
        ctx.drawItem(modeIcon(), 70, 35);
    }

    @Override
    public Identifier getRegistryName(MetalFormerRecipe recipe) {
        return recipe.getId();
    }

    private ItemStack modeIcon() {
        return switch (mode) {
            case EXTRUDING -> IlJeiUtil.cable(CableKind.COPPER, 0);
            case ROLLING -> new ItemStack(ModItems.FORGE_HAMMER);
            case CUTTING -> new ItemStack(ModItems.CUTTER);
        };
    }
}
