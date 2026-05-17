package com.shipovskijkorp.industriallegacy.screen.slot;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Slot that delegates GUI insertion checks to the backing inventory. */
public class FilteredSlot extends Slot {
    private final int inventoryIndex;

    public FilteredSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.inventoryIndex = index;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return this.inventory.isValid(this.inventoryIndex, stack);
    }
}
