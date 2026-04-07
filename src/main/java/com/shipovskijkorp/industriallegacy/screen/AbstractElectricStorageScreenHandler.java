package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Shared menu layout and quick-move logic for IL electric storage blocks.
 */
public abstract class AbstractElectricStorageScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 2;
    public static final int PROP_COUNT = 4;
    private static final int ARMOR_SLOT_COUNT = 4;
    private static final int CONTAINER_SLOT_COUNT = SLOT_COUNT + ARMOR_SLOT_COUNT;

    public final BlockPos pos;
    private final Inventory inventory;
    private final PropertyDelegate properties;

    protected AbstractElectricStorageScreenHandler(
            ScreenHandlerType<?> type,
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties,
            BlockPos pos
    ) {
        super(type, syncId);
        checkSize(inventory, SLOT_COUNT);
        checkDataCount(properties, PROP_COUNT);

        this.pos = pos;
        this.inventory = inventory;
        this.properties = properties;

        addElectricStorageSlots(inventory);
        addArmorSlots(playerInventory);
        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
    }

    private void addElectricStorageSlots(Inventory inventory) {
        this.addSlot(new NonStackingElectricSlot(inventory, 0, 56, 17));
        this.addSlot(new NonStackingElectricSlot(inventory, 1, 56, 53));
    }

    private void addArmorSlots(PlayerInventory playerInventory) {
        this.addSlot(new EquipmentArmorSlot(playerInventory, 36, 8, 84, EquipmentSlot.FEET));
        this.addSlot(new EquipmentArmorSlot(playerInventory, 37, 26, 84, EquipmentSlot.LEGS));
        this.addSlot(new EquipmentArmorSlot(playerInventory, 38, 44, 84, EquipmentSlot.CHEST));
        this.addSlot(new EquipmentArmorSlot(playerInventory, 39, 62, 84, EquipmentSlot.HEAD));
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        int startX = 8;
        int startY = 114;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, hotbarY));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) {
            return moved;
        }

        ItemStack original = slot.getStack();
        moved = original.copy();

        if (index < CONTAINER_SLOT_COUNT) {
            if (!this.insertItem(original, CONTAINER_SLOT_COUNT, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertItem(original, 0, SLOT_COUNT, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return moved;
    }

    public int getEuStored() {
        return properties.get(0);
    }

    public int getEuCap() {
        return properties.get(1);
    }

    public int getOutputEUt() {
        return properties.get(2);
    }

    public int getRedstoneMode() {
        return properties.get(3);
    }

    protected static final class NonStackingElectricSlot extends Slot {
        protected NonStackingElectricSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ElectricItemManager.isElectric(stack);
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

    protected static final class EquipmentArmorSlot extends Slot {
        private final EquipmentSlot expectedSlot;

        protected EquipmentArmorSlot(PlayerInventory inventory, int index, int x, int y, EquipmentSlot expectedSlot) {
            super(inventory, index, x, y);
            this.expectedSlot = expectedSlot;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!(stack.getItem() instanceof ArmorItem armor)) {
                return false;
            }
            return armor.getSlotType() == expectedSlot;
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
