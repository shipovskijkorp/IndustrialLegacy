package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.CannerBlockEntity;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class CannerScreenHandler extends ScreenHandler {
    public static final int BUTTON_MODE_BASE = 0;
    public static final int BUTTON_SWAP_TANKS = 4;

    private final Inventory inventory;
    private final PropertyDelegate props;
    private final CannerBlockEntity canner;

    public CannerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, getBlockEntityInventory(playerInv, buf));
    }

    public CannerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv) {
        super(ModScreenHandlers.CANNER, syncId);
        this.canner = inv instanceof CannerBlockEntity be ? be : null;
        if (inv == null) {
            this.inventory = new SimpleInventory(CannerBlockEntity.INV_SIZE);
            this.props = emptyProps();
        } else {
            this.inventory = inv;
            this.props = inv instanceof CannerBlockEntity be ? be.getGuiProps() : emptyProps();
        }

        this.addSlot(new Slot(this.inventory, CannerBlockEntity.SLOT_CONTAINER, 42, 18));
        this.addSlot(new Slot(this.inventory, CannerBlockEntity.SLOT_FILL, 81, 45));
        this.addSlot(new Slot(this.inventory, CannerBlockEntity.SLOT_OUTPUT, 120, 18) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(this.inventory, CannerBlockEntity.SLOT_DISCHARGE, 9, 81));
        for (int i = 0; i < CannerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(this.inventory, CannerBlockEntity.SLOT_UPGRADE_0 + i, 153, 27 + i * 18));
        }

        int invX = 9;
        int invY = 102;
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
            @Override public int size() { return 9; }
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) { }
        };
    }

    private static Inventory getBlockEntityInventory(PlayerInventory playerInv, PacketByteBuf buf) {
        if (playerInv == null || playerInv.player == null || buf == null) return null;
        var be = playerInv.player.getWorld().getBlockEntity(buf.readBlockPos());
        return be instanceof CannerBlockEntity canner ? canner : null;
    }

    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }
    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return Math.max(1, props.get(3)); }
    public CannerBlockEntity.Mode getMode() { return CannerBlockEntity.Mode.VALUES[Math.max(0, Math.min(CannerBlockEntity.Mode.VALUES.length - 1, props.get(4)))]; }
    public int getInputTankAmount() { return props.get(5); }
    public int getOutputTankAmount() { return props.get(6); }
    public UniversalFluidCellItem.CellFluid getInputTankFluid() { return UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, props.get(7)))]; }
    public UniversalFluidCellItem.CellFluid getOutputTankFluid() { return UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, props.get(8)))]; }
    public int getTankCapacity() { return 8000; }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (canner == null) return false;
        if (id >= BUTTON_MODE_BASE && id < BUTTON_MODE_BASE + CannerBlockEntity.Mode.VALUES.length) {
            canner.setMode(CannerBlockEntity.Mode.VALUES[id]);
            return true;
        }
        if (id == BUTTON_SWAP_TANKS) {
            canner.swapTanks();
            return true;
        }
        return false;
    }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = CannerBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, CannerBlockEntity.SLOT_CONTAINER, CannerBlockEntity.SLOT_CONTAINER + 1, false)
                    && !this.insertItem(original, CannerBlockEntity.SLOT_FILL, CannerBlockEntity.SLOT_FILL + 1, false)
                    && !this.insertItem(original, CannerBlockEntity.SLOT_DISCHARGE, CannerBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, CannerBlockEntity.SLOT_UPGRADE_0, CannerBlockEntity.SLOT_UPGRADE_0 + CannerBlockEntity.UPGRADE_SLOTS, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY); else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return net.minecraft.item.ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }
}
