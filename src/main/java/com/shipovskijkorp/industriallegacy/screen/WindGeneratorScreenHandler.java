package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.WindGeneratorBlockEntity;
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

public class WindGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 1;
    public static final int PROP_COUNT = 5;
    private final Inventory inv;
    private final PropertyDelegate props;
    public final BlockPos pos;

    public WindGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private WindGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public WindGeneratorScreenHandler(int syncId, PlayerInventory playerInv, WindGeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public WindGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.WIND_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.inv = inv;
        this.props = props;
        this.pos = pos;

        addSlot(new FilteredSlot(inv, WindGeneratorBlockEntity.SLOT_CHARGE, 81, 27));
        int startX = 8;
        int startY = 84;
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

    @Override public boolean canUse(PlayerEntity player) { return inv.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;
        ItemStack original = slot.getStack();
        newStack = original.copy();
        if (index < SLOT_COUNT) {
            if (!insertItem(original, SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(original, 0, 1, false)) {
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
    public float getProduction() { return props.get(2) / 1000.0F; }
    public int getObstructions() { return props.get(3); }
    public float getOverheatRatio() { return props.get(4) / 1000.0F; }
    public boolean hasWind() { return getProduction() > 0.0F; }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WindGeneratorBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WindGeneratorBlockEntity be) return be.getGuiProperties();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
