package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.CannerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.CanningEnrichRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.CannerScreenHandler;
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

import java.util.Optional;

public class CannerBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public enum Mode {
        BOTTLE_SOLID,
        EMPTY_LIQUID,
        BOTTLE_LIQUID,
        ENRICH_LIQUID;

        public static final Mode[] VALUES = values();

        public Mode next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

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
    private static final long CAPACITY = 800L;
    private static final int EU_PER_TICK = 4;
    private static final int BASE_TICKS = 200;
    private static final int TANK_CAPACITY = 8000;
    private static final int CELL_MB = 1000;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;
    private Mode mode = Mode.BOTTLE_SOLID;

    private UniversalFluidCellItem.CellFluid inputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int inputTankAmount = 0;
    private UniversalFluidCellItem.CellFluid outputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int outputTankAmount = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 9; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> mode.ordinal();
                case 5 -> inputTankAmount;
                case 6 -> outputTankAmount;
                case 7 -> inputTankFluid.ordinal();
                case 8 -> outputTankFluid.ordinal();
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> mode = Mode.VALUES[Math.max(0, Math.min(Mode.VALUES.length - 1, value))];
                case 5 -> inputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, value));
                case 6 -> outputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, value));
                case 7 -> inputTankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                case 8 -> outputTankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                default -> { }
            }
        }
    };

    public CannerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANNER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CannerBlockEntity be) {
        if (world.isClient) return;
        boolean active = be.processTick(world);
        if (state.get(CannerBlock.LIT) != active) {
            world.setBlockState(pos, state.with(CannerBlock.LIT, active), 3);
        }
        if (active) be.markDirty();
    }

    public static boolean isValidContainer(ItemStack stack) {
        return !stack.isEmpty();
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        if (mode == null || this.mode == mode) return;
        this.mode = mode;
        this.progress = 0;
        this.maxProgress = BASE_TICKS;
        markDirty();
    }

    public void cycleMode() {
        setMode(mode.next());
    }

    public void swapTanks() {
        UniversalFluidCellItem.CellFluid fluid = inputTankFluid;
        int amount = inputTankAmount;
        inputTankFluid = outputTankFluid;
        inputTankAmount = outputTankAmount;
        outputTankFluid = fluid;
        outputTankAmount = amount;
        sanitizeTanks();
        markDirty();
    }

    private boolean processTick(World world) {
        return switch (mode) {
            case BOTTLE_SOLID -> processBottleSolid(world);
            case EMPTY_LIQUID -> processEmptyLiquid();
            case BOTTLE_LIQUID -> processBottleLiquid();
            case ENRICH_LIQUID -> processEnrichLiquid();
        };
    }

    private boolean processBottleSolid(World world) {
        CanningRecipe recipe = findRecipe(world).orElse(null);
        if (recipe == null) {
            if (progress != 0) progress = 0;
            return false;
        }

        ItemStack out = recipe.getResultStack().copy();
        if (!canOutput(out)) return false;
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(recipe.getContainerCount());
            items.get(SLOT_FILL).decrement(recipe.getFillCount());
            insertOutput(out);
            progress = 0;
        }

        return true;
    }

    private boolean processEmptyLiquid() {
        ItemStack stack = items.get(SLOT_CONTAINER);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) {
            progress = 0;
            return false;
        }

        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            progress = 0;
            return false;
        }
        if (!canAcceptInputFluid(fluid, CELL_MB)) {
            progress = 0;
            return false;
        }
        if (!canOutput(UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY))) {
            progress = 0;
            return false;
        }
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = BASE_TICKS;
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(1);
            insertOutput(UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
            addToInputTank(fluid, CELL_MB);
            progress = 0;
        }
        return true;
    }

    private boolean processBottleLiquid() {
        ItemStack stack = items.get(SLOT_CONTAINER);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) {
            progress = 0;
            return false;
        }

        if (UniversalFluidCellItem.getFluid(stack) != UniversalFluidCellItem.CellFluid.EMPTY) {
            progress = 0;
            return false;
        }
        if (inputTankAmount < CELL_MB || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            progress = 0;
            return false;
        }

        ItemStack out = UniversalFluidCellItem.createStack(inputTankFluid);
        if (!canOutput(out)) {
            progress = 0;
            return false;
        }
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = BASE_TICKS;
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(1);
            insertOutput(out);
            inputTankAmount -= CELL_MB;
            sanitizeTanks();
            progress = 0;
        }
        return true;
    }

    private boolean processEnrichLiquid() {
        CanningEnrichRecipe recipe = findEnrichRecipe();
        if (recipe == null) {
            progress = 0;
            return false;
        }

        ItemStack containerStack = items.get(SLOT_CONTAINER);
        boolean bottleToCell = !containerStack.isEmpty()
                && containerStack.getItem() instanceof UniversalFluidCellItem
                && UniversalFluidCellItem.getFluid(containerStack) == UniversalFluidCellItem.CellFluid.EMPTY;

        ItemStack bottledOutput = bottleToCell ? UniversalFluidCellItem.createStack(recipe.getOutputFluid()) : ItemStack.EMPTY;
        if (bottleToCell && !canOutput(bottledOutput)) {
            progress = 0;
            return false;
        }

        int fluidToTank = bottleToCell ? Math.max(0, recipe.getOutputAmount() - CELL_MB) : recipe.getOutputAmount();
        if (fluidToTank > 0 && !canAcceptOutputFluid(recipe.getOutputFluid(), fluidToTank)) {
            progress = 0;
            return false;
        }

        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            inputTankAmount -= recipe.getInputAmount();
            if (inputTankAmount < 0) inputTankAmount = 0;
            sanitizeTanks();

            items.get(SLOT_FILL).decrement(recipe.getAdditiveCount());

            if (bottleToCell) {
                items.get(SLOT_CONTAINER).decrement(1);
                insertOutput(bottledOutput);
            }

            if (fluidToTank > 0) {
                addToOutputTank(recipe.getOutputFluid(), fluidToTank);
            }

            progress = 0;
        }

        return true;
    }

    private boolean canAcceptInputFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (inputTankAmount <= 0 || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            return amount <= TANK_CAPACITY;
        }
        return inputTankFluid == fluid && inputTankAmount + amount <= TANK_CAPACITY;
    }

    private void addToInputTank(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (inputTankAmount <= 0 || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            inputTankFluid = fluid;
            inputTankAmount = 0;
        }
        inputTankAmount = Math.min(TANK_CAPACITY, inputTankAmount + amount);
        sanitizeTanks();
    }

    private void sanitizeTanks() {
        if (inputTankAmount <= 0) {
            inputTankAmount = 0;
            inputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
        }
        if (outputTankAmount <= 0) {
            outputTankAmount = 0;
            outputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
        }
    }

    private CanningEnrichRecipe findEnrichRecipe() {
        ItemStack additive = items.get(SLOT_FILL);
        if (additive.isEmpty()) return null;
        return MachineRecipeManager.findCanningEnrichRecipe(inputTankFluid, inputTankAmount, additive).orElse(null);
    }

    private boolean canAcceptOutputFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (outputTankAmount <= 0 || outputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            return amount <= TANK_CAPACITY;
        }
        return outputTankFluid == fluid && outputTankAmount + amount <= TANK_CAPACITY;
    }

    private void addToOutputTank(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (outputTankAmount <= 0 || outputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            outputTankFluid = fluid;
            outputTankAmount = 0;
        }
        outputTankAmount = Math.min(TANK_CAPACITY, outputTankAmount + amount);
        sanitizeTanks();
    }


    private Optional<CanningRecipe> findRecipe(World world) {
        return MachineRecipeManager.findCanningRecipe(this);
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
        nbt.putInt("mode", mode.ordinal());
        nbt.putString("inputTankFluid", inputTankFluid.id);
        nbt.putInt("inputTankAmount", inputTankAmount);
        nbt.putString("outputTankFluid", outputTankFluid.id);
        nbt.putInt("outputTankAmount", outputTankAmount);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
        mode = Mode.VALUES[Math.max(0, Math.min(Mode.VALUES.length - 1, nbt.getInt("mode")))];
        inputTankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("inputTankFluid"));
        inputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, nbt.getInt("inputTankAmount")));
        outputTankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("outputTankFluid"));
        outputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, nbt.getInt("outputTankAmount")));
        sanitizeTanks();
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
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return slot != SLOT_OUTPUT; }
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
    public int getInputTankAmount() { return inputTankAmount; }
    public int getOutputTankAmount() { return outputTankAmount; }
    public UniversalFluidCellItem.CellFluid getInputTankFluid() { return inputTankFluid; }
    public UniversalFluidCellItem.CellFluid getOutputTankFluid() { return outputTankFluid; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.canner"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new CannerScreenHandler(syncId, inv, this); }
}
