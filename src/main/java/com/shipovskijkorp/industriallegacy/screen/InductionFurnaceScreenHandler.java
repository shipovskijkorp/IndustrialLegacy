package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.InductionFurnaceBlockEntity;
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

public class InductionFurnaceScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 6;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public InductionFurnaceScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private InductionFurnaceScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public InductionFurnaceScreenHandler(int syncId, PlayerInventory playerInv, InductionFurnaceBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps(), be.getPos());
    }

    public InductionFurnaceScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.INDUCTION_FURNACE, syncId);
        checkSize(inv, InductionFurnaceBlockEntity.INV_SIZE);
        checkDataCount(props, PROP_COUNT);
        this.inv = inv;
        this.props = props;
        this.pos = pos;

        this.addSlot(new Slot(inv, InductionFurnaceBlockEntity.SLOT_INPUT_A, 43, 17));
        this.addSlot(new Slot(inv, InductionFurnaceBlockEntity.SLOT_INPUT_B, 59, 17));
        this.addSlot(new Slot(inv, InductionFurnaceBlockEntity.SLOT_OUTPUT_A, 113, 35) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, InductionFurnaceBlockEntity.SLOT_OUTPUT_B, 129, 35) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new FilteredSlot(inv, InductionFurnaceBlockEntity.SLOT_DISCHARGE, 50, 52));
        for (int i = 0; i < InductionFurnaceBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(inv, InductionFurnaceBlockEntity.SLOT_UPGRADE_0 + i, 151, 25 + i * 18));
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

        final int machineSlots = InductionFurnaceBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) {
                return net.minecraft.item.ItemStack.EMPTY;
            }
        } else {
            if (!this.insertItem(original, InductionFurnaceBlockEntity.SLOT_INPUT_A, InductionFurnaceBlockEntity.SLOT_INPUT_B + 1, false)
                    && !this.insertItem(original, InductionFurnaceBlockEntity.SLOT_DISCHARGE, InductionFurnaceBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, InductionFurnaceBlockEntity.SLOT_UPGRADE_0,
                    InductionFurnaceBlockEntity.SLOT_UPGRADE_0 + InductionFurnaceBlockEntity.UPGRADE_SLOTS, false)) {
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
    public int getHeat() { return props.get(2); }
    public int getMaxHeat() { return Math.max(1, props.get(3)); }
    public int getProgress() { return props.get(4); }
    public int getMaxProgress() { return Math.max(1, props.get(5)); }
    public int getHeatPercent() { return getHeat() * 100 / getMaxHeat(); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof InductionFurnaceBlockEntity be) return be;
        return new SimpleInventory(InductionFurnaceBlockEntity.INV_SIZE);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof InductionFurnaceBlockEntity be) return be.getGuiProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
