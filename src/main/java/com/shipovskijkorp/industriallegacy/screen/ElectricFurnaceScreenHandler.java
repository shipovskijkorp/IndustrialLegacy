package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.ElectricFurnaceBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class ElectricFurnaceScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 5;
    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInv, ElectricFurnaceBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps(), be.getPos());
    }

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.ELECTRIC_FURNACE, syncId);
        checkSize(inv, ElectricFurnaceBlockEntity.INV_SIZE);
        checkDataCount(props, PROP_COUNT);
        this.inv = inv;
        this.props = props;
        this.pos = pos;

        this.addSlot(new Slot(inv, ElectricFurnaceBlockEntity.SLOT_INPUT, 56, 17));
        this.addSlot(new Slot(inv, ElectricFurnaceBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean canInsert(net.minecraft.item.ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new FilteredSlot(inv, ElectricFurnaceBlockEntity.SLOT_DISCHARGE, 56, 53));
        for (int i = 0; i < ElectricFurnaceBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(inv, ElectricFurnaceBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        int startX = 7;
        int startY = 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
        }

        this.addProperties(props);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inv.canPlayerUse(player);
    }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = ElectricFurnaceBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        } else {
            if (!this.insertItem(original, ElectricFurnaceBlockEntity.SLOT_INPUT, ElectricFurnaceBlockEntity.SLOT_INPUT + 1, false)
                    && !this.insertItem(original, ElectricFurnaceBlockEntity.SLOT_DISCHARGE, ElectricFurnaceBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, ElectricFurnaceBlockEntity.SLOT_UPGRADE_0, ElectricFurnaceBlockEntity.SLOT_UPGRADE_0 + ElectricFurnaceBlockEntity.UPGRADE_SLOTS, false)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY);
        else slot.markDirty();

        if (original.getCount() == newStack.getCount()) return net.minecraft.item.ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }

    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getProgress() { return props.get(2); }
    public int getMaxProgress() { return Math.max(1, props.get(3)); }
    public int getStoredXp() { return props.get(4); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity be) return be;
        return new SimpleInventory(ElectricFurnaceBlockEntity.INV_SIZE);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity be) return be.getGuiProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
