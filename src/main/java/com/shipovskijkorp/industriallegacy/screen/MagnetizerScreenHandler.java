package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.MagnetizedIronFenceBlock;
import com.shipovskijkorp.industriallegacy.block.entity.MagnetizerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
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

public class MagnetizerScreenHandler extends ScreenHandler {
    public static final int MACHINE_SLOT_COUNT = MagnetizerBlockEntity.INV_SIZE;
    public static final int PROP_COUNT = 4;
    private static final int PLAYER_ARMOR_FEET_SLOT = 36;

    private final Inventory inv;
    private final PropertyDelegate props;
    public final BlockPos pos;

    public MagnetizerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private MagnetizerScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public MagnetizerScreenHandler(int syncId, PlayerInventory playerInv, MagnetizerBlockEntity be) {
        this(syncId, playerInv, be, be.getMagnetizerProps(), be.getPos());
    }

    public MagnetizerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.MAGNETIZER, syncId);
        checkSize(inv, MACHINE_SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.inv = inv;
        this.props = props;
        this.pos = pos;

        addSlot(new FilteredSlot(inv, MagnetizerBlockEntity.SLOT_DISCHARGE, 8, 44));
        for (int i = 0; i < MagnetizerBlockEntity.UPGRADE_SLOTS; i++) {
            addSlot(new FilteredSlot(inv, MagnetizerBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }
        addSlot(new Slot(playerInv, PLAYER_ARMOR_FEET_SLOT, 45, 26) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return MagnetizedIronFenceBlock.hasMetalShoesStack(stack);
            }

            @Override
            public int getMaxItemCount() {
                return 1;
            }
        });

        int startX = 7;
        int startY = 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
        }
        addProperties(props);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inv.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        ItemStack stack = slot.getStack();
        copy = stack.copy();

        int machineSlotsEnd = MACHINE_SLOT_COUNT;
        int bootsSlot = machineSlotsEnd;
        int playerInvStart = bootsSlot + 1;
        int playerInvEnd = playerInvStart + 27;
        int hotbarEnd = playerInvEnd + 9;

        if (index < machineSlotsEnd || index == bootsSlot) {
            if (!insertItem(stack, playerInvStart, hotbarEnd, true)) return ItemStack.EMPTY;
        } else {
            if (MagnetizedIronFenceBlock.hasMetalShoesStack(stack)) {
                if (!insertItem(stack, bootsSlot, bootsSlot + 1, false)) return ItemStack.EMPTY;
            } else if (!insertItem(stack, MagnetizerBlockEntity.SLOT_DISCHARGE, MagnetizerBlockEntity.SLOT_DISCHARGE + 1, false)) {
                if (!insertItem(stack, MagnetizerBlockEntity.SLOT_UPGRADE_0, MagnetizerBlockEntity.SLOT_UPGRADE_0 + MagnetizerBlockEntity.UPGRADE_SLOTS, false)) {
                    if (index < playerInvEnd) {
                        if (!insertItem(stack, playerInvEnd, hotbarEnd, false)) return ItemStack.EMPTY;
                    } else if (index < hotbarEnd) {
                        if (!insertItem(stack, playerInvStart, playerInvEnd, false)) return ItemStack.EMPTY;
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, stack);
        return copy;
    }

    public int getEnergy() { return props.get(0); }
    public int getEnergyCap() { return props.get(1); }
    public int getTier() { return props.get(2); }
    public int getRange() { return props.get(3); }
    public boolean hasMetalShoes(PlayerEntity player) { return MagnetizedIronFenceBlock.hasMetalShoes(player); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof MagnetizerBlockEntity be) return be;
        return new SimpleInventory(MACHINE_SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof MagnetizerBlockEntity be) return be.getMagnetizerProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
