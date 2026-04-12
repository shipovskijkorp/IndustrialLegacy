package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.SolidCannerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import com.shipovskijkorp.industriallegacy.screen.SolidCannerScreenHandler;
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
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

public class SolidCannerBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_CONTAINER = 0;
    public static final int SLOT_FILL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_DISCHARGE = 3;
    public static final int SLOT_UPGRADE_0 = 4;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_CONTAINER, SLOT_FILL };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_CONTAINER, SLOT_FILL, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 600L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 200;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 4; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                default -> { }
            }
        }
    };

    public SolidCannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLID_CANNER, pos, state);
    }

    public static boolean isValidContainer(ItemStack stack) {
        return stack.isOf(ModItems.TIN_CAN);
    }

    public static void tick(World world, BlockPos pos, BlockState state, SolidCannerBlockEntity be) {
        if (world.isClient) return;
        boolean active = be.processTick(world);
        if (state.get(SolidCannerBlock.LIT) != active) {
            world.setBlockState(pos, state.with(SolidCannerBlock.LIT, active), 3);
        }
        if (active) be.markDirty();
    }

    private boolean processTick(World world) {
        CanningRecipe recipe = findRecipe(world).orElse(null);
        if (recipe == null) {
            if (progress != 0) progress = 0;
            return false;
        }

        ItemStack out = recipe.getResultStack().copy();
        if (!canOutput(out)) return false;
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = recipe.getTicks() <= 0 ? BASE_TICKS : recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(recipe.getContainerCount());
            items.get(SLOT_FILL).decrement(recipe.getFillCount());
            insertOutput(out);
            progress = 0;
        }

        return true;
    }

    private Optional<CanningRecipe> findRecipe(World world) {
        Optional<?> opt = world.getRecipeManager().getFirstMatch(ModRecipes.CANNING_TYPE, this, world);
        if (opt.isEmpty()) return Optional.empty();
        Object o = opt.get();
        if (o instanceof CanningRecipe r) return Optional.of(r);
        try {
            Method m = o.getClass().getMethod("value");
            Object v = m.invoke(o);
            if (v instanceof CanningRecipe r) return Optional.of(r);
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack current = items.get(SLOT_OUTPUT);
        return current.isEmpty() || (ItemStack.canCombine(current, stack) && current.getCount() + stack.getCount() <= current.getMaxCount());
    }

    private void insertOutput(ItemStack stack) {
        ItemStack current = items.get(SLOT_OUTPUT);
        if (current.isEmpty()) items.set(SLOT_OUTPUT, stack);
        else current.increment(stack.getCount());
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack out = Inventories.splitStack(items, slot, amount); if (!out.isEmpty()) markDirty(); return out; }
    @Override public ItemStack removeStack(int slot) { ItemStack out = Inventories.removeStack(items, slot); markDirty(); return out; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty(); }
    @Override public void clear() { for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY); }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override public int[] getAvailableSlots(Direction side) { return side == Direction.UP ? TOP_SLOTS : side == Direction.DOWN ? BOTTOM_SLOTS : SIDE_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return slot != SLOT_OUTPUT && (slot != SLOT_CONTAINER || isValidContainer(stack)); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0) return 0;
        long free = CAPACITY - energy;
        if (free <= 0) return 0;
        long accepted = Math.min(amount, free);
        if (!simulate && accepted > 0) { energy += accepted; markDirty(); }
        return accepted;
    }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { return 0; }

    public PropertyDelegate getGuiProps() { return props; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.solid_canner"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new SolidCannerScreenHandler(syncId, inv, this); }
}
