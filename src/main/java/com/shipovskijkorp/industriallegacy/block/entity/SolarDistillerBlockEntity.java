package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.SolarDistillerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.FluidContainerUtil;
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
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * IC2 Experimental Solar Distiller.
 *
 * Source truth: TileEntitySolarDestiller, IC2 2.8.222-ex112:
 * - input water tank 10000 mB;
 * - output distilled water tank 10000 mB;
 * - produces 1 mB distilled water per tickrate cycle when skylight > 0.5;
 * - tickrate: hot biome 36, normal 72, cold 144.
 */
public class SolarDistillerBlockEntity extends BlockEntity implements SidedInventory, ExtendedScreenHandlerFactory {
    public static final int SLOT_WATER_INPUT = 0;
    public static final int SLOT_DISTILLED_INPUT = 1;
    public static final int SLOT_WATER_OUTPUT = 2;
    public static final int SLOT_DISTILLED_OUTPUT = 3;
    public static final int SLOT_UPGRADE_0 = 4;
    public static final int UPGRADE_SLOTS = 2;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_WATER_INPUT };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_WATER_INPUT, SLOT_DISTILLED_INPUT, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_WATER_OUTPUT, SLOT_DISTILLED_OUTPUT };

    private static final int TANK_CAPACITY_MB = 10_000;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private int inputWater = 0;
    private int distilledWater = 0;
    private int tickrate = 72;
    private int updateTicker = -1;
    private float skyLight = 0.0f;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 7; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> inputWater;
                case 1 -> TANK_CAPACITY_MB;
                case 2 -> distilledWater;
                case 3 -> TANK_CAPACITY_MB;
                case 4 -> Math.round(skyLight * 1000.0f);
                case 5 -> tickrate;
                case 6 -> Math.max(0, updateTicker);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> inputWater = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 2 -> distilledWater = Math.max(0, Math.min(TANK_CAPACITY_MB, value));
                case 4 -> skyLight = MathHelper.clamp(value / 1000.0f, 0.0f, 1.0f);
                case 5 -> tickrate = Math.max(1, value);
                case 6 -> updateTicker = Math.max(0, value);
                default -> { }
            }
        }
    };

    public SolarDistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_DISTILLER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, SolarDistillerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = false;

        if (be.updateTicker < 0) {
            be.tickrate = be.getTickRate();
            be.updateTicker = world.random.nextInt(Math.max(1, be.tickrate));
            be.updateSunVisibility();
        }

        dirty |= be.processWaterInput();
        dirty |= be.processDistilledOutput();

        if (++be.updateTicker >= be.tickrate) {
            be.tickrate = be.getTickRate();
            be.updateSunVisibility();
            if (be.canWork()) {
                be.inputWater--;
                be.distilledWater++;
                dirty = true;
            }
            be.updateTicker = 0;
        }

        if (dirty) be.markDirty();
    }

    public static boolean canInsertWaterContainer(ItemStack stack) {
        return FluidContainerUtil.isWaterContainer(stack);
    }

    public static boolean canInsertDistilledContainer(ItemStack stack) {
        return FluidContainerUtil.isEmptyContainerFor(UniversalFluidCellItem.CellFluid.DISTILLED_WATER, stack);
    }

    private boolean processWaterInput() {
        ItemStack input = items.get(SLOT_WATER_INPUT);
        FluidContainerUtil.DrainData data = FluidContainerUtil.getDrainData(input);
        if (data == null || data.fluid() != UniversalFluidCellItem.CellFluid.WATER) return false;
        if (inputWater + data.amountMb() > TANK_CAPACITY_MB) return false;
        if (!canOutput(SLOT_WATER_OUTPUT, data.output())) return false;

        input.decrement(1);
        insertOutput(SLOT_WATER_OUTPUT, data.output());
        inputWater += data.amountMb();
        return true;
    }

    private boolean processDistilledOutput() {
        ItemStack input = items.get(SLOT_DISTILLED_INPUT);
        FluidContainerUtil.FillData fill = FluidContainerUtil.getFillData(input, UniversalFluidCellItem.CellFluid.DISTILLED_WATER, distilledWater);
        if (fill == null) return false;
        if (!canOutput(SLOT_DISTILLED_OUTPUT, fill.output())) return false;

        input.decrement(1);
        insertOutput(SLOT_DISTILLED_OUTPUT, fill.output());
        distilledWater -= fill.amountMb();
        return true;
    }

    public void updateSunVisibility() {
        if (world == null) {
            skyLight = 0.0f;
            return;
        }
        skyLight = SolarPanelBlockEntity.getSkyLight(world, pos.up());
    }

    public boolean canWork() {
        return inputWater > 0 && distilledWater < TANK_CAPACITY_MB && skyLight > 0.5f;
    }

    public int getTickRate() {
        if (world == null) return 72;
        float temperature = world.getBiome(pos).value().getTemperature();
        var biomeId = world.getRegistryManager().get(RegistryKeys.BIOME).getId(world.getBiome(pos).value());
        String path = biomeId == null ? "" : biomeId.getPath();
        if (temperature >= 1.0f || path.contains("desert") || path.contains("badlands")) return 36;
        if (temperature <= 0.15f || path.contains("snow") || path.contains("frozen") || path.contains("ice")) return 144;
        return 72;
    }

    private boolean canOutput(int slot, ItemStack stack) {
        ItemStack current = items.get(slot);
        return current.isEmpty() || (ItemStack.canCombine(current, stack) && current.getCount() + stack.getCount() <= current.getMaxCount());
    }

    private void insertOutput(int slot, ItemStack stack) {
        ItemStack current = items.get(slot);
        if (current.isEmpty()) items.set(slot, stack.copy());
        else current.increment(stack.getCount());
    }

    public PropertyDelegate getGuiProps() { return props; }
    public int getInputWater() { return inputWater; }
    public int getDistilledWater() { return distilledWater; }
    public int getTankCapacity() { return TANK_CAPACITY_MB; }
    public float getSkyLight() { return skyLight; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putInt("inputWater", inputWater);
        nbt.putInt("distilledWater", distilledWater);
        nbt.putInt("tickrate", tickrate);
        nbt.putInt("updateTicker", updateTicker);
        nbt.putFloat("skyLight", skyLight);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        inputWater = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("inputWater")));
        distilledWater = Math.max(0, Math.min(TANK_CAPACITY_MB, nbt.getInt("distilledWater")));
        tickrate = Math.max(1, nbt.contains("tickrate") ? nbt.getInt("tickrate") : 72);
        updateTicker = nbt.contains("updateTicker") ? nbt.getInt("updateTicker") : -1;
        skyLight = MathHelper.clamp(nbt.getFloat("skyLight"), 0.0f, 1.0f);
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
        if (slot == SLOT_WATER_INPUT) return canInsertWaterContainer(stack);
        if (slot == SLOT_DISTILLED_INPUT) return canInsertDistilledContainer(stack);
        return slot >= SLOT_UPGRADE_0 && slot < SLOT_UPGRADE_0 + UPGRADE_SLOTS;
    }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_WATER_OUTPUT || slot == SLOT_DISTILLED_OUTPUT; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.solar_distiller"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new SolarDistillerScreenHandler(syncId, playerInventory, this); }
}
