package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.MaceratorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class MaceratorScreenHandler extends ScreenHandler {

    private final Inventory inv;
    private final PropertyDelegate props;

    public MaceratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, new SimpleInventory(MaceratorBlockEntity.INV_SIZE), new ArrayPropertyDelegate(4));
    }

    public MaceratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props) {
        super(ModScreenHandlers.MACERATOR, syncId);
        this.inv = inv;
        this.props = props;
        this.addProperties(props);

        // machine slots (IL classic layout)
        this.addSlot(new Slot(inv, MaceratorBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new Slot(inv, MaceratorBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new FilteredSlot(inv, MaceratorBlockEntity.SLOT_DISCHARGE, 56, 53));

        // upgrade slots (right side, 4x1)
        for (int i = 0; i < MaceratorBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(inv, MaceratorBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        // player inv
        int startX = 7;
        int startY = 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inv.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = MaceratorBlockEntity.INV_SIZE;
        final int invStart = machineSlots;
        final int invEnd = invStart + 27; // 3*9
        final int hotbarEnd = invEnd + 9;

        if (index < machineSlots) {
            // from machine -> player inventory
            if (!this.insertItem(original, invStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // from player -> machine
            // try input first, then discharge, then upgrades
            if (!this.insertItem(original, MaceratorBlockEntity.SLOT_INPUT, MaceratorBlockEntity.SLOT_INPUT + 1, false)) {
                if (!this.insertItem(original, MaceratorBlockEntity.SLOT_DISCHARGE, MaceratorBlockEntity.SLOT_DISCHARGE + 1, false)) {
                    if (!this.insertItem(original, MaceratorBlockEntity.SLOT_UPGRADE_0, MaceratorBlockEntity.SLOT_UPGRADE_0 + MaceratorBlockEntity.UPGRADE_SLOTS, false)) {
                        // move between inv/hotbar
                        if (index < invEnd) {
                            if (!this.insertItem(original, invEnd, hotbarEnd, false)) return ItemStack.EMPTY;
                        } else if (index < hotbarEnd) {
                            if (!this.insertItem(original, invStart, invEnd, false)) return ItemStack.EMPTY;
                        } else {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
        }

        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        if (original.getCount() == newStack.getCount()) return ItemStack.EMPTY;

        slot.onTakeItem(player, original);
        return newStack;
    }

    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return props.get(3); }
}
