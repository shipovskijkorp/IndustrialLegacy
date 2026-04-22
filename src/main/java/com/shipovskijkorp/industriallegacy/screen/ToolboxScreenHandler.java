package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.item.tool.ToolboxInventory;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public final class ToolboxScreenHandler extends ScreenHandler {
    private static final int TOOLBOX_SLOTS = ToolboxInventory.SIZE;

    private final ToolboxInventory inventory;
    private final int lockedPlayerSlot;

    public ToolboxScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readEnumConstant(Hand.class));
    }

    public ToolboxScreenHandler(int syncId, PlayerInventory playerInventory, Hand hand) {
        super(ModScreenHandlers.TOOL_BOX, syncId);
        this.inventory = new ToolboxInventory(playerInventory.player, hand);
        this.lockedPlayerSlot = hand == Hand.MAIN_HAND ? playerInventory.selectedSlot : -1;

        for (int col = 0; col < 9; ++col) {
            this.addSlot(new ToolboxSlot(this.inventory, col, 8 + col * 18, 41));
        }

        int startY = 84;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int playerSlot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, playerSlot, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            int playerSlot = col;
            Slot slot = (playerSlot == lockedPlayerSlot)
                    ? new LockedSlot(playerInventory, playerSlot, 8 + col * 18, startY + 58)
                    : new Slot(playerInventory, playerSlot, 8 + col * 18, startY + 58);
            this.addSlot(slot);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.markDirty();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return moved;

        ItemStack original = slot.getStack();
        moved = original.copy();

        if (index < TOOLBOX_SLOTS) {
            if (!this.insertItem(original, TOOLBOX_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!ToolboxInventory.isToolboxAllowed(original)) {
                return ItemStack.EMPTY;
            }
            if (!this.insertItem(original, 0, TOOLBOX_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        slot.onTakeItem(player, original);
        return moved;
    }

    private static final class ToolboxSlot extends Slot {
        private ToolboxSlot(ToolboxInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ToolboxInventory.isToolboxAllowed(stack);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }

    private static final class LockedSlot extends Slot {
        private LockedSlot(PlayerInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
