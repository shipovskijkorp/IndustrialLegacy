package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.screen.slot.FilteredSlot;
import com.shipovskijkorp.industriallegacy.block.entity.GeneratorBlockEntity;
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

public class GeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 2;
    public static final int PROP_COUNT = 4;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    // Client ctor (registerExtended)
    public GeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    // Client helper
    private GeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    // Server ctor (real BE)
    public GeneratorScreenHandler(int syncId, PlayerInventory playerInv, GeneratorBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public GeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);

        this.pos = pos;
        this.inv = inv;
        this.props = props;

        // IL coords (generator.xml)
        this.addSlot(new FilteredSlot(inv, GeneratorBlockEntity.SLOT_CHARGE, 57, 17)); // charge
        this.addSlot(new Slot(inv, GeneratorBlockEntity.SLOT_FUEL, 57, 53) {
            @Override
            public boolean canInsert(net.minecraft.item.ItemStack stack) {
                return GeneratorBlockEntity.isValidFuel(stack);
            }
        });

        // player inventory (7,83)
        int startX = 8;
        int startY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        // hotbar
        int hotbarY = startY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, startX + col * 18, hotbarY));
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
        if (slot != null && slot.hasStack()) {
            net.minecraft.item.ItemStack original = slot.getStack();
            newStack = original.copy();

            int containerSlots = SLOT_COUNT;
            if (index < containerSlots) {
                if (!this.insertItem(original, containerSlots, this.slots.size(), true)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            } else {
                // try fuel then charge
                if (!this.insertItem(original, 1, 2, false) && !this.insertItem(original, 0, 1, false)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    public int getEuStored() { return props.get(0); }
    public int getEuCap()    { return props.get(1); }
    public int getFuel()     { return props.get(2); }
    public int getFuelMax()  { return props.get(3); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            return gen;
        }
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            return gen.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
