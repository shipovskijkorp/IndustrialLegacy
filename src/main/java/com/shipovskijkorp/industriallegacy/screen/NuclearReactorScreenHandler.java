package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.NuclearReactorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

public class NuclearReactorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate props;

    public NuclearReactorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, getBlockEntityInventory(playerInv, buf));
    }

    public NuclearReactorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.NUCLEAR_REACTOR, syncId);
        if (inv == null) {
            this.inventory = new SimpleInventory(NuclearReactorBlockEntity.INV_SIZE);
            this.props = emptyProps();
        } else {
            this.inventory = inv;
            this.props = inv instanceof NuclearReactorBlockEntity be ? be.getGuiProps() : emptyProps();
        }

        final int startX = 26;
        final int startY = 25;
        for (int y = 0; y < NuclearReactorBlockEntity.ROWS; y++) {
            for (int x = 0; x < NuclearReactorBlockEntity.COLUMNS; x++) {
                final int slotIndex = y * NuclearReactorBlockEntity.COLUMNS + x;
                this.addSlot(new Slot(this.inventory, slotIndex, startX + x * 18, startY + y * 18) {
                    @Override
                    public boolean canInsert(net.minecraft.item.ItemStack stack) {
                        return isSlotEnabled(slotIndex) && inventory.isValid(slotIndex, stack);
                    }

                    @Override
                    public boolean isEnabled() {
                        return isSlotEnabled(slotIndex);
                    }
                });
            }
        }

        final int invX = 26;
        final int invY = 161;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
        }

        addProperties(this.props);
    }

    private boolean isSlotEnabled(int slot) {
        int x = slot % NuclearReactorBlockEntity.COLUMNS;
        return x < getReactorSize();
    }

    private static PropertyDelegate emptyProps() {
        return new PropertyDelegate() {
            @Override public int size() { return 5; }
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) { }
        };
    }

    private static Inventory getBlockEntityInventory(PlayerInventory playerInv, PacketByteBuf buf) {
        if (playerInv == null || playerInv.player == null || buf == null) return null;
        var be = playerInv.player.getWorld().getBlockEntity(buf.readBlockPos());
        return be instanceof NuclearReactorBlockEntity reactor ? reactor : null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    public int getHeat() { return props.get(0); }
    public int getMaxHeat() { return Math.max(1, props.get(1)); }
    public int getEmitHeat() { return props.get(2); }
    public int getReactorSize() { return Math.max(3, props.get(3)); }
    public float getOutput() { return props.get(4) / 10.0f; }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = NuclearReactorBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            List<Integer> enabledSlots = new ArrayList<>();
            for (int y = 0; y < NuclearReactorBlockEntity.ROWS; y++) {
                for (int x = 0; x < getReactorSize(); x++) {
                    enabledSlots.add(y * NuclearReactorBlockEntity.COLUMNS + x);
                }
            }
            boolean moved = false;
            for (int slotIndex : enabledSlots) {
                if (this.insertItem(original, slotIndex, slotIndex + 1, false)) {
                    moved = true;
                    if (original.isEmpty()) break;
                }
            }
            if (!moved) return net.minecraft.item.ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY); else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return net.minecraft.item.ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }
}
