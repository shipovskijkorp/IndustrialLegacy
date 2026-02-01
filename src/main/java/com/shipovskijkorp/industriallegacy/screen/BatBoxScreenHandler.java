package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class BatBoxScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 2;
    public static final int PROP_COUNT = 4;

    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    // Client ctor (registerExtended)
    public BatBoxScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    // Client helper
    private BatBoxScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos);
    }

    // Server ctor (real BE)
    public BatBoxScreenHandler(int syncId, PlayerInventory playerInv, BatBoxBlockEntity be) {
        this(syncId, playerInv, be, be.getGuiProperties(), be.getPos());
    }

    public BatBoxScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.BATBOX, syncId);
        checkSize(inv, SLOT_COUNT);
        checkDataCount(props, PROP_COUNT);

        this.pos = pos;
        this.inv = inv;
        this.props = props;

        // IL coords
        this.addSlot(new Slot(inv, 0, 56, 17)); // charge
        this.addSlot(new Slot(inv, 1, 56, 53)); // discharge

        // IL: 4 armor slots row at y=84
        // PlayerInventory indices: 36..39 = boots..helmet
        this.addSlot(new EquipmentArmorSlot(playerInv, 36, 8,  84, EquipmentSlot.FEET));
        this.addSlot(new EquipmentArmorSlot(playerInv, 37, 26, 84, EquipmentSlot.LEGS));
        this.addSlot(new EquipmentArmorSlot(playerInv, 38, 44, 84, EquipmentSlot.CHEST));
        this.addSlot(new EquipmentArmorSlot(playerInv, 39, 62, 84, EquipmentSlot.HEAD));

        // player inventory lower because GUI height is 196
        // Shift player inventory + hotbar 2px to the right on X (requested).
        int startX = 8;
        int startY = 114;
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

            int containerSlots = SLOT_COUNT + 4; // batbox(2) + armor(4)
            if (index < containerSlots) {
                if (!this.insertItem(original, containerSlots, this.slots.size(), true)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            } else {
                // try into charge then discharge
                if (!this.insertItem(original, 0, 1, false) && !this.insertItem(original, 1, 2, false)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY);
            else slot.markDirty();
        }
        return newStack;
    }

    public int getEuStored()    { return props.get(0); }
    public int getEuCap()       { return props.get(1); }
    public int getOutputEUt()   { return props.get(2); }
    public int getRedstoneMode(){ return props.get(3); }

    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity bat) {
            return bat;
        }
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity bat) {
            return bat.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }


    private static final class NonStackingElectricSlot extends Slot {
        NonStackingElectricSlot(Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return ElectricItemManager.isElectric(stack);
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return 1;
        }
    }

    private static final class EquipmentArmorSlot extends Slot {
        private final EquipmentSlot expected;

        EquipmentArmorSlot(PlayerInventory inv, int index, int x, int y, EquipmentSlot expected) {
            super(inv, index, x, y);
            this.expected = expected;
        }

        @Override
        public boolean canInsert(net.minecraft.item.ItemStack stack) {
            if (!(stack.getItem() instanceof ArmorItem armor)) return false;
            return armor.getSlotType() == expected;
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
