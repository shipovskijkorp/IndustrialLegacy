package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.RTGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.RTGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RTGeneratorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_COUNT = 6;
    private static final int[] ALL_SLOTS = new int[] { 0, 1, 2, 3, 4, 5 };
    private static final int TIER = 1;
    private static final long CAPACITY = 20_000L;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
    private long energy;
    private double fractionalEnergy;
    private final double efficiency;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 3; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> countPellets();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) energy = Math.max(0L, Math.min(CAPACITY, value));
        }
    };

    public RTGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RT_GENERATOR, pos, state);
        this.efficiency = Math.max(0.0, ILConfig.getFloat("balance/energy/generator/radioisotope", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, RTGeneratorBlockEntity be) {
        if (world.isClient) return;

        boolean active = be.gainEnergy();
        be.emitToNeighbors();

        if (state.contains(RTGeneratorBlock.LIT) && state.get(RTGeneratorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(RTGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        }
        if (active || be.energy > 0L) be.markDirty();
    }

    private boolean gainEnergy() {
        int pellets = countPellets();
        if (pellets <= 0 || efficiency <= 0.0 || energy >= CAPACITY) {
            return false;
        }

        fractionalEnergy += Math.pow(2.0, pellets - 1) * efficiency;
        long whole = (long) Math.floor(fractionalEnergy);
        if (whole <= 0L) {
            return true;
        }

        long accepted = Math.min(whole, CAPACITY - energy);
        energy += accepted;
        fractionalEnergy -= accepted;
        if (energy >= CAPACITY) fractionalEnergy = Math.min(fractionalEnergy, 0.999999);
        return true;
    }

    public int countPellets() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.isOf(ModItems.RTG_PELLET)) count++;
        }
        return count;
    }

    public double getGenerationRateEuPerTick() {
        int pellets = countPellets();
        return pellets <= 0 ? 0.0 : Math.pow(2.0, pellets - 1) * efficiency;
    }

    private void emitToNeighbors() {
        if (world == null || energy <= 0L) return;
        long packet = Math.min(energy, EuUtil.powerFromTier(TIER));
        long remaining = packet;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0L) break;
            long spent = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= spent;
        }
    }

    public PropertyDelegate getGuiProperties() { return props; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putDouble("fractionalEnergy", fractionalEnergy);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fractionalEnergy = Math.max(0.0, Math.min(0.999999, nbt.getDouble("fractionalEnergy")));
        sanitizePelletStacks();
    }

    private void sanitizePelletStacks() {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            if (!isValid(i, stack)) {
                items.set(i, ItemStack.EMPTY);
            } else if (stack.getCount() > 1) {
                stack.setCount(1);
            }
        }
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); markDirty(); return r; }

    @Override
    public void setStack(int slot, ItemStack stack) {
        ItemStack toStore = ItemStack.EMPTY;
        if (stack != null && !stack.isEmpty() && isValid(slot, stack)) {
            toStore = stack.copy();
            toStore.setCount(1);
        }
        items.set(slot, toStore);
        markDirty();
    }

    @Override public int getMaxCountPerStack() { return 1; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < SLOT_COUNT && stack.isOf(ModItems.RTG_PELLET);
    }

    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return ALL_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return items.get(slot).isEmpty() && isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot >= 0 && slot < SLOT_COUNT; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.rt_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new RTGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return TIER; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { return 0L; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { long ex = Math.min(Math.max(0L, amount), energy); if (!simulate) energy -= ex; return ex; }
    @Override public boolean canInsert(Direction from) { return false; }
    @Override public boolean canExtract(Direction to) { return true; }
}
