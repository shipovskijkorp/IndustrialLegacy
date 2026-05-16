package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.PumpBlockEntity;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
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

public class PumpScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = PumpBlockEntity.INV_SIZE;
    public static final int PROP_COUNT = 7;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public PumpScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private PumpScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public PumpScreenHandler(int syncId, PlayerInventory playerInv, PumpBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps(), be.getPos());
    }

    public PumpScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.PUMP, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);

        this.pos = pos;
        this.inv = inv;
        this.props = props;

        this.addSlot(new Slot(inv, PumpBlockEntity.SLOT_INPUT, 98, 16) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return PumpBlockEntity.canFillFromPumpTank(stack); }
        });
        this.addSlot(new Slot(inv, PumpBlockEntity.SLOT_OUTPUT, 131, 33) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, PumpBlockEntity.SLOT_DISCHARGE, 7, 43));
        for (int i = 0; i < PumpBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(inv, PumpBlockEntity.SLOT_UPGRADE_0 + i, 151, 7 + i * 18));
        }

        int invX = 7;
        int invY = 83;
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
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        if (index < SLOT_COUNT) {
            if (!this.insertItem(original, SLOT_COUNT, this.slots.size(), true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, PumpBlockEntity.SLOT_INPUT, PumpBlockEntity.SLOT_INPUT + 1, false)
                    && !this.insertItem(original, PumpBlockEntity.SLOT_DISCHARGE, PumpBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, PumpBlockEntity.SLOT_UPGRADE_0, PumpBlockEntity.SLOT_UPGRADE_0 + PumpBlockEntity.UPGRADE_SLOTS, false)) {
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
    public int getTankAmount() { return props.get(4); }
    public int getTankCapacity() { return props.get(5); }
    public UniversalFluidCellItem.CellFluid getTankFluid() {
        UniversalFluidCellItem.CellFluid[] values = UniversalFluidCellItem.CellFluid.values();
        return values[Math.max(0, Math.min(values.length - 1, props.get(6)))];
    }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof PumpBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof PumpBlockEntity be) return be.getGuiProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
