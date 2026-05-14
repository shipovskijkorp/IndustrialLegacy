package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.ExtractorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Extractor screen handler (IC2-style layout).
 *
 * Slots:
 *  0 input, 1 output, 2 discharge, 3..6 upgrades
 */
public class ExtractorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate props;

    public ExtractorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, getBlockEntityInventory(playerInv, buf));
    }

    public ExtractorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.EXTRACTOR, syncId);

        if (inv == null) {
            this.inventory = new SimpleInventory(ExtractorBlockEntity.INV_SIZE);
            this.props = new PropertyDelegate() {
                @Override public int size() { return 4; }
                @Override public int get(int index) { return 0; }
                @Override public void set(int index, int value) { }
            };
        } else {
            this.inventory = inv;
            if (inv instanceof ExtractorBlockEntity be) {
                this.props = be.getGuiProps();
            } else {
                this.props = new PropertyDelegate() {
                    @Override public int size() { return 4; }
                    @Override public int get(int index) { return 0; }
                    @Override public void set(int index, int value) { }
                };
            }
        }

        // Machine inventory
        // NOTE: HandledScreen draws the slot background at (slot.x - 1, slot.y - 1),
        // so these coordinates are (frame + 1) to match IC2-style frames.
        this.addSlot(new Slot(this.inventory, ExtractorBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new Slot(this.inventory, ExtractorBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(this.inventory, ExtractorBlockEntity.SLOT_DISCHARGE, 56, 53));

        // upgrades (right side)
        for (int i = 0; i < ExtractorBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(this.inventory, ExtractorBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        // Player inventory frames (IC2-style position)
        final int invX = 7;
        final int invY = 83;
        // main inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
        }

        this.addProperties(this.props);
    }

    private static Inventory getBlockEntityInventory(PlayerInventory playerInv, PacketByteBuf buf) {
        if (playerInv == null || playerInv.player == null) return null;
        if (buf == null) return null;
        var pos = buf.readBlockPos();
        var be = playerInv.player.getWorld().getBlockEntity(pos);
        if (be instanceof ExtractorBlockEntity cbe) return cbe;
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    // --- GUI data ---
    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return Math.max(1, props.get(3)); }

    // --- Shift-click behavior (simple + safe) ---
    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = ExtractorBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            // from machine -> player
            if (!this.insertItem(original, playerStart, playerEnd, true)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        } else {
            // from player -> machine
            if (!this.insertItem(original, ExtractorBlockEntity.SLOT_INPUT, ExtractorBlockEntity.SLOT_INPUT + 1, false)
                    && !this.insertItem(original, ExtractorBlockEntity.SLOT_DISCHARGE, ExtractorBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, ExtractorBlockEntity.SLOT_UPGRADE_0, ExtractorBlockEntity.SLOT_UPGRADE_0 + ExtractorBlockEntity.UPGRADE_SLOTS, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(net.minecraft.item.ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (original.getCount() == newStack.getCount()) {
            return net.minecraft.item.ItemStack.EMPTY;
        }

        slot.onTakeItem(player, original);
        return newStack;
    }
}
