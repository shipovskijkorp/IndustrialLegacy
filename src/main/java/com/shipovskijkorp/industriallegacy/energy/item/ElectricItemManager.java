package com.shipovskijkorp.industriallegacy.energy.item;

import net.minecraft.item.ItemStack;

/**
 * Helpers for working with {@link IElectricItem} through an {@link ItemStack}.
 */
public final class ElectricItemManager {
    private ElectricItemManager() {}

    public static boolean isElectric(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof IElectricItem;
    }

    public static long getEnergy(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        return ei.getEnergy(stack);
    }

    public static long getCapacity(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        return ei.getCapacity(stack);
    }

    public static long getFree(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        return Math.max(0L, ei.getCapacity(stack) - ei.getEnergy(stack));
    }

    public static long getTransferLimit(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        return Math.max(0L, ei.getTransferLimit(stack));
    }

    public static int getTier(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0;
        return ei.getTier(stack);
    }

    public static boolean canProvideEnergy(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.getItem() instanceof IElectricItem ei
                && ei.canProvideEnergy(stack);
    }

    /** @return accepted EU */
    public static long charge(ItemStack stack, long amount, boolean simulate) {
        return charge(stack, amount, Integer.MAX_VALUE, false, simulate);
    }

    /**
     * IC2-like charge with tier and transfer-limit controls.
     *
     * @return accepted EU
     */
    public static long charge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IElectricItem ei)) return 0L;
        if (amount <= 0L || stack.getCount() > 1 || ei.getTier(stack) > tier) return 0L;

        long limit = ignoreTransferLimit ? amount : Math.min(amount, ei.getTransferLimit(stack));
        long free = Math.max(0L, ei.getCapacity(stack) - ei.getEnergy(stack));
        long accepted = Math.min(limit, free);

        if (!simulate && accepted > 0L) {
            ei.setEnergy(stack, ei.getEnergy(stack) + accepted);
        }
        return accepted;
    }

    /** @return extracted EU */
    public static long discharge(ItemStack stack, long amount, boolean simulate) {
        return discharge(stack, amount, Integer.MAX_VALUE, false, false, simulate);
    }

    /**
     * IC2-like discharge with tier, transfer-limit and external-provider controls.
     *
     * @return extracted EU
     */
    public static long discharge(ItemStack stack, long amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IElectricItem ei)) return 0L;
        if (amount <= 0L || stack.getCount() > 1 || ei.getTier(stack) > tier) return 0L;
        if (externally && !ei.canProvideEnergy(stack)) return 0L;

        long limit = ignoreTransferLimit ? amount : Math.min(amount, ei.getTransferLimit(stack));
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(limit, stored);

        if (!simulate && extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }

    public static float getChargeRatio(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0.0f;
        return ei.getChargeRatio(stack);
    }

    public static void setEnergy(ItemStack stack, long energy) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return;
        ei.setEnergy(stack, energy);
    }
}
