package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.GeoGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.GeoGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
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

public class GeoGeneratorBlockEntity extends net.minecraft.block.entity.BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_FLUID = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CHARGE = 2;
    public static final int INV_SIZE = 3;

    private static final int[] TOP_SLOTS = new int[]{SLOT_FLUID, SLOT_CHARGE};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_FLUID, SLOT_CHARGE};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    private static final int TIER = 1;
    private static final long CAPACITY = 2400L;
    private static final int TANK_CAPACITY_MB = 8000;
    private static final int FLUID_PER_TICK_MB = 2;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    private long energy = 0L;
    private int fuel = 0;
    private int lavaMb = 0;
    private final long production;

    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override
        public int size() {
            return 5;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> fuel;
                case 3 -> lavaMb;
                case 4 -> TANK_CAPACITY_MB;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, (long) value));
                case 2 -> fuel = Math.max(0, value);
                case 3 -> lavaMb = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                default -> {}
            }
        }
    };

    public GeoGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEO_GENERATOR, pos, state);
        float factor = ILConfig.getFloat("balance/energy/generator/geothermal", 1.0f);
        this.production = Math.max(0L, Math.round(20.0f * factor));
    }

    public static void tick(World world, BlockPos pos, BlockState state, GeoGeneratorBlockEntity be) {
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
        if (state.get(GeoGeneratorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(GeoGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        }
    }

    private boolean processFluidSlot() {
        ItemStack stack = items.get(SLOT_FLUID);
        if (stack.isEmpty()) return false;
        if (TANK_CAPACITY_MB - lavaMb < 1000) return false;

        ItemStack out = items.get(SLOT_OUTPUT);

        if (stack.isOf(Items.LAVA_BUCKET)) {
            if (!canOutputStack(out, new ItemStack(Items.BUCKET))) return false;
            stack.decrement(1);
            insertOutput(new ItemStack(Items.BUCKET));
            lavaMb += 1000;
            return true;
        }

        if (stack.getItem() instanceof UniversalFluidCellItem
                && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.LAVA) {
            ItemStack emptyCell = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
            if (!canOutputStack(out, emptyCell)) return false;
            stack.decrement(1);
            insertOutput(emptyCell);
            lavaMb += 1000;
            return true;
        }

        return false;
    }

    private boolean needsFuel() {
        return fuel <= 0 && getEuFree() >= production;
    }

    private boolean gainFuel() {
        if (lavaMb < FLUID_PER_TICK_MB) return false;
        lavaMb -= FLUID_PER_TICK_MB;
        fuel++;
        return true;
    }

    private boolean gainEnergy() {
        if (fuel > 0 && production > 0 && getEuFree() >= production) {
            energy += production;
            fuel--;
            return true;
        }
        return false;
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
        if (existing.isEmpty()) {
            items.set(SLOT_OUTPUT, stack.copy());
        } else {
            existing.increment(stack.getCount());
        }
    }

    public PropertyDelegate getGuiProperties() {
        return guiProps;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("fuel", fuel);
        nbt.putInt("lavaMb", lavaMb);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fuel = Math.max(0, nbt.getInt("fuel"));
        lavaMb = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("lavaMb")));
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
        if (world == null) return false;
        if (world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        if (side == Direction.UP) return TOP_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_CHARGE) return ElectricItemManager.isElectric(stack);
        if (slot == SLOT_FLUID) {
            if (stack.isOf(Items.LAVA_BUCKET)) return true;
            return stack.getItem() instanceof UniversalFluidCellItem
                    && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.LAVA;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.geo_generator");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new GeoGeneratorScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public long getEuStored() {
        return energy;
    }

    @Override
    public long getEuCapacity() {
        return CAPACITY;
    }

    @Override
    public int getSinkTier() {
        return TIER;
    }

    @Override
    public int getSourceTier() {
        return TIER;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L) return 0L;
        long accepted = Math.min(amount, CAPACITY - energy);
        if (!simulate && accepted > 0L) energy += accepted;
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0L) return 0L;
        long extracted = Math.min(amount, energy);
        if (!simulate && extracted > 0L) energy -= extracted;
        return extracted;
    }

    @Override
    public boolean canInsert(Direction from) {
        return false;
    }

    @Override
    public boolean canExtract(Direction to) {
        return true;
    }
}
