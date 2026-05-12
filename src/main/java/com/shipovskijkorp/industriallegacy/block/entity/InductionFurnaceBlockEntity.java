package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.InductionFurnaceBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.InductionFurnaceScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
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

public class InductionFurnaceBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_INPUT_A = 0;
    public static final int SLOT_INPUT_B = 1;
    public static final int SLOT_OUTPUT_A = 2;
    public static final int SLOT_OUTPUT_B = 3;
    public static final int SLOT_DISCHARGE = 4;
    public static final int SLOT_UPGRADE_0 = 5;
    public static final int UPGRADE_SLOTS = 2;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    public static final int MAX_HEAT = 10000;
    public static final int MAX_PROGRESS = 4000;

    private static final int[] TOP_SLOTS = new int[]{SLOT_INPUT_A, SLOT_INPUT_B};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_INPUT_A, SLOT_INPUT_B, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT_A, SLOT_OUTPUT_B};

    private static final int TIER = 2;
    private static final long CAPACITY = 10000L;
    private static final int HEATUP_EU_PER_TICK = 1;
    private static final int PROCESS_EU_PER_TICK = 15;
    private static final int COOLDOWN_PER_TICK = 4;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int heat = 0;
    private int progress = 0;
    private final PropertyDelegate props = new PropertyDelegate() {
        @Override
        public int size() {
            return 6;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> heat;
                case 3 -> MAX_HEAT;
                case 4 -> progress;
                case 5 -> MAX_PROGRESS;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> heat = Math.max(0, Math.min(MAX_HEAT, value));
                case 4 -> progress = Math.max(0, Math.min(MAX_PROGRESS, value));
                default -> { }
            }
        }
    };

    public InductionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTION_FURNACE, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, InductionFurnaceBlockEntity be) {
        if (world.isClient) return;

        boolean dirty = false;
        dirty |= be.chargeFromDischargeSlot();

        boolean newActive = state.get(InductionFurnaceBlock.LIT);
        if (be.heat == 0) {
            newActive = false;
        }

        if (be.progress >= MAX_PROGRESS) {
            dirty |= be.operate(world);
            be.progress = 0;
            newActive = false;
        }

        boolean canOperate = be.canOperate(world);
        boolean redstone = world.isReceivingRedstonePower(pos);

        if ((canOperate || redstone) && be.useEnergy(HEATUP_EU_PER_TICK)) {
            if (be.heat < MAX_HEAT) {
                be.heat++;
            }
            newActive = true;
            dirty = true;
        } else if (be.heat > 0) {
            be.heat -= Math.min(be.heat, COOLDOWN_PER_TICK);
            dirty = true;
        }

        if (!newActive || be.progress == 0) {
            if (canOperate) {
                if (be.energy >= PROCESS_EU_PER_TICK) {
                    newActive = true;
                }
            } else {
                if (be.progress != 0) {
                    be.progress = 0;
                    dirty = true;
                }
            }
        } else if (!canOperate || be.energy < PROCESS_EU_PER_TICK) {
            if (!canOperate && be.progress != 0) {
                be.progress = 0;
                dirty = true;
            }
            newActive = false;
        }

        if (newActive && canOperate) {
            be.progress += be.heat / 30;
            be.useEnergy(PROCESS_EU_PER_TICK);
            dirty = true;
        }

        if (state.get(InductionFurnaceBlock.LIT) != newActive) {
            world.setBlockState(pos, state.with(InductionFurnaceBlock.LIT, newActive), 3);
            dirty = true;
        }

        if (dirty) {
            be.markDirty();
            world.updateComparators(pos, state.getBlock());
        }
    }

    private boolean chargeFromDischargeSlot() {
        ItemStack discharge = items.get(SLOT_DISCHARGE);
        if (discharge.isEmpty() || !ElectricItemManager.isElectric(discharge) || discharge.getCount() != 1) return false;
        long free = CAPACITY - energy;
        if (free <= 0L) return false;
        long maxMove = Math.min(free, EuUtil.powerFromTier(TIER));
        long extracted = ElectricItemManager.discharge(discharge, maxMove, false);
        if (extracted > 0L) {
            energy += extracted;
            return true;
        }
        return false;
    }

    private boolean useEnergy(long amount) {
        if (amount <= 0L) return true;
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }

    private boolean canOperate(World world) {
        return canOperate(world, SLOT_INPUT_A, SLOT_OUTPUT_A) || canOperate(world, SLOT_INPUT_B, SLOT_OUTPUT_B);
    }

    private boolean canOperate(World world, int inputSlot, int outputSlot) {
        ItemStack input = items.get(inputSlot);
        if (input.isEmpty()) return false;
        SmeltingMatch match = findRecipe(world, input).orElse(null);
        return match != null && canOutput(outputSlot, match.output());
    }

    private boolean operate(World world) {
        boolean did = false;
        did |= operate(world, SLOT_INPUT_A, SLOT_OUTPUT_A);
        did |= operate(world, SLOT_INPUT_B, SLOT_OUTPUT_B);
        return did;
    }

    private boolean operate(World world, int inputSlot, int outputSlot) {
        ItemStack input = items.get(inputSlot);
        if (input.isEmpty()) return false;
        SmeltingMatch match = findRecipe(world, input).orElse(null);
        if (match == null || !canOutput(outputSlot, match.output())) return false;

        input.decrement(1);
        insertOutput(outputSlot, match.output().copy());
        return true;
    }

    private Optional<SmeltingMatch> findRecipe(World world, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        Optional<?> opt = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SimpleInventory(input.copy()), world);
        if (opt.isEmpty()) return Optional.empty();

        Object o = opt.get();
        if (o instanceof AbstractCookingRecipe recipe) {
            return Optional.of(new SmeltingMatch(recipe.getOutput(world.getRegistryManager()).copy()));
        }

        try {
            Method value = o.getClass().getMethod("value");
            Object recipeObj = value.invoke(o);
            if (recipeObj instanceof AbstractCookingRecipe recipe) {
                return Optional.of(new SmeltingMatch(recipe.getOutput(world.getRegistryManager()).copy()));
            }
        } catch (Throwable ignored) {
        }

        return Optional.empty();
    }

    private boolean canOutput(int slotId, ItemStack stack) {
        if (stack.isEmpty()) return false;
        ItemStack out = items.get(slotId);
        if (out.isEmpty()) return true;
        if (!ItemStack.canCombine(out, stack)) return false;
        return out.getCount() + stack.getCount() <= out.getMaxCount();
    }

    private void insertOutput(int slotId, ItemStack stack) {
        ItemStack out = items.get(slotId);
        if (out.isEmpty()) {
            items.set(slotId, stack);
        } else {
            out.increment(stack.getCount());
        }
    }

    public int getComparatorOutput() {
        return heat * 15 / MAX_HEAT;
    }

    public PropertyDelegate getGuiProps() {
        return props;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("heat", heat);
        nbt.putInt("progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        heat = Math.max(0, Math.min(MAX_HEAT, nbt.getInt("heat")));
        progress = Math.max(0, Math.min(MAX_PROGRESS, nbt.getInt("progress")));
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT_A || slot == SLOT_OUTPUT_B) return false;
        if (slot == SLOT_DISCHARGE) return ElectricItemManager.isElectric(stack);
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT_A || slot == SLOT_OUTPUT_B;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.induction_furnace");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new InductionFurnaceScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, CAPACITY - energy);
        if (!simulate && accepted > 0L) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0L;
    }

    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }

    private record SmeltingMatch(ItemStack output) {}
}
