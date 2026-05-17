package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.AbstractChargepadBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/** Shared container layout for IC2-style charge pads. */
public abstract class AbstractChargepadScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 2;
    public static final int PROP_COUNT = 3;
    private static final int CONTAINER_SLOT_COUNT = SLOT_COUNT;

    public final BlockPos pos;
    private final Inventory inventory;
    private final PropertyDelegate properties;

    protected AbstractChargepadScreenHandler(
            ScreenHandlerType<?> type,
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties,
            BlockPos pos
    ) {
        super(type, syncId);
        checkSize(inventory, SLOT_COUNT);
        checkDataCount(properties, PROP_COUNT);

        this.pos = pos;
        this.inventory = inventory;
        this.properties = properties;

        addSlot(new NonStackingElectricSlot(inventory, 0, 56, 17));
        addSlot(new NonStackingElectricSlot(inventory, 1, 56, 53));

        int startX = 8;
        int startY = 79;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, startX + col * 18, hotbarY));
        }

        addProperties(properties);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return moved;

        ItemStack original = slot.getStack();
        moved = original.copy();

        if (index < CONTAINER_SLOT_COUNT) {
            if (!this.insertItem(original, CONTAINER_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertItem(original, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return moved;
    }

    public int getEuStored() {
        return properties.get(0);
    }

    public int getEuCap() {
        return properties.get(1);
    }

    public int getRedstoneMode() {
        return properties.get(2);
    }

    protected static final class NonStackingElectricSlot extends Slot {
        private final int inventoryIndex;

        protected NonStackingElectricSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
            this.inventoryIndex = index;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.inventory.isValid(this.inventoryIndex, stack);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return 1;
        }
    }
}
