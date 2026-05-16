package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModFluids;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

final class IlJeiUtil {
    private IlJeiUtil() {}

    static Identifier id(String path) {
        return new Identifier(IndustrialLegacy.MOD_ID, path);
    }

    static ItemStack stack(ItemConvertible item) {
        return new ItemStack(item);
    }

    static ItemStack cable(CableKind kind, int insulation) {
        return CableItem.createStack(ModItems.CABLE, kind, insulation);
    }

    static ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(Math.max(1, count));
        return copy;
    }

    static List<ItemStack> item(ItemConvertible item) {
        return List.of(new ItemStack(item));
    }

    static ItemStack fluidSheet(UniversalFluidCellItem.CellFluid fluid) {
        if (fluid == UniversalFluidCellItem.CellFluid.WATER) {
            return new ItemStack(ModItems.WATER_SHEET);
        }
        if (fluid == UniversalFluidCellItem.CellFluid.LAVA) {
            return new ItemStack(ModItems.LAVA_SHEET);
        }
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            return ItemStack.EMPTY;
        }

        ModFluids.Ic2FluidEntry entry = ModFluids.getEntry(fluid.id);
        return entry == null ? ItemStack.EMPTY : new ItemStack(entry.item());
    }

    static List<ItemStack> ingredient(Ingredient ingredient, int count) {
        ItemStack[] matching = ingredient.getMatchingStacks();
        if (matching.length == 0) {
            return List.of(ItemStack.EMPTY);
        }

        List<ItemStack> out = new ArrayList<>(matching.length);
        for (ItemStack stack : matching) {
            out.add(copyWithCount(stack, count));
        }
        return out;
    }

    static Identifier suffix(Identifier id, String suffix) {
        return new Identifier(id.getNamespace(), id.getPath() + suffix);
    }
}
