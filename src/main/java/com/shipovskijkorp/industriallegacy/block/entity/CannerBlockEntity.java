package com.shipovskijkorp.industriallegacy.block.entity;

import java.util.Set;
import java.util.EnumSet;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradeableFluidMachine;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.MachineUpgradeSupport;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.CannerBlock;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.CanningEnrichRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.CannerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CannerBlockEntity extends AbstractElectricMachineBlockEntity implements UpgradeableFluidMachine {
    public enum Mode {
        BOTTLE_SOLID,
        EMPTY_LIQUID,
        BOTTLE_LIQUID,
        ENRICH_LIQUID;

        public static final Mode[] VALUES = values();
        public Mode next() { return VALUES[(ordinal() + 1) % VALUES.length]; }
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

    private int energyConsume = EU_PER_TICK;
    private int operationLength = BASE_TICKS;

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
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
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
                case 0 -> energy = clampEnergy(value);
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
        super(ModBlockEntities.CANNER, pos, state, INV_SIZE, CAPACITY, TIER, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, CannerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        dirty |= be.tickUpgrades();
        boolean active = be.processTick(world);
        if (state.get(CannerBlock.LIT) != active) world.setBlockState(pos, state.with(CannerBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    public static boolean isValidContainer(ItemStack stack) { return !stack.isEmpty(); }
    public Mode getMode() { return mode; }

    public void setMode(Mode mode) {
        if (mode == null || this.mode == mode) return;
        this.mode = mode;
        this.progress = 0;
        this.maxProgress = operationLength;
        markDirty();
    }

    public void cycleMode() { setMode(mode.next()); }

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
        CanningRecipe recipe = MachineRecipeManager.findCanningRecipe(this).orElse(null);
        if (recipe == null) { if (progress != 0) progress = 0; return false; }
        ItemStack out = recipe.getResultStack().copy();
        if (!canOutput(SLOT_OUTPUT, out)) return false;
        if (energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(recipe.getContainerCount());
            items.get(SLOT_FILL).decrement(recipe.getFillCount());
            insertOutput(SLOT_OUTPUT, out);
            progress = 0;
        }
        return true;
    }

    private boolean processEmptyLiquid() {
        ItemStack stack = items.get(SLOT_CONTAINER);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) { progress = 0; return false; }
        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || !canAcceptInputFluid(fluid, CELL_MB)) { progress = 0; return false; }
        ItemStack empty = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
        if (!canOutput(SLOT_OUTPUT, empty)) { progress = 0; return false; }
        if (energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = operationLength;
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(1);
            insertOutput(SLOT_OUTPUT, empty);
            addToInputTank(fluid, CELL_MB);
            progress = 0;
        }
        return true;
    }

    private boolean processBottleLiquid() {
        ItemStack stack = items.get(SLOT_CONTAINER);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) { progress = 0; return false; }
        if (UniversalFluidCellItem.getFluid(stack) != UniversalFluidCellItem.CellFluid.EMPTY) { progress = 0; return false; }
        if (inputTankAmount < CELL_MB || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) { progress = 0; return false; }
        ItemStack out = UniversalFluidCellItem.createStack(inputTankFluid);
        if (!canOutput(SLOT_OUTPUT, out)) { progress = 0; return false; }
        if (energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = operationLength;
        progress++;
        if (progress >= maxProgress) {
            items.get(SLOT_CONTAINER).decrement(1);
            insertOutput(SLOT_OUTPUT, out);
            inputTankAmount -= CELL_MB;
            sanitizeTanks();
            progress = 0;
        }
        return true;
    }

    private boolean processEnrichLiquid() {
        CanningEnrichRecipe recipe = findEnrichRecipe();
        if (recipe == null) { progress = 0; return false; }
        ItemStack containerStack = items.get(SLOT_CONTAINER);
        boolean bottleToCell = !containerStack.isEmpty()
                && containerStack.getItem() instanceof UniversalFluidCellItem
                && UniversalFluidCellItem.getFluid(containerStack) == UniversalFluidCellItem.CellFluid.EMPTY;
        ItemStack bottledOutput = bottleToCell ? UniversalFluidCellItem.createStack(recipe.getOutputFluid()) : ItemStack.EMPTY;
        if (bottleToCell && !canOutput(SLOT_OUTPUT, bottledOutput)) { progress = 0; return false; }
        int fluidToTank = bottleToCell ? Math.max(0, recipe.getOutputAmount() - CELL_MB) : recipe.getOutputAmount();
        if (fluidToTank > 0 && !canAcceptOutputFluid(recipe.getOutputFluid(), fluidToTank)) { progress = 0; return false; }
        if (energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        progress++;
        if (progress >= maxProgress) {
            inputTankAmount -= recipe.getInputAmount();
            if (inputTankAmount < 0) inputTankAmount = 0;
            sanitizeTanks();
            items.get(SLOT_FILL).decrement(recipe.getAdditiveCount());
            if (bottleToCell) {
                items.get(SLOT_CONTAINER).decrement(1);
                insertOutput(SLOT_OUTPUT, bottledOutput);
            }
            if (fluidToTank > 0) addToOutputTank(recipe.getOutputFluid(), fluidToTank);
            progress = 0;
        }
        return true;
    }

    private CanningEnrichRecipe findEnrichRecipe() {
        ItemStack additive = items.get(SLOT_FILL);
        if (additive.isEmpty()) return null;
        return MachineRecipeManager.findCanningEnrichRecipe(inputTankFluid, inputTankAmount, additive).orElse(null);
    }

    private boolean canAcceptInputFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (inputTankAmount <= 0 || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY;
        return inputTankFluid == fluid && inputTankAmount + amount <= TANK_CAPACITY;
    }

    private void addToInputTank(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (inputTankAmount <= 0 || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) { inputTankFluid = fluid; inputTankAmount = 0; }
        inputTankAmount = Math.min(TANK_CAPACITY, inputTankAmount + amount);
        sanitizeTanks();
    }

    private boolean canAcceptOutputFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (outputTankAmount <= 0 || outputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY;
        return outputTankFluid == fluid && outputTankAmount + amount <= TANK_CAPACITY;
    }

    private void addToOutputTank(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (outputTankAmount <= 0 || outputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY) { outputTankFluid = fluid; outputTankAmount = 0; }
        outputTankAmount = Math.min(TANK_CAPACITY, outputTankAmount + amount);
        sanitizeTanks();
    }

    private void sanitizeTanks() {
        if (inputTankAmount <= 0) { inputTankAmount = 0; inputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY; }
        if (outputTankAmount <= 0) { outputTankAmount = 0; outputTankFluid = UniversalFluidCellItem.CellFluid.EMPTY; }
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("mode", mode.ordinal());
        nbt.putString("inputTankFluid", inputTankFluid.id);
        nbt.putInt("inputTankAmount", inputTankAmount);
        nbt.putString("outputTankFluid", outputTankFluid.id);
        nbt.putInt("outputTankAmount", outputTankAmount);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        mode = Mode.VALUES[Math.max(0, Math.min(Mode.VALUES.length - 1, nbt.getInt("mode")))];
        inputTankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("inputTankFluid"));
        inputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, nbt.getInt("inputTankAmount")));
        outputTankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("outputTankFluid"));
        outputTankAmount = Math.max(0, Math.min(TANK_CAPACITY, nbt.getInt("outputTankAmount")));
        sanitizeTanks();
    }


    @Override
    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Processing,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming,
                UpgradableProperty.FluidProducing
        );
    }

    @Override
    protected void recalculateUpgrades() {
        MachineUpgradeSupport.UpgradeRates rates = MachineUpgradeSupport.calculateRates(
                this, firstUpgradeSlot, upgradeSlotCount, getUpgradableProperties(),
                BASE_TICKS, EU_PER_TICK, baseEnergyCapacity, baseSinkTier
        );
        this.energyCapacity = rates.energyStorage();
        this.sinkTier = rates.tier();
        this.energyConsume = rates.energyDemand();
        this.operationLength = rates.operationLength();
        if (energy > energyCapacity) energy = energyCapacity;
    }

    @Override
    public int fillFromUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amountMb <= 0) return 0;
        int accepted = 0;
        if (inputTankAmount <= 0 || inputTankFluid == UniversalFluidCellItem.CellFluid.EMPTY || inputTankFluid == fluid) {
            accepted = Math.min(amountMb, TANK_CAPACITY - inputTankAmount);
        }
        if (!simulate && accepted > 0) {
            addToInputTank(fluid, accepted);
            markDirty();
        }
        return accepted;
    }

    @Override
    public int drainForUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amountMb <= 0 || outputTankFluid != fluid) return 0;
        int drained = Math.min(amountMb, outputTankAmount);
        if (!simulate && drained > 0) {
            outputTankAmount -= drained;
            sanitizeTanks();
            markDirty();
        }
        return drained;
    }

    @Override
    public UniversalFluidCellItem.CellFluid getPreferredDrainFluidForUpgrade() {
        return outputTankAmount > 0 ? outputTankFluid : UniversalFluidCellItem.CellFluid.EMPTY;
    }

    @Override public PropertyDelegate getGuiProps() { return props; }
    public int getInputTankAmount() { return inputTankAmount; }
    public int getOutputTankAmount() { return outputTankAmount; }
    public UniversalFluidCellItem.CellFluid getInputTankFluid() { return inputTankFluid; }
    public UniversalFluidCellItem.CellFluid getOutputTankFluid() { return outputTankFluid; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.canner"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new CannerScreenHandler(syncId, inv, this); }
}
