package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

/**
 * IL-style handheld toolbox inventory.
 * Stores data in the toolbox item NBT and allows tools/electric hand tools.
 */
public final class ToolboxInventory implements Inventory {
    public static final int SIZE = 9;

    private final PlayerEntity player;
    private final Hand hand;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    public ToolboxInventory(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
        load();
    }

    private ItemStack getContainerStack() {
        return player.getStackInHand(hand);
    }

    private boolean isValidContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ToolboxItem;
    }

    private void load() {
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        ItemStack stack = getContainerStack();
        if (!isValidContainer(stack)) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        Inventories.readNbt(nbt, stacks);
    }

    private void save() {
        ItemStack stack = getContainerStack();
        if (!isValidContainer(stack)) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        Inventories.writeNbt(nbt, stacks);
        stack.setNbt(nbt);
    }

    public static boolean isToolboxAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof ToolboxItem || item instanceof ContainmentBoxItem) return false;
        if (item instanceof IElectricItem) return true;
        if (item instanceof MiningToolItem || item instanceof SwordItem || item instanceof HoeItem || item instanceof AxeItem || item instanceof ShovelItem) return true;
        if (item instanceof ShearsItem || item instanceof FlintAndSteelItem) return true;
        return stack.isDamageable();
    }

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(stacks, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(stacks, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !isToolboxAllowed(stack)) {
            return;
        }
        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public void markDirty() {
        save();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.player == player && isValidContainer(getContainerStack());
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isToolboxAllowed(stack);
    }

    @Override
    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }
}
