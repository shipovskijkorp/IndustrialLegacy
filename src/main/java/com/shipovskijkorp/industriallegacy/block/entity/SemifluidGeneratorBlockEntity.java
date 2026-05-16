package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.SemifluidGeneratorBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.SemifluidGeneratorScreenHandler;
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

import java.util.EnumMap;
import java.util.Map;

/**
 * IC2 Experimental Semifluid Generator.
 *
 * Source truth: TileEntitySemifluidGenerator, IC2 2.8.222-ex112:
 * - TileEntityBaseGenerator(32.0, 1, 32000)
 * - fluid tank capacity: 10000 mB
 * - biomass: 8 EU/mB at 8 EU/t
 * - biogas: 32 EU/mB at 16 EU/t
 * - creosote: 3 EU/mB at 8 EU/t
 */
public class SemifluidGeneratorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_FLUID = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CHARGE = 2;
    public static final int INV_SIZE = 3;

    private static final int[] TOP_SLOTS = new int[] { SLOT_FLUID, SLOT_CHARGE };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_FLUID, SLOT_CHARGE };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT };

    private static final int TIER = 1;
    private static final long CAPACITY = 32_000L;
    private static final int TANK_CAPACITY_MB = 10_000;
    private static final int CELL_MB = 1_000;

    private static final Map<UniversalFluidCellItem.CellFluid, FuelProperty> FUELS = new EnumMap<>(UniversalFluidCellItem.CellFluid.class);

    static {
        FUELS.put(UniversalFluidCellItem.CellFluid.BIOMASS, new FuelProperty(8, 8));
        FUELS.put(UniversalFluidCellItem.CellFluid.BIOGAS, new FuelProperty(32, 16));
        FUELS.put(UniversalFluidCellItem.CellFluid.CREOSOTE, new FuelProperty(3, 8));
    }

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    private long energy = 0L;
    /** Remaining converted fuel, in EU, matching IC2's TileEntityBaseGenerator.fuel field. */
    private int fuel = 0;
    private long production = 32L;
    private UniversalFluidCellItem.CellFluid tankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int tankAmount = 0;

    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override public int size() { return 7; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> fuel;
                case 3 -> tankAmount;
                case 4 -> TANK_CAPACITY_MB;
                case 5 -> tankFluid.ordinal();
                case 6 -> (int) Math.min(Integer.MAX_VALUE, production);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> fuel = Math.max(0, value);
                case 3 -> tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 5 -> tankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                case 6 -> production = Math.max(0L, value);
                default -> { }
            }
            sanitizeTank();
        }
    };

    public SemifluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEMIFLUID_GENERATOR, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, SemifluidGeneratorBlockEntity be) {
        if (world.isClient) return;

        boolean dirty = false;
        dirty |= be.processFluidSlot();

        if (be.needsFuel()) {
            dirty |= be.gainFuel();
        }

        boolean active = be.gainEnergy();
        dirty |= be.chargeItem();
        be.emitToNeighbors();

        if (dirty) be.markDirty();
        if (state.get(SemifluidGeneratorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(SemifluidGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        }
    }

    public static boolean isAcceptedFuelCell(ItemStack stack) {
        return stack.getItem() instanceof UniversalFluidCellItem && FUELS.containsKey(UniversalFluidCellItem.getFluid(stack));
    }

    public static FuelProperty getFuelProperty(UniversalFluidCellItem.CellFluid fluid) {
        return FUELS.get(fluid);
    }

    private boolean processFluidSlot() {
        ItemStack stack = items.get(SLOT_FLUID);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) return false;

        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
        if (!FUELS.containsKey(fluid)) return false;
        if (!canAcceptTankFluid(fluid, CELL_MB)) return false;

        ItemStack emptyCell = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
        if (!canOutputStack(items.get(SLOT_OUTPUT), emptyCell)) return false;

        stack.decrement(1);
        insertOutput(emptyCell);
        addTankFluid(fluid, CELL_MB);
        return true;
    }

    private boolean needsFuel() {
        return fuel < production && getEuFree() >= production;
    }

    private boolean gainFuel() {
        FuelProperty property = FUELS.get(tankFluid);
        if (property == null || tankAmount <= 0) return false;

        int toConsume = property.energyPerMb >= property.energyPerTick
                ? 1
                : (int) Math.ceil((double) property.energyPerTick / (double) property.energyPerMb);
        toConsume = Math.min(toConsume, tankAmount);
        if (toConsume <= 0) return false;

        tankAmount -= toConsume;
        production = property.energyPerTick;
        fuel += toConsume * property.energyPerMb;
        sanitizeTank();
        return true;
    }

    private boolean gainEnergy() {
        if (fuel <= 0 || production <= 0 || getEuFree() <= 0) return false;

        long add = Math.min(Math.min((long) fuel, production), getEuFree());
        if (add <= 0L) return false;

        energy += add;
        fuel -= (int) add;
        return true;
    }

    private boolean chargeItem() {
        ItemStack charge = items.get(SLOT_CHARGE);
        if (charge.isEmpty() || !ElectricItemManager.isElectric(charge) || charge.getCount() != 1) return false;
        if (energy <= 0L) return false;

        long maxMove = Math.min(production, ElectricItemManager.getTransferLimit(charge));
        if (maxMove <= 0L) return false;

        long free = ElectricItemManager.getFree(charge);
        long move = Math.min(maxMove, Math.min(energy, free));
        if (move <= 0L) return false;

        long accepted = ElectricItemManager.charge(charge, move, false);
        if (accepted > 0L) {
            energy -= accepted;
            return true;
        }
        return false;
    }

    private void emitToNeighbors() {
        if (world == null || energy <= 0L) return;

        long packet = Math.min(energy, EuUtil.powerFromTier(TIER));
        if (packet <= 0L) return;

        long remaining = packet;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0L) break;
            long spent = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= spent;
        }
    }

    private boolean canAcceptTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || !FUELS.containsKey(fluid) || amount <= 0) return false;
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

    private long getEuFree() {
        return Math.max(0L, CAPACITY - energy);
    }

    private static boolean canOutputStack(ItemStack existing, ItemStack inserted) {
        if (existing.isEmpty()) return true;
        if (!ItemStack.canCombine(existing, inserted)) return false;
        return existing.getCount() + inserted.getCount() <= existing.getMaxCount();
    }

    private void insertOutput(ItemStack stack) {
        ItemStack existing = items.get(SLOT_OUTPUT);
        if (existing.isEmpty()) items.set(SLOT_OUTPUT, stack.copy());
        else existing.increment(stack.getCount());
    }

    public PropertyDelegate getGuiProperties() { return guiProps; }
    public int getTankAmount() { return tankAmount; }
    public int getTankCapacity() { return TANK_CAPACITY_MB; }
    public UniversalFluidCellItem.CellFluid getTankFluid() { return tankFluid; }
    public long getProduction() { return production; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("fuel", fuel);
        nbt.putLong("production", production);
        nbt.putString("tankFluid", tankFluid.id);
        nbt.putInt("tankAmount", tankAmount);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fuel = Math.max(0, nbt.getInt("fuel"));
        production = Math.max(0L, nbt.contains("production") ? nbt.getLong("production") : 32L);
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
        if (slot == SLOT_CHARGE) return ElectricItemManager.isElectric(stack);
        if (slot == SLOT_FLUID) return isAcceptedFuelCell(stack);
        return false;
    }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.semifluid_generator"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new SemifluidGeneratorScreenHandler(syncId, playerInventory, this); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return TIER; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { return 0L; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0L) return 0L;
        long extracted = Math.min(amount, energy);
        if (!simulate && extracted > 0L) energy -= extracted;
        return extracted;
    }
    @Override public boolean canInsert(Direction from) { return false; }
    @Override public boolean canExtract(Direction to) { return true; }

    public record FuelProperty(int energyPerMb, int energyPerTick) { }
}
