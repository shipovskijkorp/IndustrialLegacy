package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.item.tool.ContainmentBoxInventory;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public final class ContainmentBoxScreenHandler extends ScreenHandler {
    private static final int BOX_SLOTS = ContainmentBoxInventory.SIZE;

    private final ContainmentBoxInventory inventory;
    private final Hand hand;
    private final int lockedPlayerSlot;

    public ContainmentBoxScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readEnumConstant(Hand.class));
    }

    public ContainmentBoxScreenHandler(int syncId, PlayerInventory playerInventory, Hand hand) {
        super(ModScreenHandlers.CONTAINMENT_BOX, syncId);
        this.hand = hand;
        this.inventory = new ContainmentBoxInventory(playerInventory.player, hand);
        this.lockedPlayerSlot = hand == Hand.MAIN_HAND ? playerInventory.selectedSlot : -1;

        for (int i = 0; i < 4; ++i) {
            this.addSlot(new RadioactiveSlot(this.inventory, i, 53 + i * 18, 19));
        }
        for (int i = 4; i < 8; ++i) {
            this.addSlot(new RadioactiveSlot(this.inventory, i, 53 + (i - 4) * 18, 37));
        }
        for (int i = 8; i < 12; ++i) {
            this.addSlot(new RadioactiveSlot(this.inventory, i, 53 + (i - 8) * 18, 55));
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

    public Hand getHand() {
        return hand;
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

        if (index < BOX_SLOTS) {
            if (!this.insertItem(original, BOX_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!ContainmentBoxInventory.isRadioactiveAllowed(original)) {
                return ItemStack.EMPTY;
            }
            if (!this.insertItem(original, 0, BOX_SLOTS, false)) {
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

    private static final class RadioactiveSlot extends Slot {
        private RadioactiveSlot(ContainmentBoxInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ContainmentBoxInventory.isRadioactiveAllowed(stack);
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
