package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * IC2 detector/splitter cable crafting.
 *
 * <p>Vanilla shaped recipes can't express the NBT-backed cable variants used by IL, so these
 * two IC2 cable recipes are implemented as special crafting recipes.</p>
 */
public final class CableVariantCraftingRecipe extends SpecialCraftingRecipe {
    private final CableKind resultKind;
    private final ItemStack result;

    public CableVariantCraftingRecipe(Identifier id, CraftingRecipeCategory category, CableKind resultKind) {
        super(id, category);
        if (resultKind != CableKind.DETECTOR && resultKind != CableKind.SPLITTER) {
            throw new IllegalArgumentException("Only detector/splitter cable variants are supported: " + resultKind);
        }
        this.resultKind = resultKind;
        this.result = CableItem.createStack(ModItems.CABLE, resultKind, 0);
    }

    public CableKind resultKind() {
        return resultKind;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;
        return switch (resultKind) {
            case DETECTOR -> matchesDetector(inv);
            case SPLITTER -> matchesSplitter(inv);
            default -> false;
        };
    }

    /** IC2: " C |RIR| R ". */
    private static boolean matchesDetector(RecipeInputInventory inv) {
        return empty(inv, 0) && inv.getStack(1).isOf(ModItems.ELECTRONIC_CIRCUIT) && empty(inv, 2)
                && inv.getStack(3).isOf(Items.REDSTONE) && isInsulatedIronCable(inv.getStack(4)) && inv.getStack(5).isOf(Items.REDSTONE)
                && empty(inv, 6) && inv.getStack(7).isOf(Items.REDSTONE) && empty(inv, 8);
    }

    /** IC2: " R |ILI| R ". */
    private static boolean matchesSplitter(RecipeInputInventory inv) {
        return empty(inv, 0) && inv.getStack(1).isOf(Items.REDSTONE) && empty(inv, 2)
                && isInsulatedIronCable(inv.getStack(3)) && inv.getStack(4).isOf(Items.LEVER) && isInsulatedIronCable(inv.getStack(5))
                && empty(inv, 6) && inv.getStack(7).isOf(Items.REDSTONE) && empty(inv, 8);
    }

    private static boolean empty(RecipeInputInventory inv, int slot) {
        return inv.getStack(slot).isEmpty();
    }

    private static boolean isInsulatedIronCable(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof CableItem
                && CableItem.getKind(stack) == CableKind.IRON
                && CableItem.getInsulation(stack) == 3;
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
        return ModRecipes.CABLE_VARIANT_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
