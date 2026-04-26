package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.RTGeneratorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
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
import net.minecraft.util.math.BlockPos;

public class RTGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 6;
    public static final int PROP_COUNT = 3;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public RTGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private RTGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public RTGeneratorScreenHandler(int syncId, PlayerInventory playerInv, RTGeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public RTGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.RT_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.pos = pos;
        this.inv = inv;
        this.props = props;

        int gridX = 31;
        int gridY = 26;
        int index = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new RTGPelletSlot(inv, index++, gridX + col * 18, gridY + row * 18));
            }
        }

        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
        }

        addProperties(props);
    }

    @Override public boolean canUse(PlayerEntity player) { return inv.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack original = slot.getStack();
        newStack = original.copy();
        if (index < SLOT_COUNT) {
            if (!this.insertItem(original, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (original.isOf(ModItems.RTG_PELLET)) {
            if (!this.insertItem(original, 0, SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }

    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getPelletCount() { return props.get(2); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof RTGeneratorBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof RTGeneratorBlockEntity be) return be.getGuiProperties();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }

    private static final class RTGPelletSlot extends Slot {
        private RTGPelletSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.getStack().isEmpty() && stack.isOf(ModItems.RTG_PELLET);
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
