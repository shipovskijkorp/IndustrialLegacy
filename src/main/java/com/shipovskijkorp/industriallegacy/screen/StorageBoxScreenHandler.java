package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.StorageBoxBlock;
import com.shipovskijkorp.industriallegacy.block.entity.StorageBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public final class StorageBoxScreenHandler extends ScreenHandler {
    public final BlockPos pos;
    private final Inventory inventory;
    private final StorageBoxBlock.Type type;

    public StorageBoxScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos());
    }

    private StorageBoxScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, getClientInventory(playerInventory, pos), getClientType(playerInventory, pos), pos);
    }

    public StorageBoxScreenHandler(int syncId, PlayerInventory playerInventory, StorageBoxBlockEntity be) {
        this(syncId, playerInventory, be, be.getStorageBoxType(), be.getPos());
    }

    private StorageBoxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, StorageBoxBlock.Type type, BlockPos pos) {
        super(ModScreenHandlers.STORAGE_BOX, syncId);
        checkSize(inventory, type.slots());

        this.pos = pos;
        this.inventory = inventory;
        this.type = type;

        for (int row = 0; row < type.rows(); row++) {
            for (int col = 0; col < type.columns(); col++) {
                int index = col + row * type.columns();
                this.addSlot(new Slot(inventory, index, type.inventoryX() + 1 + col * 18, type.inventoryY() + 1 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, type.playerInventoryX() + 1 + col * 18, type.playerInventoryY() + 1 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, type.playerInventoryX() + 1 + col * 18, type.playerInventoryY() + 59));
        }
    }

    public StorageBoxBlock.Type getStorageBoxType() {
        return type;
    }

    public int getBoxSlots() {
        return type.slots();
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

        int boxSlots = type.slots();
        if (index < boxSlots) {
            if (!this.insertItem(original, boxSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.insertItem(original, 0, boxSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (original.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return moved;
    }

    private static Inventory getClientInventory(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof StorageBoxBlockEntity box) {
            return box;
        }
        return new SimpleInventory(getClientType(playerInventory, pos).slots());
    }

    private static StorageBoxBlock.Type getClientType(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof StorageBoxBlockEntity box) {
            return box.getStorageBoxType();
        }
        return StorageBoxBlock.getType(playerInventory.player.getWorld().getBlockState(pos));
    }
}
