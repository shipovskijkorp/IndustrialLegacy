package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.item.RadioactiveItem;
import com.shipovskijkorp.industriallegacy.item.reactor.MoxFuelRodItem;
import com.shipovskijkorp.industriallegacy.item.reactor.UraniumFuelRodItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;

/**
 * IL-style handheld containment box inventory.
 * Data is stored directly in the item stack NBT of the box held in the selected hand.
 */
public final class ContainmentBoxInventory implements Inventory {
    public static final int SIZE = 12;
    private static final String ITEMS_KEY = "Items";

    private final PlayerEntity player;
    private final Hand hand;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);

    public ContainmentBoxInventory(PlayerEntity player, Hand hand) {
        this.player = player;
        this.hand = hand;
        load();
    }

    public Hand getHand() {
        return hand;
    }

    private ItemStack getContainerStack() {
        return player.getStackInHand(hand);
    }

    private boolean isValidContainer(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ContainmentBoxItem;
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

    public static boolean isRadioactiveAllowed(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof RadioactiveItem
                || stack.getItem() instanceof UraniumFuelRodItem
                || stack.getItem() instanceof MoxFuelRodItem;
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
        if (!stack.isEmpty() && !isRadioactiveAllowed(stack)) {
            return;
        }
        stacks.set(slot, stack);
        markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
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
        return isRadioactiveAllowed(stack);
    }

    @Override
    public void clear() {
        for (int i = 0; i < SIZE; i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }
}
