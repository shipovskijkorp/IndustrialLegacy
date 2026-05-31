package com.shipovskijkorp.industriallegacy.energy.item;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/**
 * IL-like charge/discharge slot rules.
 *
 * <p>Source of truth: IL 1.12.2 InvSlotCharge and InvSlotDischarge.</p>
 */
public final class ElectricSlotHelper {
    private ElectricSlotHelper() {}

    /**
     * IL InvSlotCharge.accepts: item must be chargeable by this tier.
     */
    public static boolean canCharge(ItemStack stack, int tier) {
        return ElectricItemManager.charge(stack, Long.MAX_VALUE, tier, true, true) > 0L;
    }

    /**
     * IL InvSlotDischarge.accepts: energy-value item or externally dischargeable electric item.
     */
    public static boolean canDischarge(ItemStack stack, int tier) {
        return canDischarge(stack, tier, true);
    }

    public static boolean canDischarge(ItemStack stack, int tier, boolean allowRedstoneDust) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.isOf(Items.REDSTONE) && !allowRedstoneDust) return false;
        if (getEnergyValue(stack) > 0L) return true;
        return ElectricItemManager.discharge(stack, Long.MAX_VALUE, tier, true, true, true) > 0L;
    }

    /**
     * IL Info.itemInfo.getEnergyValue equivalent for items currently present in IL.
     */
    public static long getEnergyValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        if (stack.isOf(Items.REDSTONE)) return 800L;
        if (stack.isOf(ModItems.ENERGIUM_DUST)) return 16_000L;
        return 0L;
    }

    /**
     * Charge a stack from a tile buffer. Mirrors InvSlotCharge.charge(amount).
     *
     * @return EU moved from the tile into the item.
     */
    public static long chargeFromStorage(ItemStack stack, long availableEu, int tier, boolean simulate) {
        if (availableEu <= 0L) return 0L;
        return ElectricItemManager.charge(stack, availableEu, tier, false, simulate);
    }

    /**
     * Discharge a stack into a tile buffer. Mirrors InvSlotDischarge.discharge(amount, false).
     *
     * @return EU provided by the stack or single-use energy item.
     */
    public static long dischargeIntoStorage(ItemStack stack, long requestedEu, int tier, boolean allowRedstoneDust, boolean simulate) {
        if (requestedEu <= 0L || stack == null || stack.isEmpty()) return 0L;
        if (stack.isOf(Items.REDSTONE) && !allowRedstoneDust) return 0L;

        long extracted = ElectricItemManager.discharge(stack, requestedEu, tier, false, true, simulate);
        if (extracted > 0L) return extracted;

        long value = getEnergyValue(stack);
        if (value <= 0L) return 0L;
        if (!simulate) {
            stack.decrement(1);
        }
        return value;
    }
}
