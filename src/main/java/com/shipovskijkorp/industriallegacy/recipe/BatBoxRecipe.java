package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.tag.TagKeyRecipeIngredient;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * BatBox recipe:
 *  WCW
 *  BBB
 *  WWW
 *
 * W = any planks (minecraft:planks)
 * C = tin cable (insulation=1)  [IC2 uses insulated tin cable]
 * B = RE-Battery (any charge; NBT ignored)
 */
public final class BatBoxRecipe extends SpecialCraftingRecipe {

    private final ItemStack result;

    public BatBoxRecipe(Identifier id, CraftingRecipeCategory category, ItemStack result) {
        super(id, category);
        this.result = result;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;

        // Row 0: W C W
        if (!isPlanks(inv.getStack(0))) return false;
        if (!isTinCableIns1(inv.getStack(1))) return false;
        if (!isPlanks(inv.getStack(2))) return false;

        // Row 1: B B B
        if (!isBattery(inv.getStack(3))) return false;
        if (!isBattery(inv.getStack(4))) return false;
        if (!isBattery(inv.getStack(5))) return false;

        // Row 2: W W W
        if (!isPlanks(inv.getStack(6))) return false;
        if (!isPlanks(inv.getStack(7))) return false;
        if (!isPlanks(inv.getStack(8))) return false;

        return true;
    }

    private static boolean isPlanks(ItemStack stack) {
        return !stack.isEmpty() && stack.isIn(ItemTags.PLANKS);
    }

    private static boolean isTinCableIns1(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof CableItem)) return false;
        return CableItem.getKind(stack) == CableKind.TIN && CableItem.getInsulation(stack) == 1;
    }

    private static boolean isBattery(ItemStack stack) {
        // Accept any charge percent: crafting ingredient matching ignores NBT by default.
        return !stack.isEmpty() && stack.isOf(ModItems.RE_BATTERY);
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        return DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BATBOX_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
