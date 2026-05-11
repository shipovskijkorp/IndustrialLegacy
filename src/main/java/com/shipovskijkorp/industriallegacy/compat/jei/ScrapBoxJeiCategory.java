package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class ScrapBoxJeiCategory implements IRecipeCategory<ScrapBoxJeiRecipe> {
    private final IDrawable icon;

    public ScrapBoxJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new ItemStack(ModItems.SCRAP_BOX));
    }

    @Override
    public RecipeType<ScrapBoxJeiRecipe> getRecipeType() {
        return IlJeiRecipeTypes.SCRAP_BOX;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("item.industrial_legacy.scrap_box");
    }

    @Override
    public int getWidth() {
        return 126;
    }

    @Override
    public int getHeight() {
        return 26;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ScrapBoxJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 0, 4)
                .addItemStack(new ItemStack(ModItems.SCRAP_BOX));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 4)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(ScrapBoxJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, DrawContext ctx, double mouseX, double mouseY) {
        IlJeiDraw.drawScrapBoxFrame(ctx);
        String value = recipe.chance() < 0.001f ? "< 0.01" : "  " + String.format(Locale.ROOT, "%.2f", recipe.chance() * 100.0f);
        ctx.drawText(MinecraftClient.getInstance().textRenderer, value + "%", 86, 9, 0x404040, false);
    }

    @Override
    public Identifier getRegistryName(ScrapBoxJeiRecipe recipe) {
        Identifier out = net.minecraft.registry.Registries.ITEM.getId(recipe.output().getItem());
        return IlJeiUtil.id("scrap_box/" + out.getNamespace() + "/" + out.getPath());
    }
}
