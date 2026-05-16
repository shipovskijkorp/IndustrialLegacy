package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.FluidBottlerBlockEntity;
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

public class FluidBottlerScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 7;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public FluidBottlerScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private FluidBottlerScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public FluidBottlerScreenHandler(int syncId, PlayerInventory playerInv, FluidBottlerBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProps(), be.getPos());
    }

    public FluidBottlerScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.FLUID_BOTTLER, syncId);
        checkSize(inv, FluidBottlerBlockEntity.INV_SIZE);
        checkDataCount(props, PROP_COUNT);

        this.pos = pos;
        this.inv = inv;
        this.props = props;

        this.addSlot(new Slot(inv, FluidBottlerBlockEntity.SLOT_DRAIN, 44, 35) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return FluidBottlerBlockEntity.canDrainContainer(stack); }
        });
        this.addSlot(new Slot(inv, FluidBottlerBlockEntity.SLOT_FILL, 44, 72) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return FluidBottlerBlockEntity.canFillContainer(stack); }
        });
        this.addSlot(new Slot(inv, FluidBottlerBlockEntity.SLOT_OUTPUT, 117, 53) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, FluidBottlerBlockEntity.SLOT_DISCHARGE, 8, 53));
        for (int i = 0; i < FluidBottlerBlockEntity.UPGRADE_SLOTS; i++) {
            this.addSlot(new Slot(inv, FluidBottlerBlockEntity.SLOT_UPGRADE_0 + i, 152, 26 + i * 18));
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
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        final int machineSlots = FluidBottlerBlockEntity.INV_SIZE;
        final int playerStart = machineSlots;
        final int playerEnd = this.slots.size();

        if (index < machineSlots) {
            if (!this.insertItem(original, playerStart, playerEnd, true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, FluidBottlerBlockEntity.SLOT_DRAIN, FluidBottlerBlockEntity.SLOT_DRAIN + 1, false)
                    && !this.insertItem(original, FluidBottlerBlockEntity.SLOT_FILL, FluidBottlerBlockEntity.SLOT_FILL + 1, false)
                    && !this.insertItem(original, FluidBottlerBlockEntity.SLOT_DISCHARGE, FluidBottlerBlockEntity.SLOT_DISCHARGE + 1, false)
                    && !this.insertItem(original, FluidBottlerBlockEntity.SLOT_UPGRADE_0, FluidBottlerBlockEntity.SLOT_UPGRADE_0 + FluidBottlerBlockEntity.UPGRADE_SLOTS, false)) {
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
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof FluidBottlerBlockEntity be) return be;
        return new SimpleInventory(FluidBottlerBlockEntity.INV_SIZE);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof FluidBottlerBlockEntity be) return be.getGuiProps();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
