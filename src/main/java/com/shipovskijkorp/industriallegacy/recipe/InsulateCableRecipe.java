package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Crafting recipe: 1x cable + 1x rubber -> cable with insulation+1 (clamped by kind.maxInsulation).
 *
 * <p>Matches IC2 behavior: one rubber adds exactly one insulation layer.</p>
 */
public final class InsulateCableRecipe extends SpecialCraftingRecipe {

    private final Ingredient material;
    private final ItemStack exampleOutput;

    public InsulateCableRecipe(Identifier id,
                               CraftingRecipeCategory category,
                               Ingredient material,
                               ItemStack exampleOutput) {
        super(id, category);
        this.material = material;
        this.exampleOutput = exampleOutput;
    }

    public Ingredient material() {
        return material;
    }

    public ItemStack exampleOutput() {
        return exampleOutput;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        int cableCount = 0;
        int materialCount = 0;
        ItemStack cableStack = ItemStack.EMPTY;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof CableItem) {
                cableCount++;
                if (cableCount > 1) return false;
                cableStack = stack;
            } else if (material.test(stack)) {
                materialCount++;
                if (materialCount > 1) return false;
            } else {
                return false;
            }
        }

        if (cableCount != 1 || materialCount != 1) return false;

        CableKind kind = CableItem.getKind(cableStack);
        int insulation = CableItem.getInsulation(cableStack);

        // Can't insulate past max.
        return insulation < kind.maxInsulation;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        // Find the cable, then increment insulation by exactly 1.
        ItemStack cableStack = ItemStack.EMPTY;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && (s.getItem() instanceof CableItem)) {
                cableStack = s;
                break;
            }
        }
        if (cableStack.isEmpty()) return ItemStack.EMPTY;

        CableKind kind = CableItem.getKind(cableStack);
        int insulation = CableItem.getInsulation(cableStack);

        if (insulation >= kind.maxInsulation) return ItemStack.EMPTY;

        return CableItem.createStack(ModItems.CABLE, kind, insulation + 1);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inv) {
        // Rubber and cable are consumed; no remainder.
        return DefaultedList.ofSize(inv.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return exampleOutput;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.INSULATE_CABLE_SERIALIZER;
    }

    /**
     * IMPORTANT:
     * Crafting tables only search RecipeType.CRAFTING.
     * If we return a custom RecipeType, the recipe will never match in a crafting grid.
     */
    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
