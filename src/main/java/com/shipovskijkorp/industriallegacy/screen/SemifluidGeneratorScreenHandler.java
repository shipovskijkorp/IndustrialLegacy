package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.SemifluidGeneratorBlockEntity;
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

public class SemifluidGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = SemifluidGeneratorBlockEntity.INV_SIZE;
    public static final int PROP_COUNT = 7;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public SemifluidGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private SemifluidGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    public SemifluidGeneratorScreenHandler(int syncId, PlayerInventory playerInv, SemifluidGeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public SemifluidGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.SEMIFLUID_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);

        this.pos = pos;
        this.inv = inv;
        this.props = props;

        this.addSlot(new Slot(inv, SemifluidGeneratorBlockEntity.SLOT_FLUID, 27, 17) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) {
                return SemifluidGeneratorBlockEntity.isAcceptedFuelCell(stack);
            }
        });
        this.addSlot(new Slot(inv, SemifluidGeneratorBlockEntity.SLOT_OUTPUT, 27, 53) {
            @Override public boolean canInsert(net.minecraft.item.ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(inv, SemifluidGeneratorBlockEntity.SLOT_CHARGE, 115, 49));

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
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        net.minecraft.item.ItemStack newStack = net.minecraft.item.ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) return net.minecraft.item.ItemStack.EMPTY;

        net.minecraft.item.ItemStack original = slot.getStack();
        newStack = original.copy();

        if (index < SLOT_COUNT) {
            if (!this.insertItem(original, SLOT_COUNT, this.slots.size(), true)) return net.minecraft.item.ItemStack.EMPTY;
        } else {
            if (!this.insertItem(original, SemifluidGeneratorBlockEntity.SLOT_FLUID, SemifluidGeneratorBlockEntity.SLOT_FLUID + 1, false)
                    && !this.insertItem(original, SemifluidGeneratorBlockEntity.SLOT_CHARGE, SemifluidGeneratorBlockEntity.SLOT_CHARGE + 1, false)) {
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
    public int getFuel() { return props.get(2); }
    public int getTankAmount() { return props.get(3); }
    public int getTankCapacity() { return props.get(4); }
    public com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem.CellFluid getTankFluid() {
        var values = com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem.CellFluid.values();
        return values[Math.max(0, Math.min(values.length - 1, props.get(5)))];
    }
    public int getProduction() { return props.get(6); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof SemifluidGeneratorBlockEntity be) return be;
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof SemifluidGeneratorBlockEntity be) return be.getGuiProperties();
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
