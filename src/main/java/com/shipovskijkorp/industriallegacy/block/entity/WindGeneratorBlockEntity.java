package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.RotorGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricSlotHelper;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.WindGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.world.WindSimulation;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WindGeneratorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory, LegacyRotorProvider {
    public static final int SLOT_CHARGE = 0;
    public static final int INV_SIZE = 1;
    private static final int[] ALL_SLOTS = new int[]{SLOT_CHARGE};
    private static final int TIER = 1;
    private static final long CAPACITY = 32L;
    private static final int TICK_RATE = 128;
    private static final int OBSTRUCTION_RECHECK_RATE = 1024;
    private static final int ROTOR_DIAMETER = 2;
    private static final Identifier ROTOR_TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/item/rotor/iron_rotor_model.png");

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy;
    private double fractionalEnergy;
    private double production;
    private int ticker = -1;
    private int obstructedBlockCount;
    private double overheatRatio;
    private float angle;
    private long lastAngleCheck;
    private final double windToEnergy;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return WindGeneratorScreenHandler.PROP_COUNT; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> (int) Math.round(production * 1000.0D);
                case 3 -> obstructedBlockCount;
                case 4 -> (int) Math.round(overheatRatio * 1000.0D);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> production = Math.max(0.0D, value / 1000.0D);
                case 3 -> obstructedBlockCount = value;
                case 4 -> overheatRatio = Math.max(0.0D, value / 1000.0D);
                default -> {}
            }
        }
    };

    public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_GENERATOR, pos, state);
        this.windToEnergy = 0.1D * Math.max(0.0D, ILConfig.getFloat("balance/energy/generator/wind", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, WindGeneratorBlockEntity be) {
        if (world.isClient) return;
        if (be.ticker < 0) {
            be.ticker = world.random.nextInt(TICK_RATE);
            be.updateObstructedBlockCount();
        }
        boolean active = be.gainEnergy();
        be.chargeItem();
        be.emitToNeighbors();
        if (state.get(RotorGeneratorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(RotorGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        }
        be.markDirty();
    }

    private boolean gainEnergy() {
        if (++ticker % TICK_RATE == 0) {
            if (ticker % OBSTRUCTION_RECHECK_RATE == 0) updateObstructedBlockCount();
            double previousProduction = production;
            production = 0.0D;
            overheatRatio = 0.0D;
            if (windToEnergy <= 0.0D || !(world instanceof ServerWorld serverWorld)) return false;

            WindSimulation windSim = WindSimulation.get(serverWorld);
            windSim.tick(serverWorld);
            double wind = windSim.getWindAt(serverWorld, pos.getY()) * (1.0D - (double) obstructedBlockCount / 567.0D);
            if (wind <= 0.0D) return false;
            double windRatio = wind / windSim.getMaxWind();
            overheatRatio = Math.max(0.0D, (windRatio - 0.5D) / 0.5D);

            if (wind > windSim.getMaxWind() * 0.5D && (double) world.random.nextInt(5000) <= previousProduction - 5.0D) {
                breakAndDropIron();
                return false;
            }
            production = wind * windToEnergy;
        }

        if (production <= 0.0D || energy >= CAPACITY) return false;
        fractionalEnergy += production;
        long whole = (long) Math.floor(fractionalEnergy);
        if (whole <= 0L) return production > 0.0D;
        long accepted = Math.min(whole, CAPACITY - energy);
        energy += accepted;
        fractionalEnergy -= accepted;
        if (energy >= CAPACITY) fractionalEnergy = Math.min(fractionalEnergy, 0.999999D);
        return production > 0.0D;
    }

    private void breakAndDropIron() {
        if (world == null) return;
        world.breakBlock(pos, true);
        int drops = world.random.nextInt(5);
        for (int i = drops; i > 0; --i) {
            world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, new ItemStack(Items.IRON_INGOT)));
        }
    }

    private void updateObstructedBlockCount() {
        if (world == null) return;
        int count = -1;
        for (int x = -4; x < 5; ++x) {
            for (int y = -2; y < 5; ++y) {
                for (int z = -4; z < 5; ++z) {
                    if (!world.isAir(pos.add(x, y, z))) ++count;
                }
            }
        }
        obstructedBlockCount = count;
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
    public double getProduction() { return production; }
    public int getObstructions() { return obstructedBlockCount; }
    public double getOverheatRatio() { return overheatRatio; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putDouble("fractionalEnergy", fractionalEnergy);
        nbt.putDouble("production", production);
        nbt.putInt("ticker", ticker);
        nbt.putInt("obstructedBlockCount", obstructedBlockCount);
        nbt.putDouble("overheatRatio", overheatRatio);
        nbt.putFloat("angle", angle);
        nbt.putLong("lastAngleCheck", lastAngleCheck);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fractionalEnergy = MathHelper.clamp(nbt.getDouble("fractionalEnergy"), 0.0D, 0.999999D);
        production = Math.max(0.0D, nbt.getDouble("production"));
        ticker = nbt.contains("ticker") ? nbt.getInt("ticker") : -1;
        obstructedBlockCount = nbt.getInt("obstructedBlockCount");
        overheatRatio = Math.max(0.0D, nbt.getDouble("overheatRatio"));
        angle = nbt.getFloat("angle");
        lastAngleCheck = nbt.getLong("lastAngleCheck");
    }

    @Override public int getRotorDiameter() { return ROTOR_DIAMETER; }
    @Override public Direction getFacing() { return getCachedState().contains(RotorGeneratorBlock.FACING) ? getCachedState().get(RotorGeneratorBlock.FACING) : Direction.NORTH; }
    @Override public Identifier getRotorRenderTexture() { return ROTOR_TEXTURE; }
    @Override public float getAngle() {
        long now = System.currentTimeMillis();
        if (production > 0.0D) {
            angle += (float) (now - lastAngleCheck) * 0.4F;
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
    @Override public int[] getAvailableSlots(Direction side) { return ALL_SLOTS; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_CHARGE && ElectricSlotHelper.canCharge(stack, TIER); }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_CHARGE; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.wind_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new WindGeneratorScreenHandler(syncId, playerInventory, this); }
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
