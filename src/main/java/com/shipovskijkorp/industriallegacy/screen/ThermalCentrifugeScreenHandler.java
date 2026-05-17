package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.ThermalCentrifugeBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ThermalCentrifugeScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate props;

    public ThermalCentrifugeScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, getBlockEntityInventory(playerInv, buf));
    }

    public ThermalCentrifugeScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.THERMAL_CENTRIFUGE, syncId);
        if (inv == null) {
            this.inventory = new SimpleInventory(ThermalCentrifugeBlockEntity.INV_SIZE);
            this.props = emptyProps();
        } else {
            this.inventory = inv;
            this.props = inv instanceof ThermalCentrifugeBlockEntity be ? be.getGuiProps() : emptyProps();
        }

        this.addSlot(new Slot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_INPUT, 11, 21));
        this.addSlot(new FilteredSlot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_DISCHARGE, 11, 60));
        this.addSlot(new Slot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_OUTPUT_0, 124, 18) { @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; } });
        this.addSlot(new Slot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_OUTPUT_1, 124, 36) { @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; } });
        this.addSlot(new Slot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_OUTPUT_2, 124, 54) { @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; } });
        for (int i = 0; i < ThermalCentrifugeBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(this.inventory, ThermalCentrifugeBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
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
            @Override public int size() { return 6; }
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) { }
        };
    }

    private static Inventory getBlockEntityInventory(PlayerInventory playerInv, PacketByteBuf buf) {
        if (playerInv == null || playerInv.player == null || buf == null) return null;
        var be = playerInv.player.getWorld().getBlockEntity(buf.readBlockPos());
        return be instanceof ThermalCentrifugeBlockEntity tc ? tc : null;
    }

    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }
    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return Math.max(1, props.get(3)); }
    public int getHeat() { return props.get(4); }
    public int getWorkHeat() { return Math.max(1, props.get(5)); }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = ThermalCentrifugeBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, ThermalCentrifugeBlockEntity.SLOT_INPUT, ThermalCentrifugeBlockEntity.SLOT_INPUT + 1, false)
                    && !this.insertItem(original, ThermalCentrifugeBlockEntity.SLOT_DISCHARGE, ThermalCentrifugeBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, ThermalCentrifugeBlockEntity.SLOT_UPGRADE_0, ThermalCentrifugeBlockEntity.SLOT_UPGRADE_0 + ThermalCentrifugeBlockEntity.UPGRADE_SLOTS, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY); else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return net.minecraft.item.ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }
}
