package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity;
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

public class MetalFormerScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 5;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public MetalFormerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private MetalFormerScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public MetalFormerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.METAL_FORMER, syncId);
        this.inv = inv;
        this.props = props;
        this.pos = pos;
        checkSize(inv, MetalFormerBlockEntity.INV_SIZE);
        checkDataCount(props, PROP_COUNT);

        this.addSlot(new Slot(inv, MetalFormerBlockEntity.SLOT_INPUT, 17, 17));
        this.addSlot(new Slot(inv, MetalFormerBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new FilteredSlot(inv, MetalFormerBlockEntity.SLOT_DISCHARGE, 17, 53));

        for (int i = 0; i < MetalFormerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(inv, MetalFormerBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        int invX = 8;
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, invX + col * 18, invY + 58));
        }

        addProperties(props);
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

        final int machineSlots = MetalFormerBlockEntity.INV_SIZE;
        final int invStart = machineSlots;
        final int invEnd = invStart + 27;
        final int hotbarEnd = invEnd + 9;

        if (index < machineSlots) {
            if (!this.insertItem(original, invStart, hotbarEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, MetalFormerBlockEntity.SLOT_INPUT, MetalFormerBlockEntity.SLOT_INPUT + 1, false)) {
                if (!this.insertItem(original, MetalFormerBlockEntity.SLOT_DISCHARGE, MetalFormerBlockEntity.SLOT_DISCHARGE + 1, false)) {
                    if (!this.insertItem(original, MetalFormerBlockEntity.SLOT_UPGRADE_0,
                            MetalFormerBlockEntity.SLOT_UPGRADE_0 + MetalFormerBlockEntity.UPGRADE_SLOTS, false)) {
                        if (index < invEnd) {
                            if (!this.insertItem(original, invEnd, hotbarEnd, false)) return net.minecraft.item.ItemStack.EMPTY;
                        } else if (index < hotbarEnd) {
                            if (!this.insertItem(original, invStart, invEnd, false)) return net.minecraft.item.ItemStack.EMPTY;
                        } else {
                            return net.minecraft.item.ItemStack.EMPTY;
                        }
                    }
                }
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
    public int getModeOrdinal() { return props.get(4); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof MetalFormerBlockEntity be) {
            return be;
        }
        return new SimpleInventory(MetalFormerBlockEntity.INV_SIZE);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof MetalFormerBlockEntity be) {
            return be.getGuiProps();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
