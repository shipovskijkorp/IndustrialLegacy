package com.shipovskijkorp.industriallegacy.block.entity;

import java.util.Set;
import java.util.EnumSet;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.ThermalCentrifugeBlock;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.ThermalCentrifugeRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ThermalCentrifugeScreenHandler;
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

import java.util.ArrayList;
import java.util.List;

public class ThermalCentrifugeBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT_0 = 1;
    public static final int SLOT_OUTPUT_1 = 2;
    public static final int SLOT_OUTPUT_2 = 3;
    public static final int SLOT_DISCHARGE = 4;
    public static final int SLOT_UPGRADE_0 = 5;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[] { SLOT_INPUT };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3 };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 };

    private static final int TIER = 2;
    // IL Exp 2.8.222: TileEntityCentrifuge extends TileEntityStandardMachine(48, 500, 3, 2),
    // so the base buffer is 48 EU/t * 500 ticks = 24000 EU.
    private static final long CAPACITY = 24000L;
    private static final int EU_PER_TICK_PROCESS = 48;
    private static final int EU_PER_HEAT_TICK = 1;
    private static final int BASE_TICKS = 500;
    private static final int MAX_HEAT = 5000;

    private int heat = 0;
    private int workHeat = MAX_HEAT;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 6; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int)Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int)Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> heat;
                case 5 -> workHeat;
                default -> 0;
            };
        }
        @Override public void set(int i, int value) {
            switch (i) {
                case 0 -> energy = clampEnergy(value);
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> heat = Math.max(0, Math.min(MAX_HEAT, value));
                case 5 -> workHeat = Math.max(1, Math.min(MAX_HEAT, value));
                default -> { }
            }
        }
    };

    public ThermalCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_CENTRIFUGE, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK_PROCESS, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS,
                new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2});
    }

    public static void tick(World world, BlockPos pos, BlockState state, ThermalCentrifugeBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        dirty |= be.tickUpgrades();
        boolean active = be.processTick(world);
        if (state.get(ThermalCentrifugeBlock.LIT) != active) world.setBlockState(pos, state.with(ThermalCentrifugeBlock.LIT, active), 3);
        if (active || be.heat > 0 || dirty) be.markDirty();
    }

    private boolean processTick(World world) {
        ThermalCentrifugeRecipe recipe = MachineRecipeManager.findThermalCentrifugeRecipe(this).orElse(null);
        int recipeHeat = recipe == null ? 0 : Math.min(MAX_HEAT, recipe.getHeat());
        boolean redstoneHeat = hasEffectiveRedstoneInput();
        boolean active = false;

        if (redstoneHeat) {
            workHeat = MAX_HEAT;
        } else if (recipe != null) {
            workHeat = Math.max(1, recipeHeat);
            if (heat > recipeHeat) {
                heat = recipeHeat;
            }
        }

        if (recipe != null && heat >= recipeHeat) {
            List<ItemStack> outputs = recipe.getResults();
            if (canOutputStacks(outputs)) {
                if (energy >= energyConsume) {
                    energy -= energyConsume;
                    maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
                    progress++;
                    active = true;
                    if (progress >= maxProgress) {
                        items.get(SLOT_INPUT).decrement(recipe.getInputCount());
                        insertOutputStacks(outputs);
                        progress = 0;
                    }
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
        }

        int heatRequested = redstoneHeat ? MAX_HEAT : recipe == null ? -1 : recipeHeat;
        if (heatRequested >= 0 && heat < MAX_HEAT && heat - 1 < heatRequested && energy >= EU_PER_HEAT_TICK) {
            energy -= EU_PER_HEAT_TICK;
            heat++;
            active = true;
        } else if (heat > 0) {
            heat--;
        }

        heat = Math.max(0, Math.min(MAX_HEAT, heat));
        workHeat = Math.max(1, Math.min(MAX_HEAT, workHeat));
        return active || (recipe != null && heat > 0);
    }

    private boolean canOutputStacks(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy());
        temp.add(items.get(SLOT_OUTPUT_1).copy());
        temp.add(items.get(SLOT_OUTPUT_2).copy());
        return canMergeToTempOutputs(outputs, temp);
    }

    private void insertOutputStacks(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            ItemStack remaining = out.copy();
            for (int slot : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                if (remaining.isEmpty()) break;
                ItemStack current = items.get(slot);
                if (!current.isEmpty() && ItemStack.canCombine(current, remaining)) {
                    int move = Math.min(remaining.getCount(), current.getMaxCount() - current.getCount());
                    if (move > 0) { current.increment(move); remaining.decrement(move); }
                }
            }
            for (int slot : new int[]{SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2}) {
                if (remaining.isEmpty()) break;
                ItemStack current = items.get(slot);
                if (current.isEmpty()) {
                    int move = Math.min(remaining.getCount(), remaining.getMaxCount());
                    ItemStack placed = remaining.copy();
                    placed.setCount(move);
                    items.set(slot, placed);
                    remaining.decrement(move);
                }
            }
        }
    }

    private static boolean canMergeToTempOutputs(List<ItemStack> outputs, List<ItemStack> temp) {
        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (ItemStack slot : temp) {
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) { slot.increment(add); remaining -= add; }
                }
            }
            for (int i = 0; i < temp.size() && remaining > 0; i++) {
                ItemStack slot = temp.get(i);
                if (slot.isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    temp.set(i, placed);
                    remaining -= add;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }


    @Override
    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Processing,
                UpgradableProperty.RedstoneSensitive,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing
        );
    }

    @Override public PropertyDelegate getGuiProps() { return props; }
    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); nbt.putInt("heat", heat); nbt.putInt("workHeat", workHeat); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); heat = Math.max(0, Math.min(MAX_HEAT, nbt.getInt("heat"))); workHeat = Math.max(1, Math.min(MAX_HEAT, nbt.getInt("workHeat"))); }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.thermal_centrifuge"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new ThermalCentrifugeScreenHandler(syncId, inv, this); }
}
