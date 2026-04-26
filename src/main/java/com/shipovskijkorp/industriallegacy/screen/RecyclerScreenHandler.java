package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.RecyclerBlockEntity;
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

public class RecyclerScreenHandler extends ScreenHandler {
    private final Inventory inv;
    private final PropertyDelegate props;

    public RecyclerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, new SimpleInventory(RecyclerBlockEntity.INV_SIZE), new ArrayPropertyDelegate(4));
    }

    public RecyclerScreenHandler(int syncId, PlayerInventory playerInv, RecyclerBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps());
    }

    public RecyclerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props) {
        super(ModScreenHandlers.RECYCLER, syncId);
        this.inv = inv;
        this.props = props;
        this.addProperties(props);

        this.addSlot(new Slot(inv, RecyclerBlockEntity.SLOT_INPUT, 56, 17) {
            @Override public boolean canInsert(ItemStack stack) {
                return !RecyclerBlockEntity.isRecyclerBlacklisted(stack);
            }
        });
        this.addSlot(new Slot(inv, RecyclerBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, RecyclerBlockEntity.SLOT_DISCHARGE, 56, 53));

        for (int i = 0; i < RecyclerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(inv, RecyclerBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

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

    @Override public boolean canUse(PlayerEntity player) { return inv.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;
        ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = RecyclerBlockEntity.INV_SIZE;
        final int invStart = machineSlots;
        final int invEnd = invStart + 27;
        final int hotbarEnd = invEnd + 9;

        if (index < machineSlots) {
            if (!this.insertItem(original, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
        } else {
            if (!RecyclerBlockEntity.isRecyclerBlacklisted(original)
                    && this.insertItem(original, RecyclerBlockEntity.SLOT_INPUT, RecyclerBlockEntity.SLOT_INPUT + 1, false)) {
                // moved to input
            } else if (this.insertItem(original, RecyclerBlockEntity.SLOT_DISCHARGE, RecyclerBlockEntity.SLOT_DISCHARGE + 1, false)) {
                // moved to discharge
            } else if (this.insertItem(original, RecyclerBlockEntity.SLOT_UPGRADE_0, RecyclerBlockEntity.SLOT_UPGRADE_0 + RecyclerBlockEntity.UPGRADE_SLOTS, false)) {
                // moved to upgrades
            } else if (index < invEnd) {
                if (!this.insertItem(original, invEnd, hotbarEnd, false)) return ItemStack.EMPTY;
            } else if (index < hotbarEnd) {
                if (!this.insertItem(original, invStart, invEnd, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
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
