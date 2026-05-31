package com.shipovskijkorp.industriallegacy.block.entity;

import java.util.Set;
import java.util.EnumSet;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradeableFluidMachine;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.MachineUpgradeSupport;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.FluidBottlerBlock;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.item.armor.BiogasJetpackItem;
import com.shipovskijkorp.industriallegacy.item.armor.FoamPackItem;
import com.shipovskijkorp.industriallegacy.item.tool.FoamSprayerItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.FluidBottlerScreenHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** IL Experimental Fluid Bottler / Bottling Plant. */
public class FluidBottlerBlockEntity extends AbstractElectricMachineBlockEntity implements UpgradeableFluidMachine {
    public static final int SLOT_DRAIN = 0;
    public static final int SLOT_FILL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_DISCHARGE = 3;
    public static final int SLOT_UPGRADE_0 = 4;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_DRAIN };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_DRAIN, SLOT_FILL, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 800L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 100;
    private static final int TANK_CAPACITY_MB = 8_000;
    private static final int CELL_MB = 1_000;
    private static final int BUCKET_MB = 1_000;
    private static final int BOTTLE_MB = 250;

    private int energyConsume = EU_PER_TICK;
    private int operationLength = BASE_TICKS;

    private UniversalFluidCellItem.CellFluid tankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int tankAmount = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 7; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
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
                case 0 -> energy = clampEnergy(value);
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 6 -> tankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                default -> { }
            }
            sanitizeTank();
        }
    };

    public FluidBottlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BOTTLER, pos, state, INV_SIZE, CAPACITY, TIER, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, FluidBottlerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        dirty |= be.tickUpgrades();
        boolean active = be.processTick();
        if (state.get(FluidBottlerBlock.LIT) != active) world.setBlockState(pos, state.with(FluidBottlerBlock.LIT, active), Block.NOTIFY_ALL);
        if (active || dirty) be.markDirty();
    }

    public static boolean canDrainContainer(ItemStack stack) { return getDrainData(stack) != null; }

    public static boolean canFillContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof UniversalFluidCellItem) return UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.EMPTY;
        if (stack.getItem() instanceof BiogasJetpackItem) return BiogasJetpackItem.getFuel(stack) < BiogasJetpackItem.CAPACITY_MB;
        if (stack.getItem() instanceof FoamSprayerItem) return FoamSprayerItem.canFill(stack);
        if (stack.getItem() instanceof FoamPackItem) return FoamPackItem.canFill(stack);
        return stack.isOf(Items.BUCKET) || stack.isOf(Items.GLASS_BOTTLE);
    }

    private boolean processTick() {
        TransferAction action = findAction();
        if (action == null) { progress = 0; maxProgress = operationLength; return false; }
        if (energy < energyConsume) return false;
        energy -= energyConsume;
        maxProgress = operationLength;
        progress++;
        if (progress >= maxProgress) {
            action.apply();
            progress = 0;
            markDirty();
        }
        return true;
    }

    private TransferAction findAction() {
        TransferAction drain = findDrainAction();
        return drain != null ? drain : findFillAction();
    }

    private TransferAction findDrainAction() {
        ItemStack stack = items.get(SLOT_DRAIN);
        DrainData data = getDrainData(stack);
        if (data == null) return null;
        if (!canAcceptTankFluid(data.fluid(), data.amountMb())) return null;
        if (!canOutput(SLOT_OUTPUT, data.output())) return null;
        return () -> {
            items.get(SLOT_DRAIN).decrement(1);
            insertOutput(SLOT_OUTPUT, data.output());
            addTankFluid(data.fluid(), data.amountMb());
        };
    }

    private TransferAction findFillAction() {
        ItemStack stack = items.get(SLOT_FILL);
        if (stack.isEmpty() || tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return null;
        FillData data = getFillData(stack, tankFluid, tankAmount);
        if (data == null) return null;
        if (!canOutput(SLOT_OUTPUT, data.output())) return null;
        return () -> {
            items.get(SLOT_FILL).decrement(1);
            insertOutput(SLOT_OUTPUT, data.output());
            tankAmount -= data.amountMb();
            sanitizeTank();
        };
    }

    private static @Nullable DrainData getDrainData(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof UniversalFluidCellItem) {
            UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) return new DrainData(UniversalFluidCellItem.CellFluid.AIR, CELL_MB, UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
            return new DrainData(fluid, CELL_MB, UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
        }
        if (stack.getItem() instanceof BiogasJetpackItem) {
            int fuel = BiogasJetpackItem.getFuel(stack);
            if (fuel <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); BiogasJetpackItem.setFuel(out, 0);
            return new DrainData(UniversalFluidCellItem.CellFluid.BIOGAS, fuel, out);
        }
        if (stack.getItem() instanceof FoamSprayerItem) {
            int foam = FoamSprayerItem.getFoam(stack);
            if (foam <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); FoamSprayerItem.setFoam(out, 0);
            return new DrainData(UniversalFluidCellItem.CellFluid.CONSTRUCTION_FOAM, foam, out);
        }
        if (stack.getItem() instanceof FoamPackItem) {
            int foam = FoamPackItem.getFoam(stack);
            if (foam <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); FoamPackItem.setFoam(out, 0);
            return new DrainData(UniversalFluidCellItem.CellFluid.CONSTRUCTION_FOAM, foam, out);
        }
        UniversalFluidCellItem.CellFluid bucketFluid = getFilledBucketFluid(stack);
        if (bucketFluid != UniversalFluidCellItem.CellFluid.EMPTY) return new DrainData(bucketFluid, BUCKET_MB, new ItemStack(Items.BUCKET));
        if (isWaterBottle(stack)) return new DrainData(UniversalFluidCellItem.CellFluid.WATER, BOTTLE_MB, new ItemStack(Items.GLASS_BOTTLE));
        return null;
    }

    private static @Nullable FillData getFillData(ItemStack stack, UniversalFluidCellItem.CellFluid fluid, int availableMb) {
        if (stack.isEmpty() || fluid == UniversalFluidCellItem.CellFluid.EMPTY || availableMb <= 0) return null;
        if (stack.getItem() instanceof UniversalFluidCellItem) {
            if (UniversalFluidCellItem.getFluid(stack) != UniversalFluidCellItem.CellFluid.EMPTY || availableMb < CELL_MB) return null;
            return new FillData(CELL_MB, UniversalFluidCellItem.createStack(fluid));
        }
        if (stack.getItem() instanceof BiogasJetpackItem && fluid == UniversalFluidCellItem.CellFluid.BIOGAS) {
            int current = BiogasJetpackItem.getFuel(stack);
            int fill = Math.min(availableMb, BiogasJetpackItem.CAPACITY_MB - current);
            if (fill <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); BiogasJetpackItem.setFuel(out, current + fill);
            return new FillData(fill, out);
        }
        if (stack.getItem() instanceof FoamSprayerItem && fluid == UniversalFluidCellItem.CellFluid.CONSTRUCTION_FOAM) {
            int fill = Math.min(availableMb, FoamSprayerItem.CAPACITY_MB - FoamSprayerItem.getFoam(stack));
            if (fill <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); FoamSprayerItem.fill(out, fill);
            return new FillData(fill, out);
        }
        if (stack.getItem() instanceof FoamPackItem && fluid == UniversalFluidCellItem.CellFluid.CONSTRUCTION_FOAM) {
            int fill = Math.min(availableMb, FoamPackItem.CAPACITY_MB - FoamPackItem.getFoam(stack));
            if (fill <= 0) return null;
            ItemStack out = stack.copy(); out.setCount(1); FoamPackItem.fill(out, fill);
            return new FillData(fill, out);
        }
        if (stack.isOf(Items.BUCKET) && availableMb >= BUCKET_MB) {
            ItemStack bucket = createFilledBucket(fluid);
            if (!bucket.isEmpty()) return new FillData(BUCKET_MB, bucket);
        }
        if (stack.isOf(Items.GLASS_BOTTLE) && fluid == UniversalFluidCellItem.CellFluid.WATER && availableMb >= BOTTLE_MB) return new FillData(BOTTLE_MB, createWaterBottle());
        return null;
    }

    private static UniversalFluidCellItem.CellFluid getFilledBucketFluid(ItemStack stack) {
        if (stack.isOf(Items.WATER_BUCKET)) return UniversalFluidCellItem.CellFluid.WATER;
        if (stack.isOf(Items.LAVA_BUCKET)) return UniversalFluidCellItem.CellFluid.LAVA;
        if (stack.isOf(Items.MILK_BUCKET)) return UniversalFluidCellItem.CellFluid.MILK;
        return UniversalFluidCellItem.CellFluid.EMPTY;
    }

    private static ItemStack createFilledBucket(UniversalFluidCellItem.CellFluid fluid) {
        return switch (fluid) {
            case WATER -> new ItemStack(Items.WATER_BUCKET);
            case LAVA -> new ItemStack(Items.LAVA_BUCKET);
            case MILK -> new ItemStack(Items.MILK_BUCKET);
            default -> ItemStack.EMPTY;
        };
    }

    private static boolean isWaterBottle(ItemStack stack) { return stack.isOf(Items.POTION) && PotionUtil.getPotion(stack) == Potions.WATER; }
    private static ItemStack createWaterBottle() { return PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER); }

    private boolean canAcceptTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY_MB;
        return tankFluid == fluid && tankAmount + amount <= TANK_CAPACITY_MB;
    }

    private void addTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) { tankFluid = fluid; tankAmount = 0; }
        tankAmount = Math.min(TANK_CAPACITY_MB, tankAmount + amount);
        sanitizeTank();
    }

    private void sanitizeTank() {
        if (tankAmount <= 0) { tankAmount = 0; tankFluid = UniversalFluidCellItem.CellFluid.EMPTY; }
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("tankFluid", tankFluid.id);
        nbt.putInt("tankAmount", tankAmount);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        tankFluid = UniversalFluidCellItem.CellFluid.byId(nbt.getString("tankFluid"));
        tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("tankAmount")));
        sanitizeTank();
    }

    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_DRAIN) return canDrainContainer(stack);
        if (slot == SLOT_FILL) return canFillContainer(stack);
        return super.canInsert(slot, stack, dir);
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
        if (!canAcceptTankFluid(fluid, amountMb)) {
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amountMb <= 0) return 0;
            if (tankAmount > 0 && tankFluid != fluid) return 0;
        }
        int accepted = Math.min(amountMb, TANK_CAPACITY_MB - tankAmount);
        if (!simulate && accepted > 0) {
            addTankFluid(fluid, accepted);
            markDirty();
        }
        return accepted;
    }

    @Override
    public int drainForUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amountMb <= 0 || tankFluid != fluid) return 0;
        int drained = Math.min(amountMb, tankAmount);
        if (!simulate && drained > 0) {
            tankAmount -= drained;
            sanitizeTank();
            markDirty();
        }
        return drained;
    }

    @Override
    public UniversalFluidCellItem.CellFluid getPreferredDrainFluidForUpgrade() {
        return tankAmount > 0 ? tankFluid : UniversalFluidCellItem.CellFluid.EMPTY;
    }

    @Override public PropertyDelegate getGuiProps() { return props; }
    public int getTankAmount() { return tankAmount; }
    public int getTankCapacity() { return TANK_CAPACITY_MB; }
    public UniversalFluidCellItem.CellFluid getTankFluid() { return tankFluid; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.fluid_bottler"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new FluidBottlerScreenHandler(syncId, playerInventory, this); }

    @FunctionalInterface private interface TransferAction { void apply(); }
    private record DrainData(UniversalFluidCellItem.CellFluid fluid, int amountMb, ItemStack output) { }
    private record FillData(int amountMb, ItemStack output) { }
}
