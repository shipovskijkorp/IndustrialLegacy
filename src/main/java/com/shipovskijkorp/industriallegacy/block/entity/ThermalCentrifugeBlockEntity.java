package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.ThermalCentrifugeBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.ThermalCentrifugeRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ThermalCentrifugeScreenHandler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThermalCentrifugeBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_0 = 1;
    public static final int SLOT_OUTPUT_1 = 2;
    public static final int SLOT_OUTPUT_2 = 3;
    public static final int SLOT_DISCHARGE = 4;
    public static final int SLOT_UPGRADE_0 = 5;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 };

    private static final int TIER = 2;
    private static final long CAPACITY = 10000L;
    private static final int EU_PER_TICK_PROCESS = 48;
    private static final int EU_PER_HEAT_TICK = 1;
    private static final int BASE_TICKS = 500;
    private static final int MAX_HEAT = 5000;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;
    private int heat = 0;
    private int workHeat = MAX_HEAT;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 6; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> heat;
                case 5 -> workHeat;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> heat = Math.max(0, Math.min(MAX_HEAT, value));
                case 5 -> workHeat = Math.max(1, value);
                default -> { }
            }
        }
    };

    public ThermalCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_CENTRIFUGE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ThermalCentrifugeBlockEntity be) {
        if (world.isClient) return;
        boolean active = be.processTick(world);
        if (state.get(ThermalCentrifugeBlock.LIT) != active) {
            world.setBlockState(pos, state.with(ThermalCentrifugeBlock.LIT, active), 3);
        }
        if (active || be.heat > 0) be.markDirty();
    }

    private boolean processTick(World world) {
        ThermalCentrifugeRecipe recipe = findRecipe(world).orElse(null);
        int targetHeat = recipe == null ? 0 : Math.min(MAX_HEAT, recipe.getHeat());
        workHeat = Math.max(1, targetHeat == 0 ? MAX_HEAT : targetHeat);

        boolean active = false;

        if (recipe != null) {
            if (heat < targetHeat && energy >= EU_PER_HEAT_TICK) {
                energy -= EU_PER_HEAT_TICK;
                heat++;
                active = true;
            } else if (heat > targetHeat) {
                heat--;
            }

            if (heat >= targetHeat) {
                List<ItemStack> outputs = recipe.getResults();
                if (canOutput(outputs)) {
                    if (energy >= EU_PER_TICK_PROCESS) {
                        energy -= EU_PER_TICK_PROCESS;
                        maxProgress = recipe.getTicks() <= 0 ? BASE_TICKS : recipe.getTicks();
                        progress++;
                        active = true;

                        if (progress >= maxProgress) {
                            items.get(SLOT_INPUT).decrement(recipe.getInputCount());
                            insertOutputs(outputs);
                            progress = 0;
                        }
                    }
                } else {
                    progress = 0;
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
            if (heat > 0) heat--;
        }

        return active || (recipe != null && heat > 0);
    }

    private Optional<ThermalCentrifugeRecipe> findRecipe(World world) {
        return MachineRecipeManager.findThermalCentrifugeRecipe(this);
    }

    private boolean canOutput(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy());
        temp.add(items.get(SLOT_OUTPUT_1).copy());
        temp.add(items.get(SLOT_OUTPUT_2).copy());

        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (ItemStack slot : temp) {
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
                }
            }
            for (int i = 0; i < temp.size() && remaining > 0; i++) {
                ItemStack slot = temp.get(i);
                if (slot.isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    temp.set(i, placed);
                    remaining -= add;
                }
            }
            if (remaining > 0) return false;
        }

        return true;
    }

    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (int slotId : new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 }) {
                ItemStack slot = items.get(slotId);
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
                }
            }
            for (int slotId : new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 }) {
                if (remaining <= 0) break;
                ItemStack slot = items.get(slotId);
                if (slot.isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    items.set(slotId, placed);
                    remaining -= add;
                }
            }
        }
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        nbt.putInt("heat", heat);
        nbt.putInt("workHeat", workHeat);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
        heat = Math.max(0, Math.min(MAX_HEAT, nbt.getInt("heat")));
        workHeat = Math.max(1, nbt.getInt("workHeat"));
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
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return slot != SLOT_OUTPUT_0 && slot != SLOT_OUTPUT_1 && slot != SLOT_OUTPUT_2; }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT_0 || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2; }

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

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.thermal_centrifuge"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new ThermalCentrifugeScreenHandler(syncId, inv, this); }
}
