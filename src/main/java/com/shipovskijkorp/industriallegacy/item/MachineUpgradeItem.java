package com.shipovskijkorp.industriallegacy.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Marker item for IC2-style machine upgrades.
 *
 * The upgrade effects are intentionally not implemented yet; this class only
 * lets machine upgrade slots distinguish upgrade modules from regular items.
 */
public class MachineUpgradeItem extends Item {
    public enum UpgradeType {
        OVERCLOCKER,
        TRANSFORMER,
        ENERGY_STORAGE,
        REDSTONE_INVERTER,
        EJECTOR,
        ADVANCED_EJECTOR,
        PULLING,
        ADVANCED_PULLING,
        FLUID_EJECTOR,
        FLUID_PULLING
    }

    private final UpgradeType type;

    public MachineUpgradeItem(Settings settings, UpgradeType type) {
        super(settings);
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    public static boolean isUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MachineUpgradeItem;
    }
}
