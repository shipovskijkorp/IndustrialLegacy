package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.FluidBottlerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.item.armor.BiogasJetpackItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.FluidBottlerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
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
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * IC2 Experimental Fluid Bottler / Bottling Plant.
 *
 * Source truth: TileEntityFluidBottler, IC2 2.8.222-ex112:
 * - TileEntityStandardMachine(2, 100, 1)
 * - fluid tank capacity: 8000 mB
 * - uses generic empty/fill fluid container recipe managers.
 *
 * Construction foam cases are intentionally left out until the CF tool path exists.
 */
public class FluidBottlerBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
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

    public FluidBottlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_BOTTLER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, FluidBottlerBlockEntity be) {
        if (world.isClient) return;
        boolean active = be.processTick();
        if (state.get(FluidBottlerBlock.LIT) != active) {
            world.setBlockState(pos, state.with(FluidBottlerBlock.LIT, active), Block.NOTIFY_ALL);
        }
        if (active) be.markDirty();
    }

    public static boolean canDrainContainer(ItemStack stack) {
        return getDrainData(stack) != null;
    }

    public static boolean canFillContainer(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof UniversalFluidCellItem) {
            return UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.EMPTY;
        }
        if (stack.getItem() instanceof BiogasJetpackItem) {
            return BiogasJetpackItem.getFuel(stack) < BiogasJetpackItem.CAPACITY_MB;
        }
        return stack.isOf(Items.BUCKET) || stack.isOf(Items.GLASS_BOTTLE);
    }

    private boolean processTick() {
        TransferAction action = findAction();
        if (action == null) {
            progress = 0;
            maxProgress = BASE_TICKS;
            return false;
        }
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = BASE_TICKS;
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
        if (drain != null) return drain;
        return findFillAction();
    }

    private TransferAction findDrainAction() {
        ItemStack stack = items.get(SLOT_DRAIN);
        DrainData data = getDrainData(stack);
        if (data == null) return null;
        if (!canAcceptTankFluid(data.fluid(), data.amountMb())) return null;
        if (!canOutput(data.output())) return null;

        return () -> {
            items.get(SLOT_DRAIN).decrement(1);
            insertOutput(data.output());
            addTankFluid(data.fluid(), data.amountMb());
        };
    }

    private TransferAction findFillAction() {
        ItemStack stack = items.get(SLOT_FILL);
        if (stack.isEmpty() || tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return null;

        FillData data = getFillData(stack, tankFluid, tankAmount);
        if (data == null) return null;
        if (!canOutput(data.output())) return null;

        return () -> {
            items.get(SLOT_FILL).decrement(1);
            insertOutput(data.output());
            tankAmount -= data.amountMb();
            sanitizeTank();
        };
    }

    private static @Nullable DrainData getDrainData(ItemStack stack) {
        if (stack.isEmpty()) return null;

        if (stack.getItem() instanceof UniversalFluidCellItem) {
            UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
            if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
                return new DrainData(UniversalFluidCellItem.CellFluid.AIR, CELL_MB, UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
            }
            return new DrainData(fluid, CELL_MB, UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
        }

        if (stack.getItem() instanceof BiogasJetpackItem) {
            int fuel = BiogasJetpackItem.getFuel(stack);
            if (fuel <= 0) return null;
            ItemStack out = stack.copy();
            out.setCount(1);
            BiogasJetpackItem.setFuel(out, 0);
            return new DrainData(UniversalFluidCellItem.CellFluid.BIOGAS, fuel, out);
        }

        UniversalFluidCellItem.CellFluid bucketFluid = getFilledBucketFluid(stack);
        if (bucketFluid != UniversalFluidCellItem.CellFluid.EMPTY) {
            return new DrainData(bucketFluid, BUCKET_MB, new ItemStack(Items.BUCKET));
        }

        if (isWaterBottle(stack)) {
            return new DrainData(UniversalFluidCellItem.CellFluid.WATER, BOTTLE_MB, new ItemStack(Items.GLASS_BOTTLE));
        }

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
            ItemStack out = stack.copy();
            out.setCount(1);
            BiogasJetpackItem.setFuel(out, current + fill);
            return new FillData(fill, out);
        }

        if (stack.isOf(Items.BUCKET) && availableMb >= BUCKET_MB) {
            ItemStack bucket = createFilledBucket(fluid);
            if (!bucket.isEmpty()) return new FillData(BUCKET_MB, bucket);
        }

        if (stack.isOf(Items.GLASS_BOTTLE) && fluid == UniversalFluidCellItem.CellFluid.WATER && availableMb >= BOTTLE_MB) {
            return new FillData(BOTTLE_MB, createWaterBottle());
        }

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

    private static boolean isWaterBottle(ItemStack stack) {
        return stack.isOf(Items.POTION) && PotionUtil.getPotion(stack) == Potions.WATER;
    }

    private static ItemStack createWaterBottle() {
        return PotionUtil.setPotion(new ItemStack(Items.POTION), Potions.WATER);
    }

    private boolean canAcceptTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0) return false;
        if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY_MB;
        return tankFluid == fluid && tankAmount + amount <= TANK_CAPACITY_MB;
    }

    private void addTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
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
        if (slot == SLOT_DRAIN) return canDrainContainer(stack);
        if (slot == SLOT_FILL) return canFillContainer(stack);
        if (slot == SLOT_DISCHARGE) return ElectricItemManager.isElectric(stack);
        return slot >= SLOT_UPGRADE_0 && slot < SLOT_UPGRADE_0 + UPGRADE_SLOTS;
    }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }

    public PropertyDelegate getGuiProps() { return props; }
    public int getTankAmount() { return tankAmount; }
    public int getTankCapacity() { return TANK_CAPACITY_MB; }
    public UniversalFluidCellItem.CellFluid getTankFluid() { return tankFluid; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.fluid_bottler"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new FluidBottlerScreenHandler(syncId, playerInventory, this); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, CAPACITY - energy);
        if (!simulate && accepted > 0L) energy += accepted;
        return accepted;
    }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { return 0L; }

    @FunctionalInterface
    private interface TransferAction {
        void apply();
    }

    private record DrainData(UniversalFluidCellItem.CellFluid fluid, int amountMb, ItemStack output) {}
    private record FillData(int amountMb, ItemStack output) {}
}
