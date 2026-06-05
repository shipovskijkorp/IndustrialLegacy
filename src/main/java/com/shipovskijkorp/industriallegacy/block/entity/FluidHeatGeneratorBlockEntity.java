package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.FluidHeatGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.heat.IHeatSource;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.FluidHeatGeneratorScreenHandler;
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

public class FluidHeatGeneratorBlockEntity extends BlockEntity implements SidedInventory, IHeatSource, ExtendedScreenHandlerFactory {
    public static final int SLOT_FLUID = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int INV_SIZE = 2;
    private static final int[] TOP_SIDE = new int[]{SLOT_FLUID};
    private static final int[] BOTTOM = new int[]{SLOT_OUTPUT};
    private static final int TANK_CAPACITY_MB = 10_000;
    private static final int CELL_MB = 1_000;
    private static final Map<UniversalFluidCellItem.CellFluid, BurnProperty> FUELS = new EnumMap<>(UniversalFluidCellItem.CellFluid.class);

    static {
        FUELS.put(UniversalFluidCellItem.CellFluid.BIOMASS, new BurnProperty(20, Math.round(16.0f * ILConfig.getFloat("balance/energy/heatgenerator/semiFluidBiomass", 1.0f))));
        FUELS.put(UniversalFluidCellItem.CellFluid.BIOGAS, new BurnProperty(10, Math.round(32.0f * ILConfig.getFloat("balance/energy/heatgenerator/semiFluidBiogas", 1.0f))));
        FUELS.put(UniversalFluidCellItem.CellFluid.CREOSOTE, new BurnProperty(10, 6));
    }

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private UniversalFluidCellItem.CellFluid tankFluid = UniversalFluidCellItem.CellFluid.EMPTY;
    private int tankAmount;
    private short ticker;
    private int burnAmount;
    private int production;
    private int heatBuffer;
    private int transmitHeat;
    private int maxHeatEmitPerTick;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return FluidHeatGeneratorScreenHandler.PROP_COUNT; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> transmitHeat; case 1 -> maxHeatEmitPerTick; case 2 -> heatBuffer; case 3 -> tankAmount; case 4 -> TANK_CAPACITY_MB; case 5 -> tankFluid.ordinal(); default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> transmitHeat = Math.max(0, value);
                case 1 -> maxHeatEmitPerTick = Math.max(0, value);
                case 2 -> heatBuffer = Math.max(0, value);
                case 3 -> tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 5 -> tankFluid = UniversalFluidCellItem.CellFluid.values()[Math.max(0, Math.min(UniversalFluidCellItem.CellFluid.values().length - 1, value))];
                default -> { }
            }
            sanitizeTank();
        }
    };

    public FluidHeatGeneratorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FLUID_HEAT_GENERATOR, pos, state); }

    public static void tick(World world, BlockPos pos, BlockState state, FluidHeatGeneratorBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.processFluidSlot();
        int amount = be.getMaxHeatEmittedPerTick() - be.heatBuffer;
        if (amount > 0) {
            int filled = be.fillHeatBuffer(amount);
            if (filled != 0) { be.heatBuffer += filled; dirty = true; }
        }
        be.maxHeatEmitPerTick = be.getMaxHeatEmittedPerTick();
        boolean active = be.isConverting();
        if (state.get(FluidHeatGeneratorBlock.LIT) != active) world.setBlockState(pos, state.with(FluidHeatGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        if (dirty || active) be.markDirty();
    }

    public static boolean isAcceptedFuelCell(ItemStack stack) { return stack.getItem() instanceof UniversalFluidCellItem && FUELS.containsKey(UniversalFluidCellItem.getFluid(stack)); }
    public boolean isConverting() { return tankAmount > 0 && heatBuffer < getMaxHeatEmittedPerTick(); }
    protected int fillHeatBuffer(int maxAmount) {
        if (isConverting()) {
            if (ticker >= 19) { drainTank(burnAmount); ticker = 0; } else ticker++;
            return production;
        }
        return 0;
    }
    public int getMaxHeatEmittedPerTick() { return calcHeatProduction(); }
    private int calcHeatProduction() { BurnProperty p = FUELS.get(tankFluid); production = p == null ? 0 : p.heat; return production; }
    private void calcBurnAmount() { BurnProperty p = FUELS.get(tankFluid); burnAmount = p == null ? 0 : p.amount; }

    private boolean processFluidSlot() {
        ItemStack stack = items.get(SLOT_FLUID);
        if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) return false;
        UniversalFluidCellItem.CellFluid fluid = UniversalFluidCellItem.getFluid(stack);
        if (!FUELS.containsKey(fluid) || !canAcceptTankFluid(fluid, CELL_MB)) return false;
        ItemStack empty = UniversalFluidCellItem.createStack(UniversalFluidCellItem.CellFluid.EMPTY);
        if (!canOutputStack(items.get(SLOT_OUTPUT), empty)) return false;
        stack.decrement(1);
        insertOutput(empty);
        addTankFluid(fluid, CELL_MB);
        calcBurnAmount();
        return true;
    }

    private boolean canAcceptTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) { if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || !FUELS.containsKey(fluid)) return false; if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) return amount <= TANK_CAPACITY_MB; return tankFluid == fluid && tankAmount + amount <= TANK_CAPACITY_MB; }
    private void addTankFluid(UniversalFluidCellItem.CellFluid fluid, int amount) { if (tankAmount <= 0 || tankFluid == UniversalFluidCellItem.CellFluid.EMPTY) { tankFluid = fluid; tankAmount = 0; } tankAmount = Math.min(TANK_CAPACITY_MB, tankAmount + amount); sanitizeTank(); }
    private void drainTank(int amount) { tankAmount = Math.max(0, tankAmount - Math.max(0, amount)); sanitizeTank(); }
    private void sanitizeTank() { if (tankAmount <= 0) { tankAmount = 0; tankFluid = UniversalFluidCellItem.CellFluid.EMPTY; ticker = 0; burnAmount = 0; production = 0; } else { calcBurnAmount(); calcHeatProduction(); } }

    @Override public int getConnectionBandwidth(Direction side) { return side == getFacing() ? getMaxHeatEmittedPerTick() : 0; }
    @Override public int drawHeat(Direction side, int request, boolean simulate) { if (side != getFacing() || request <= 0) return 0; int drawn = Math.min(request, heatBuffer); if (!simulate) { heatBuffer -= drawn; transmitHeat = drawn; markDirty(); } return drawn; }
    public Direction getFacing() { BlockState state = getCachedState(); return state.contains(FluidHeatGeneratorBlock.FACING) ? state.get(FluidHeatGeneratorBlock.FACING) : Direction.NORTH; }
    public PropertyDelegate getGuiProperties() { return props; }

    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); Inventories.writeNbt(nbt, items); nbt.putString("tankFluid", tankFluid.name()); nbt.putInt("tankAmount", tankAmount); nbt.putInt("HeatBuffer", heatBuffer); nbt.putShort("ticker", ticker); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); Inventories.readNbt(nbt, items); tankFluid = safeFluid(nbt.getString("tankFluid")); tankAmount = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("tankAmount"))); heatBuffer = Math.max(0, nbt.getInt("HeatBuffer")); ticker = nbt.getShort("ticker"); sanitizeTank(); }
    private UniversalFluidCellItem.CellFluid safeFluid(String name) { try { return UniversalFluidCellItem.CellFluid.valueOf(name); } catch (Exception e) { return UniversalFluidCellItem.CellFluid.EMPTY; } }

    private static boolean canOutputStack(ItemStack existing, ItemStack inserted) { if (existing.isEmpty()) return true; return ItemStack.canCombine(existing, inserted) && existing.getCount() + inserted.getCount() <= existing.getMaxCount(); }
    private void insertOutput(ItemStack stack) { ItemStack ex = items.get(SLOT_OUTPUT); if (ex.isEmpty()) items.set(SLOT_OUTPUT, stack.copy()); else ex.increment(stack.getCount()); }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); markDirty(); return r; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack()); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64.0; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return side == Direction.DOWN ? BOTTOM : TOP_SIDE; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_FLUID && isAcceptedFuelCell(stack); }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return slot == SLOT_FLUID && isAcceptedFuelCell(stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }
    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.fluid_heat_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new FluidHeatGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    public static final class BurnProperty {
        public final int amount;
        public final int heat;
        public BurnProperty(int amount, int heat) {
            this.amount = amount;
            this.heat = heat;
        }
    }
}
