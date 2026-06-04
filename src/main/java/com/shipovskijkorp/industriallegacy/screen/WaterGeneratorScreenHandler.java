package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.WaterGeneratorBlockEntity;
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

public class WaterGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 2;
    public static final int PROP_COUNT = 7;
    private final Inventory inv;
    private final PropertyDelegate props;
    public final BlockPos pos;

    public WaterGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private WaterGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public WaterGeneratorScreenHandler(int syncId, PlayerInventory playerInv, WaterGeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public WaterGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.WATER_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.inv = inv;
        this.props = props;
        this.pos = pos;

        addSlot(new FilteredSlot(inv, WaterGeneratorBlockEntity.SLOT_CHARGE, 81, 18));
        addSlot(new Slot(inv, WaterGeneratorBlockEntity.SLOT_FUEL, 81, 54) {
            @Override public boolean canInsert(ItemStack stack) { return WaterGeneratorBlockEntity.isValidFuelStack(stack); }
        });
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
        } else if (WaterGeneratorBlockEntity.isValidFuelStack(original)) {
            if (!insertItem(original, WaterGeneratorBlockEntity.SLOT_FUEL, WaterGeneratorBlockEntity.SLOT_FUEL + 1, false)) return ItemStack.EMPTY;
        } else if (!insertItem(original, WaterGeneratorBlockEntity.SLOT_CHARGE, WaterGeneratorBlockEntity.SLOT_CHARGE + 1, false)) {
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
    public int getFuel() { return props.get(2); }
    public int getMaxWater() { return props.get(3); }
    public int getWater() { return props.get(4); }
    public int getMicroStorage() { return props.get(5); }
    public float getProduction() { return props.get(6) / 1000.0F; }
    public float getWaterRatio() { return getMaxWater() <= 0 ? 0.0F : getFuel() / (float) getMaxWater(); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WaterGeneratorBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WaterGeneratorBlockEntity be) return be.getGuiProperties();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
