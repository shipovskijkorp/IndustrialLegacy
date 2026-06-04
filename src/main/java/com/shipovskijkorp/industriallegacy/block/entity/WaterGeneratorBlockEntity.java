package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.RotorGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricSlotHelper;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.WaterGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WaterGeneratorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory, LegacyRotorProvider {
    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_FUEL = 1;
    public static final int INV_SIZE = 2;
    private static final int[] TOP_SLOTS = new int[]{SLOT_FUEL};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_CHARGE, SLOT_FUEL};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_CHARGE};
    private static final int TIER = 1;
    private static final long CAPACITY = 4L;
    private static final int MAX_WATER = 2000;
    private static final int TICK_RATE = 128;
    private static final int ROTOR_DIAMETER = 2;
    private static final Identifier ROTOR_TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/item/rotor/iron_rotor_model.png");

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy;
    private double fractionalEnergy;
    private int fuel;
    private double production = 2.0D;
    private int water;
    private int microStorage;
    private int ticker = -1;
    private float angle;
    private long lastAngleCheck;
    private final double energyMultiplier;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return WaterGeneratorScreenHandler.PROP_COUNT; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> fuel;
                case 3 -> MAX_WATER;
                case 4 -> water;
                case 5 -> microStorage;
                case 6 -> (int) Math.round(production * 1000.0D);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> fuel = Math.max(0, Math.min(MAX_WATER, value));
                case 4 -> water = Math.max(0, value);
                case 5 -> microStorage = Math.max(0, value);
                case 6 -> production = Math.max(0.0D, value / 1000.0D);
                default -> {}
            }
        }
    };

    public WaterGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_GENERATOR, pos, state);
        this.energyMultiplier = Math.max(0.0D, ILConfig.getFloat("balance/energy/generator/water", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, WaterGeneratorBlockEntity be) {
        if (world.isClient) return;
        if (be.ticker < 0) {
            be.ticker = world.random.nextInt(TICK_RATE);
            be.updateWaterCount();
        }
        if (be.needsFuel()) {
            be.gainFuel();
        }
        boolean active = be.gainEnergy();
        be.chargeItem();
        be.emitToNeighbors();
        if (state.get(RotorGeneratorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(RotorGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        }
        be.markDirty();
    }

    private boolean gainFuel() {
        if (fuel + 500 > MAX_WATER) return false;
        ItemStack stack = items.get(SLOT_FUEL);
        if (!stack.isEmpty()) {
            if (stack.isOf(Items.WATER_BUCKET)) {
                consumeFuelContainer(new ItemStack(Items.BUCKET));
                fuel += 500;
                production = 1.0D;
                return true;
            }
            if (stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER) {
                consumeFuelContainer(UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY));
                fuel += 500;
                production = 2.0D;
                return true;
            }
            return false;
        }

        if (fuel <= 0) {
            flowPower();
            production = microStorage / 100;
            microStorage = (int) ((double) microStorage - production * 100.0D);
            if (production > 0.0D) {
                ++fuel;
                return true;
            }
            return false;
        }
        return false;
    }

    private void consumeFuelContainer(ItemStack container) {
        ItemStack stack = items.get(SLOT_FUEL);
        stack.decrement(1);
        if (stack.isEmpty()) {
            items.set(SLOT_FUEL, container);
        } else if (world != null && !container.isEmpty()) {
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, container));
        }
    }

    private boolean gainEnergy() {
        if (fuel > 0 && production > 0.0D) {
            fractionalEnergy += production;
            long whole = (long) Math.floor(fractionalEnergy);
            if (whole > 0L && energy < CAPACITY) {
                long accepted = Math.min(whole, CAPACITY - energy);
                energy += accepted;
                fractionalEnergy -= accepted;
                if (energy >= CAPACITY) fractionalEnergy = Math.min(fractionalEnergy, 0.999999D);
            }
            --fuel;
            return true;
        }
        return false;
    }

    public boolean isConverting() {
        return fuel > 0;
    }

    private boolean needsFuel() {
        return fuel <= MAX_WATER;
    }

    private void flowPower() {
        if (++ticker % TICK_RATE == 0) updateWaterCount();
        water = (int) Math.round((double) water * energyMultiplier);
        if (water > 0) microStorage += water;
    }

    private void updateWaterCount() {
        if (world == null) return;
        int count = 0;
        for (int x = -1; x < 2; ++x) {
            for (int y = -1; y < 2; ++y) {
                for (int z = -1; z < 2; ++z) {
                    BlockState state = world.getBlockState(pos.add(x, y, z));
                    if (state.isOf(Blocks.WATER) || !world.getFluidState(pos.add(x, y, z)).isEmpty() && world.getFluidState(pos.add(x, y, z)).isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                        ++count;
                    }
                }
            }
        }
        water = count;
    }

    private void chargeItem() {
        ItemStack charge = items.get(SLOT_CHARGE);
        long accepted = ElectricSlotHelper.chargeFromStorage(charge, energy, TIER, false);
        if (accepted > 0L) energy -= accepted;
    }

    private void emitToNeighbors() {
        if (world == null || energy <= 0L) return;
        long remaining = Math.min(energy, EuUtil.powerFromTier(TIER));
        for (Direction dir : Direction.values()) {
            if (remaining <= 0L) break;
            long spent = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= spent;
        }
    }

    public PropertyDelegate getGuiProperties() { return props; }
    public int getFuel() { return fuel; }
    public int getMaxWater() { return MAX_WATER; }
    public int getWater() { return water; }
    public int getMicroStorage() { return microStorage; }
    public double getProduction() { return production; }

    public static boolean isValidFuelStack(ItemStack stack) {
        return stack.isOf(Items.WATER_BUCKET) || stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.WATER;
    }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putDouble("fractionalEnergy", fractionalEnergy);
        nbt.putInt("fuel", fuel);
        nbt.putDouble("production", production);
        nbt.putInt("water", water);
        nbt.putInt("microStorage", microStorage);
        nbt.putInt("ticker", ticker);
        nbt.putFloat("angle", angle);
        nbt.putLong("lastAngleCheck", lastAngleCheck);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fractionalEnergy = MathHelper.clamp(nbt.getDouble("fractionalEnergy"), 0.0D, 0.999999D);
        fuel = Math.max(0, Math.min(MAX_WATER, nbt.getInt("fuel")));
        production = nbt.contains("production") ? Math.max(0.0D, nbt.getDouble("production")) : 2.0D;
        water = Math.max(0, nbt.getInt("water"));
        microStorage = Math.max(0, nbt.getInt("microStorage"));
        ticker = nbt.contains("ticker") ? nbt.getInt("ticker") : -1;
        angle = nbt.getFloat("angle");
        lastAngleCheck = nbt.getLong("lastAngleCheck");
    }

    @Override public int getRotorDiameter() { return ROTOR_DIAMETER; }
    @Override public Direction getFacing() { return getCachedState().contains(RotorGeneratorBlock.FACING) ? getCachedState().get(RotorGeneratorBlock.FACING) : Direction.NORTH; }
    @Override public Identifier getRotorRenderTexture() { return ROTOR_TEXTURE; }
    @Override public float getAngle() {
        long now = System.currentTimeMillis();
        if (water > 0 || fuel > 0) {
            angle += (float) (now - lastAngleCheck) * 0.4F * (fuel > 0 ? 1.0F : (float) water / 25.0F);
            angle %= 360.0F;
        }
        lastAngleCheck = now;
        return angle;
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); markDirty(); return r; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return side == Direction.UP ? TOP_SLOTS : side == Direction.DOWN ? BOTTOM_SLOTS : SIDE_SLOTS; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_CHARGE ? ElectricSlotHelper.canCharge(stack, TIER) : slot == SLOT_FUEL && isValidFuelStack(stack); }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_CHARGE || slot == SLOT_FUEL && (stack.isOf(Items.BUCKET) || stack.getItem() instanceof UniversalFluidCellItem && UniversalFluidCellItem.getFluid(stack) == UniversalFluidCellItem.CellFluid.EMPTY); }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.water_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new WaterGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return TIER; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { return 0L; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { long ex = Math.min(Math.max(0L, amount), energy); if (!simulate) energy -= ex; return ex; }
    @Override public boolean canInsert(Direction from) { return false; }
    @Override public boolean canExtract(Direction to) { return true; }
}
