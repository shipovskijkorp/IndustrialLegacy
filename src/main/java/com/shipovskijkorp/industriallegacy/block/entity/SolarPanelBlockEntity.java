package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricSlotHelper;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.SolarPanelScreenHandler;
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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SolarPanelBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public static final int SLOT_CHARGE = 0;
    public static final int INV_SIZE = 1;
    private static final int TIER = 1;
    private static final long CAPACITY = 2L;
    private static final int[] ALL_SLOTS = new int[] { SLOT_CHARGE };
    private static final int TICK_RATE = 128;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy;
    private double fractionalEnergy;
    private float skyLight;
    private int ticker = -1;
    private final double energyMultiplier;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 3; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) energy;
                case 1 -> (int) CAPACITY;
                case 2 -> Math.round(skyLight * 1000.0f);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> skyLight = MathHelper.clamp(value / 1000.0f, 0.0f, 1.0f);
                default -> {}
            }
        }
    };

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL, pos, state);
        this.energyMultiplier = Math.max(0.0, ILConfig.getFloat("balance/energy/generator/solar", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, SolarPanelBlockEntity be) {
        if (world.isClient) return;

        if (be.ticker < 0) {
            be.ticker = world.random.nextInt(TICK_RATE);
            be.updateSunVisibility();
        }
        if (++be.ticker % TICK_RATE == 0) {
            be.updateSunVisibility();
        }

        boolean active = be.gainEnergy();
        be.chargeItem();
        be.emitToNeighbors();

        if (active || be.energy > 0L) {
            be.markDirty();
        }
    }

    private boolean gainEnergy() {
        if (skyLight <= 0.0f || energy >= CAPACITY || energyMultiplier <= 0.0) {
            return false;
        }

        fractionalEnergy += skyLight * energyMultiplier;
        long whole = (long) Math.floor(fractionalEnergy);
        if (whole <= 0L) {
            return true;
        }

        long accepted = Math.min(whole, CAPACITY - energy);
        energy += accepted;
        fractionalEnergy -= accepted;
        if (energy >= CAPACITY) {
            fractionalEnergy = Math.min(fractionalEnergy, 0.999999);
        }
        return true;
    }

    private void updateSunVisibility() {
        if (world == null) {
            skyLight = 0.0f;
            return;
        }
        skyLight = getSkyLight(world, pos.up());
    }

    public static float getSkyLight(World world, BlockPos pos) {
        if (world == null || !world.getDimension().hasSkyLight()) {
            return 0.0f;
        }

        float sunBrightness = MathHelper.clamp((float) Math.cos(world.getSkyAngleRadians(1.0f)) * 2.0f + 0.2f, 0.0f, 1.0f);
        if (!isSandyBiome(world, pos)) {
            sunBrightness *= 1.0f - world.getRainGradient(1.0f) * 5.0f / 16.0f;
            sunBrightness *= 1.0f - world.getThunderGradient(1.0f) * 5.0f / 16.0f;
            sunBrightness = MathHelper.clamp(sunBrightness, 0.0f, 1.0f);
        }

        return world.getLightLevel(LightType.SKY, pos) / 15.0f * sunBrightness;
    }

    private static boolean isSandyBiome(World world, BlockPos pos) {
        var biome = world.getBiome(pos);
        var biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getId(biome.value());
        if (biomeId == null) return false;
        String path = biomeId.getPath();
        return path.contains("desert") || path.contains("badlands") || path.contains("beach");
    }

    private void chargeItem() {
        // IL TileEntityBaseGenerator uses InvSlotCharge tier 1.
        ItemStack charge = items.get(SLOT_CHARGE);
        long accepted = ElectricSlotHelper.chargeFromStorage(charge, energy, 1, false);
        if (accepted > 0L) {
            energy -= accepted;
            markDirty();
        }
    }

    private void emitToNeighbors() {
        if (world == null || energy <= 0L) return;
        long packet = Math.min(energy, EuUtil.powerFromTier(TIER));
        long remaining = packet;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0L) break;
            long spent = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= spent;
        }
    }

    public PropertyDelegate getGuiProperties() { return props; }
    public float getSkyLightForGui() { return skyLight; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putDouble("fractionalEnergy", fractionalEnergy);
        nbt.putFloat("skyLight", skyLight);
        nbt.putInt("ticker", ticker);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        fractionalEnergy = MathHelper.clamp(nbt.getDouble("fractionalEnergy"), 0.0, 0.999999);
        skyLight = MathHelper.clamp(nbt.getFloat("skyLight"), 0.0f, 1.0f);
        ticker = nbt.contains("ticker") ? nbt.getInt("ticker") : -1;
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); markDirty(); return r; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return ALL_SLOTS; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_CHARGE && ElectricSlotHelper.canCharge(stack, 1); }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_CHARGE; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.solar_panel"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new SolarPanelScreenHandler(syncId, playerInventory, this); }
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
