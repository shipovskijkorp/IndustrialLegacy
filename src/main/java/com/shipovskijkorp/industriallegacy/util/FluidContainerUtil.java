package com.shipovskijkorp.industriallegacy.util;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import org.jetbrains.annotations.Nullable;

public final class FluidContainerUtil {
    public static final int CELL_MB = 1000;
    public static final int BUCKET_MB = 1000;
    public static final int BOTTLE_MB = 250;

    private FluidContainerUtil() {}

    public static boolean isEmptyFluidCell(ItemStack stack) {
        return stack.getItem() instanceof UniversalFluidCellItem
                && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.EMPTY;
    }

    public static boolean isWaterContainer(ItemStack stack) {
        DrainData data = getDrainData(stack);
        return data != null && data.fluid() == UniversalFluidCellItem.CellFluid.WATER;
    }

    public static boolean isEmptyContainerFor(UniversalFluidCellItem.CellFluid fluid, ItemStack stack) {
        return getFillData(stack, fluid, Integer.MAX_VALUE) != null;
    }

    public static @Nullable DrainData getDrainData(ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof UniversalFluidCellItem) {
            UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) return null;
            return new DrainData(fluid, CELL_MB, UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
        }

        if (stack.isOf(Items.WATER_BUCKET)) {
            return new DrainData(UniversalFluidCellItem.CellFluid.WATER, BUCKET_MB, new ItemStack(Items.BUCKET));
        }
        if (stack.isOf(Items.LAVA_BUCKET)) {
            return new DrainData(UniversalFluidCellItem.CellFluid.LAVA, BUCKET_MB, new ItemStack(Items.BUCKET));
        }
        if (stack.isOf(Items.MILK_BUCKET)) {
            return new DrainData(UniversalFluidCellItem.CellFluid.MILK, BUCKET_MB, new ItemStack(Items.BUCKET));
        }
        if (isWaterBottle(stack)) {
            return new DrainData(UniversalFluidCellItem.CellFluid.WATER, BOTTLE_MB, new ItemStack(Items.GLASS_BOTTLE));
        }
        return null;
    }

    public static @Nullable FillData getFillData(ItemStack stack, UniversalFluidCellItem.CellFluid fluid, int availableMb) {
        if (stack.isEmpty() || fluid == UniversalFluidCellItem.CellFluid.EMPTY || availableMb <= 0) return null;

        if (stack.getItem() instanceof UniversalFluidCellItem) {
            if (UniversalFluidCellItem.getFluid(stack) != UniversalFluidCellItem.CellFluid.EMPTY || availableMb < CELL_MB) return null;
            return new FillData(CELL_MB, UniversalFluidCellItem.createStack(fluid));
        }

        if (stack.isOf(Items.BUCKET) && availableMb >= BUCKET_MB) {
            if (fluid == UniversalFluidCellItem.CellFluid.WATER) return new FillData(BUCKET_MB, new ItemStack(Items.WATER_BUCKET));
            if (fluid == UniversalFluidCellItem.CellFluid.LAVA) return new FillData(BUCKET_MB, new ItemStack(Items.LAVA_BUCKET));
            if (fluid == UniversalFluidCellItem.CellFluid.MILK) return new FillData(BUCKET_MB, new ItemStack(Items.MILK_BUCKET));
        }

        if (stack.isOf(Items.GLASS_BOTTLE) && fluid == UniversalFluidCellItem.CellFluid.WATER && availableMb >= BOTTLE_MB) {
            return new FillData(BOTTLE_MB, PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER));
        }
        return null;
    }

    public static boolean isWaterBottle(ItemStack stack) {
        return stack.isOf(Items.POTION) && PotionUtil.getPotion(stack) == Potions.WATER;
    }

    public record DrainData(UniversalFluidCellItem.CellFluid fluid, int amountMb, ItemStack output) { }
    public record FillData(int amountMb, ItemStack output) { }
}
