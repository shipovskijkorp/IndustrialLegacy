package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.StorageBoxBlock;
import com.shipovskijkorp.industriallegacy.item.StorageBoxBlockItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.StorageBoxScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public final class StorageBoxBlockEntity extends BlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {
    private DefaultedList<ItemStack> items;

    public StorageBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_BOX, pos, state);
        this.items = DefaultedList.ofSize(getStorageBoxType().slots(), ItemStack.EMPTY);
    }

    public StorageBoxBlock.Type getStorageBoxType() {
        return StorageBoxBlock.getType(getCachedState());
    }

    public void readInventoryFromStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        resizeInventoryToType();
        StorageBoxBlockItem.readInventoryFromStack(stack, items);
        markDirty();
    }

    public ItemStack createDroppedStack() {
        ItemStack stack = new ItemStack(getCachedState().getBlock().asItem());
        StorageBoxBlockItem.writeInventoryToStack(stack, items);
        return stack;
    }

    private void resizeInventoryToType() {
        int expected = getStorageBoxType().slots();
        if (items.size() == expected) return;

        DefaultedList<ItemStack> resized = DefaultedList.ofSize(expected, ItemStack.EMPTY);
        for (int i = 0; i < Math.min(items.size(), resized.size()); i++) {
            resized.set(i, items.get(i));
        }
        items = resized;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        resizeInventoryToType();
        Inventories.writeNbt(nbt, items);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        resizeInventoryToType();
        Inventories.readNbt(nbt, items);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new StorageBoxScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        int[] slots = new int[size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public int size() {
        resizeInventoryToType();
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        resizeInventoryToType();
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        resizeInventoryToType();
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        resizeInventoryToType();
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        resizeInventoryToType();
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clear() {
        resizeInventoryToType();
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        markDirty();
    }
}
