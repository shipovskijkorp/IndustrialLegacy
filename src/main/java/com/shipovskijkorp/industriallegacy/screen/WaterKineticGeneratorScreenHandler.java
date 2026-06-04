package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.WaterKineticGeneratorBlockEntity;
import com.shipovskijkorp.industriallegacy.item.WindRotorItem;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
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

public class WaterKineticGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 1;
    public static final int PROP_COUNT = 7;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public WaterKineticGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private WaterKineticGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public WaterKineticGeneratorScreenHandler(int syncId, PlayerInventory playerInv, WaterKineticGeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public WaterKineticGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.WATER_KINETIC_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.pos = pos;
        this.inv = inv;
        this.props = props;

        this.addSlot(new Slot(inv, WaterKineticGeneratorBlockEntity.SLOT_ROTOR, 80, 26) {
            @Override public boolean canInsert(ItemStack stack) { return isWaterRotor(stack); }
        });

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
        addProperties(props);
    }

    @Override public boolean canUse(PlayerEntity player) { return inv.canPlayerUse(player); }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return ItemStack.EMPTY;
        ItemStack original = slot.getStack();
        newStack = original.copy();
        if (index < SLOT_COUNT) {
            if (!this.insertItem(original, SLOT_COUNT, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (isWaterRotor(original)) {
            if (!this.insertItem(original, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }

    private static boolean isWaterRotor(ItemStack stack) {
        return stack.getItem() instanceof WindRotorItem && stack.getItem() != ModItems.ROTOR_WOOD;
    }

    public int getKuOutput() { return props.get(0); }
    public int getBiomeStateOrdinal() { return props.get(1); }
    public int getRotorHealth() { return props.get(2); }
    public boolean hasRotor() { return props.get(3) != 0; }
    public boolean rotorHasSpace() { return props.get(4) != 0; }
    public int getWaterFlow() { return props.get(5); }
    public int getObstructions() { return props.get(6); }
    public boolean isInvalidBiome() { return getBiomeStateOrdinal() == WaterKineticGeneratorBlockEntity.BiomeState.INVALID.ordinal(); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WaterKineticGeneratorBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof WaterKineticGeneratorBlockEntity be) return be.getGuiProperties();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
