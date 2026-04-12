package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.SolidCannerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class SolidCannerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate props;

    public SolidCannerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, getBlockEntityInventory(playerInv, buf));
    }

    public SolidCannerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.SOLID_CANNER, syncId);
        if (inv == null) {
            this.inventory = new SimpleInventory(SolidCannerBlockEntity.INV_SIZE);
            this.props = emptyProps();
        } else {
            this.inventory = inv;
            this.props = inv instanceof SolidCannerBlockEntity be ? be.getGuiProps() : emptyProps();
        }

        this.addSlot(new Slot(this.inventory, SolidCannerBlockEntity.SLOT_CONTAINER, 37, 36) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return SolidCannerBlockEntity.isValidContainer(stack); }
        });
        this.addSlot(new Slot(this.inventory, SolidCannerBlockEntity.SLOT_FILL, 67, 36));
        this.addSlot(new Slot(this.inventory, SolidCannerBlockEntity.SLOT_OUTPUT, 116, 36) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(this.inventory, SolidCannerBlockEntity.SLOT_DISCHARGE, 8, 62));
        for (int i = 0; i < SolidCannerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(this.inventory, SolidCannerBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        int invX = 8;
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
        }

        this.addProperties(this.props);
    }

    private static PropertyDelegate emptyProps() {
        return new PropertyDelegate() {
            @Override public int size() { return 4; }
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) { }
        };
    }

    private static Inventory getBlockEntityInventory(PlayerInventory playerInv, PacketByteBuf buf) {
        if (playerInv == null || playerInv.player == null || buf == null) return null;
        var be = playerInv.player.getWorld().getBlockEntity(buf.readBlockPos());
        return be instanceof SolidCannerBlockEntity sc ? sc : null;
    }

    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }
    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return Math.max(1, props.get(3)); }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = SolidCannerBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, SolidCannerBlockEntity.SLOT_CONTAINER, SolidCannerBlockEntity.SLOT_CONTAINER + 1, false)
                    && !this.insertItem(original, SolidCannerBlockEntity.SLOT_FILL, SolidCannerBlockEntity.SLOT_FILL + 1, false)
                    && !this.insertItem(original, SolidCannerBlockEntity.SLOT_DISCHARGE, SolidCannerBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, SolidCannerBlockEntity.SLOT_UPGRADE_0, SolidCannerBlockEntity.SLOT_UPGRADE_0 + SolidCannerBlockEntity.UPGRADE_SLOTS, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY); else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return net.minecraft.item.ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }
}
