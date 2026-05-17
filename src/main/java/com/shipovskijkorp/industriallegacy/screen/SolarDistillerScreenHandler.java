package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.SolarDistillerBlockEntity;
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

public class SolarDistillerScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = SolarDistillerBlockEntity.INV_SIZE;
    public static final int PROP_COUNT = 7;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public SolarDistillerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private SolarDistillerScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public SolarDistillerScreenHandler(int syncId, PlayerInventory playerInv, SolarDistillerBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps(), be.getPos());
    }

    public SolarDistillerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.SOLAR_DISTILLER, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);
        this.pos = pos;
        this.inv = inv;
        this.props = props;

        this.addSlot(new Slot(inv, SolarDistillerBlockEntity.SLOT_WATER_INPUT, 17, 27) {
            @Override public boolean canInsert(ItemStack stack) { return SolarDistillerBlockEntity.canInsertWaterContainer(stack); }
        });
        this.addSlot(new Slot(inv, SolarDistillerBlockEntity.SLOT_DISTILLED_INPUT, 136, 64) {
            @Override public boolean canInsert(ItemStack stack) { return SolarDistillerBlockEntity.canInsertDistilledContainer(stack); }
        });
        this.addSlot(new Slot(inv, SolarDistillerBlockEntity.SLOT_WATER_OUTPUT, 17, 45) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, SolarDistillerBlockEntity.SLOT_DISTILLED_OUTPUT, 136, 82) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int i = 0; i < SolarDistillerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new FilteredSlot(inv, SolarDistillerBlockEntity.SLOT_UPGRADE_0 + i, 152, 8 + i * 18));
        }

        int invX = 8;
        int invY = 102;
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
        } else {
            if (!this.insertItem(original, SolarDistillerBlockEntity.SLOT_WATER_INPUT, SolarDistillerBlockEntity.SLOT_WATER_INPUT + 1, false)
                    && !this.insertItem(original, SolarDistillerBlockEntity.SLOT_DISTILLED_INPUT, SolarDistillerBlockEntity.SLOT_DISTILLED_INPUT + 1, false)
                    && !this.insertItem(original, SolarDistillerBlockEntity.SLOT_UPGRADE_0, SolarDistillerBlockEntity.SLOT_UPGRADE_0 + SolarDistillerBlockEntity.UPGRADE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (original.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        if (original.getCount() == newStack.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, original);
        return newStack;
    }

    public int getInputWater() { return props.get(0); }
    public int getInputCapacity() { return props.get(1); }
    public int getDistilledWater() { return props.get(2); }
    public int getOutputCapacity() { return props.get(3); }
    public float getSkyLight() { return props.get(4) / 1000.0f; }
    public int getTickrate() { return Math.max(1, props.get(5)); }
    public int getUpdateTicker() { return Math.max(0, props.get(6)); }
    public boolean canWork() { return getInputWater() > 0 && getDistilledWater() < getOutputCapacity() && getSkyLight() > 0.5f; }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof SolarDistillerBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof SolarDistillerBlockEntity be) return be.getGuiProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
