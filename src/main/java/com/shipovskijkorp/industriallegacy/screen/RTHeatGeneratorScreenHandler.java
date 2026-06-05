package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.RTHeatGeneratorBlockEntity;
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

public class RTHeatGeneratorScreenHandler extends ScreenHandler {
    public static final int SLOT_COUNT = 6;
    public static final int PROP_COUNT = 4;
    public final BlockPos pos;
    private final Inventory inv;
    private final PropertyDelegate props;

    public RTHeatGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) { this(syncId, playerInv, buf.readBlockPos()); }
    private RTHeatGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) { this(syncId, playerInv, getClientInventory(playerInv, pos), getClientProps(playerInv, pos), pos); }
    public RTHeatGeneratorScreenHandler(int syncId, PlayerInventory playerInv, RTHeatGeneratorBlockEntity be) { this(syncId, playerInv, be, be.getGuiProperties(), be.getPos()); }
    public RTHeatGeneratorScreenHandler(int syncId, PlayerInventory playerInv, Inventory inv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.RT_HEAT_GENERATOR, syncId);
        checkSize(inv, SLOT_COUNT); checkDataCount(props, PROP_COUNT);
        this.pos = pos; this.inv = inv; this.props = props;
        int i = 0; for (int row=0; row<2; row++) for (int col=0; col<3; col++) addSlot(new RTGPelletSlot(inv, i++, 62 + col*18, 27 + row*18));
        addPlayerInventory(playerInv);
        addProperties(props);
    }
    private void addPlayerInventory(PlayerInventory playerInv) { int x=8, y=84; for (int row=0; row<3; row++) for (int col=0; col<9; col++) addSlot(new Slot(playerInv, col+row*9+9, x+col*18, y+row*18)); for (int col=0; col<9; col++) addSlot(new Slot(playerInv, col, x+col*18, y+58)); }
    @Override public boolean canUse(PlayerEntity player) { return inv.canPlayerUse(player); }
    @Override public ItemStack quickMove(PlayerEntity player, int index) { ItemStack ret=ItemStack.EMPTY; Slot slot=slots.get(index); if (slot==null || !slot.hasStack()) return ItemStack.EMPTY; ItemStack stack=slot.getStack(); ret=stack.copy(); if (index < SLOT_COUNT) { if (!insertItem(stack, SLOT_COUNT, slots.size(), true)) return ItemStack.EMPTY; } else if (stack.isOf(ModItems.RTG_PELLET)) { if (!insertItem(stack, 0, SLOT_COUNT, false)) return ItemStack.EMPTY; } else return ItemStack.EMPTY; if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty(); if (stack.getCount()==ret.getCount()) return ItemStack.EMPTY; slot.onTakeItem(player, stack); return ret; }
    public int getTransmitHeat() { return props.get(0); } public int getMaxHeat() { return props.get(1); } public int getHeatBuffer() { return props.get(2); } public int getPelletCount() { return props.get(3); }
    private static Inventory getClientInventory(PlayerInventory playerInv, BlockPos pos) { if (playerInv.player.getWorld().getBlockEntity(pos) instanceof RTHeatGeneratorBlockEntity be) return be; return new SimpleInventory(SLOT_COUNT); }
    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) { if (playerInv.player.getWorld().getBlockEntity(pos) instanceof RTHeatGeneratorBlockEntity be) return be.getGuiProperties(); return new ArrayPropertyDelegate(PROP_COUNT); }
    private static final class RTGPelletSlot extends Slot { RTGPelletSlot(Inventory inv, int idx, int x, int y) { super(inv,idx,x,y); } @Override public boolean canInsert(ItemStack stack) { return getStack().isEmpty() && stack.isOf(ModItems.RTG_PELLET); } @Override public int getMaxItemCount() { return 1; } @Override public int getMaxItemCount(ItemStack stack) { return 1; } }
}
