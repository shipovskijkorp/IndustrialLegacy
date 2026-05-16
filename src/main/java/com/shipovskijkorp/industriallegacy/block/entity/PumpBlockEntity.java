package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.PumpBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.PumpScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * IC2 Experimental Pump.
 *
 * Source truth: TileEntityPump, IC2 2.8.222-ex112:
 * - TileEntityElectricMachine(20, 1)
 * - defaultEnergyConsume = 1 EU/t
 * - defaultOperationLength = 20 ticks
 * - tank capacity: 8000 mB
 * - pumps from the block in front, or searches nearby flowing liquid for a source block
 * - fills fluid containers from its internal tank
 */
public class PumpBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 20L;
    private static final int EU_PER_TICK = 1;
    private static final int BASE_TICKS = 20;
    private static final int TANK_CAPACITY_MB = 8_000;
    private static final int CELL_MB = 1_000;
    private static final int BUCKET_MB = 1_000;
    private static final int BOTTLE_MB = 250;
    private static final int SOURCE_SEARCH_LIMIT = 64;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;
    private UniversalFluidCellItem.CellFluid tankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int tankAmount = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 7; }

        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> tankAmount;
                case 5 -> TANK_CAPACITY_MB;
                case 6 -> tankFluid.ordinal();
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 6 -> tankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                default -> { }
            }
            sanitizeTank();
        }
    };

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, PumpBlockEntity be) {
        if (world.isClient) return;

        boolean dirty = false;
        dirty |= be.dischargeBattery();

        boolean active = be.processPumpCycle(state);
        dirty |= active;
        dirty |= be.processContainerSlot();

        if (state.get(PumpBlock.LIT) != active) {
            world.setBlockState(pos, state.with(PumpBlock.LIT, active), Block.NOTIFY_ALL);
        }
        if (dirty) be.markDirty();
    }

    public static boolean canFillFromPumpTank(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof UniversalFluidCellItem) {
            return UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.EMPTY;
        }
        return stack.isOf(Items.BUCKET) || stack.isOf(Items.GLASS_BOTTLE);
    }

    private boolean dischargeBattery() {
        ItemStack discharge = items.get(SLOT_DISCHARGE);
        if (discharge.isEmpty() || !ElectricItemManager.isElectric(discharge)) return false;
        long free = CAPACITY - energy;
        if (free <= 0L) return false;
        long extracted = ElectricItemManager.discharge(discharge, free, false);
        if (extracted > 0L) {
            energy += extracted;
            return true;
        }
        return false;
    }

    private boolean processPumpCycle(BlockState state) {
        maxProgress = BASE_TICKS;
        if (!canOperate(state)) return false;
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        progress++;
        if (progress >= maxProgress) {
            progress = 0;
            PumpResult result = findPumpResult(state, false);
            if (result != null) {
                addTankFluid(result.fluid(), result.amountMb());
            }
        }
        return true;
    }

    private boolean canOperate(BlockState state) {
        return findPumpResult(state, true) != null;
    }

    private @Nullable PumpResult findPumpResult(BlockState state, boolean simulate) {
        if (world == null) return null;
        int freeSpace = TANK_CAPACITY_MB - tankAmount;
        if (freeSpace < CELL_MB) return null;

        Direction facing = state.contains(PumpBlock.FACING) ? state.get(PumpBlock.FACING) : Direction.NORTH;
        BlockPos start = pos.offset(facing);

        PumpResult direct = tryDrainSource(start, CELL_MB, simulate);
        if (direct != null) return direct;
        BlockPos source = searchFluidSource(start);
        if (source == null) return null;
        return tryDrainSource(source, CELL_MB, simulate);
    }

    private @Nullable PumpResult tryDrainSource(BlockPos sourcePos, int maxAmount, boolean simulate) {
        if (world == null || maxAmount <= 0) return null;
        FluidAtPos fluidAtPos = getFluidAt(sourcePos);
        if (fluidAtPos.fluid() == UniversalFluidCellItem.CellFluid.EMPTY || !fluidAtPos.still()) return null;
        if (!canAcceptTankFluid(fluidAtPos.fluid(), CELL_MB)) return null;

        if (!simulate) {
            world.setBlockState(sourcePos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
        return new PumpResult(fluidAtPos.fluid(), Math.min(CELL_MB, maxAmount));
    }

    private @Nullable BlockPos searchFluidSource(BlockPos start) {
        if (world == null) return null;
        FluidAtPos startFluid = getFluidAt(start);
        if (startFluid.fluid() == UniversalFluidCellItem.CellFluid.EMPTY) return null;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= SOURCE_SEARCH_LIMIT) {
            BlockPos current = queue.removeFirst();
            FluidAtPos currentFluid = getFluidAt(current);
            if (currentFluid.fluid() != startFluid.fluid()) continue;
            if (currentFluid.still()) return current;

            for (Direction dir : Direction.values()) {
                BlockPos next = current.offset(dir);
                if (!visited.add(next)) continue;
                if (visited.size() > SOURCE_SEARCH_LIMIT) break;
                FluidAtPos nextFluid = getFluidAt(next);
                if (nextFluid.fluid() == startFluid.fluid()) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    private FluidAtPos getFluidAt(BlockPos checkPos) {
        if (world == null) return new FluidAtPos(UniversalFluidCellItem.CellFluid.EMPTY, false);
        BlockState state = world.getBlockState(checkPos);
        FluidState fluidState = world.getFluidState(checkPos);
        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.CellFluid.byBlock(state.getBlock());
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || fluidState.isEmpty()) {
            return new FluidAtPos(UniversalFluidCellItem.CellFluid.EMPTY, false);
        }
        return new FluidAtPos(fluid, fluidState.isStill());
    }

    private boolean processContainerSlot() {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY || tankAmount <= 0) return false;

        FillData fill = getFillData(input, tankFluid, tankAmount);
        if (fill == null) return false;
        if (!canOutput(fill.output())) return false;

        input.decrement(1);
        insertOutput(fill.output());
        tankAmount -= fill.amountMb();
        sanitizeTank();
        return true;
    }

    private static @Nullable FillData getFillData(ItemStack stack, UniversalFluidCellItem.CellFluid fluid, int availableMb) {
        if (stack.isEmpty() || fluid == UniversalFluidCellItem.CellFluid.EMPTY || availableMb <= 0) return null;

        if (stack.getItem() instanceof UniversalFluidCellItem) {
            if (UniversalFluidCellItem.getFluid(stack) != UniversalFluidCellItem.CellFluid.EMPTY || availableMb < CELL_MB) return null;
            return new FillData(CELL_MB, UniversalFluidCellItem.createStack(fluid));
        }

        if (stack.isOf(Items.BUCKET) && availableMb >= BUCKET_MB) {
            if (fluid == UniversalFluidCellItem.CellFluid.WATER) return new FillData(BUCKET_MB, new ItemStack(Items.WATER_BUCKET));
            if (fluid == UniversalFluidCellItem.CellFluid.LAVA) return new FillData(BUCKET_MB, new ItemStack(Items.LAVA_BUCKET));
            if (fluid == UniversalFluidCellItem.CellFluid.MILK) return new FillData(BUCKET_MB, new ItemStack(Items.MILK_BUCKET));
        }

        if (stack.isOf(Items.GLASS_BOTTLE) && fluid == UniversalFluidCellItem.CellFluid.WATER && availableMb >= BOTTLE_MB) {
            return new FillData(BOTTLE_MB, net.minecraft.potion.PotionUtil.setPotion(new ItemStack(Items.POTION), net.minecraft.potion.Potions.WATER));
        }
        return null;
    }

    public int drainTank(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amountMb <= 0) return 0;
        if (tankFluid != fluid || tankAmount <= 0) return 0;
        int drained = Math.min(amountMb, tankAmount);
        if (!simulate && drained > 0) {
            tankAmount -= drained;
            sanitizeTank();
            markDirty();
        }
        return drained;
    }

    private boolean canAcceptTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY_MB;
        return tankFluid == fluid && tankAmount + amount <= TANK_CAPACITY_MB;
    }

    private void addTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return;
        if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            tankFluid = fluid;
            tankAmount = 0;
        }
        tankAmount = Math.min(TANK_CAPACITY_MB, tankAmount + amount);
        sanitizeTank();
    }

    private void sanitizeTank() {
        if (tankAmount <= 0) {
            tankAmount = 0;
            tankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
        }
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack current = items.get(SLOT_OUTPUT);
        return current.isEmpty() || (ItemStack.canCombine(current, stack) && current.getCount() + stack.getCount() <= current.getMaxCount());
    }

    private void insertOutput(ItemStack stack) {
        ItemStack current = items.get(SLOT_OUTPUT);
        if (current.isEmpty()) items.set(SLOT_OUTPUT, stack.copy());
        else current.increment(stack.getCount());
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        nbt.putString("tankFluid", tankFluid.id);
        nbt.putInt("tankAmount", tankAmount);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.contains("maxProgress") ? nbt.getInt("maxProgress") : BASE_TICKS);
        tankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("tankFluid"));
        tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("tankAmount")));
        sanitizeTank();
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack out = Inventories.splitStack(items, slot, amount); if (!out.isEmpty()) markDirty(); return out; }
    @Override public ItemStack removeStack(int slot) { ItemStack out = Inventories.removeStack(items, slot); markDirty(); return out; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty(); }
    @Override public void clear() { for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY); }

    @Override public boolean canPlayerUse(PlayerEntity player) {
        return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override public int[] getAvailableSlots(Direction side) { return side == Direction.DOWN ? BOTTOM_SLOTS : side == Direction.UP ? TOP_SLOTS : SIDE_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_INPUT) return canFillFromPumpTank(stack);
        if (slot == SLOT_DISCHARGE) return ElectricItemManager.isElectric(stack);
        return slot >= SLOT_UPGRADE_0 && slot < SLOT_UPGRADE_0 + UPGRADE_SLOTS;
    }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }

    public PropertyDelegate getGuiProps() { return props; }
    public int getTankAmount() { return tankAmount; }
    public int getTankCapacity() { return TANK_CAPACITY_MB; }
    public UniversalFluidCellItem.CellFluid getTankFluid() { return tankFluid; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.pump"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new PumpScreenHandler(syncId, playerInventory, this); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, CAPACITY - energy);
        if (!simulate && accepted > 0L) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { return 0L; }

    private record FluidAtPos(UniversalFluidCellItem.CellFluid fluid, boolean still) { }
    private record PumpResult(UniversalFluidCellItem.CellFluid fluid, int amountMb) { }
    private record FillData(int amountMb, ItemStack output) { }
}
