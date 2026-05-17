package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.ThermalCentrifugeBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ThermalCentrifugeBlockEntity extends AbstractElectricMachineBlockEntity {
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
    private static final long CAPACITY = 10000L;
    private static final int EU_PER_TICK_PROCESS = 48;
    private static final int EU_PER_HEAT_TICK = 1;
    private static final int BASE_TICKS = 500;
    private static final int MAX_HEAT = 5000;

    private int heat = 0;
    private int workHeat = MAX_HEAT;

    public ThermalCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THERMAL_CENTRIFUGE, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK_PROCESS, BASE_TICKS, 6);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ThermalCentrifugeBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processMachineTick(world);
        if (state.get(ThermalCentrifugeBlock.LIT) != active) {
            world.setBlockState(pos, state.with(ThermalCentrifugeBlock.LIT, active), 3);
        }
        if (dirty || active || be.heat > 0) be.markDirty();
    }

    @Override
    protected boolean processMachineTick(World world) {
        ThermalCentrifugeRecipe recipe = findRecipe(world).orElse(null);
        int targetHeat = recipe == null ? 0 : Math.min(MAX_HEAT, recipe.getHeat());
        workHeat = Math.max(1, targetHeat == 0 ? MAX_HEAT : targetHeat);

        boolean active = false;

        if (recipe != null) {
            if (heat < targetHeat && energy >= EU_PER_HEAT_TICK) {
                energy -= EU_PER_HEAT_TICK;
                heat++;
                active = true;
            } else if (heat > targetHeat) {
                heat--;
            }

            if (heat >= targetHeat) {
                List<ItemStack> outputs = recipe.getResults();
                if (canOutput(outputs)) {
                    if (energy >= energyConsume) {
                        energy -= energyConsume;
                        maxProgress = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
                        progress++;
                        active = true;

                        if (progress >= maxProgress) {
                            items.get(SLOT_INPUT).decrement(Math.max(1, recipe.getInputCount()));
                            insertOutputs(outputs);
                            progress = 0;
                        }
                    }
                } else {
                    resetProgress();
                }
            } else {
                resetProgress();
            }
        } else {
            resetProgress();
            if (heat > 0) heat--;
        }

        return active || (recipe != null && heat > 0);
    }

    private Optional<ThermalCentrifugeRecipe> findRecipe(World world) {
        return MachineRecipeManager.findThermalCentrifugeRecipe(this);
    }

    private boolean canOutput(List<ItemStack> outputs) {
        List<ItemStack> temp = new ArrayList<>();
        temp.add(items.get(SLOT_OUTPUT_0).copy());
        temp.add(items.get(SLOT_OUTPUT_1).copy());
        temp.add(items.get(SLOT_OUTPUT_2).copy());

        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (ItemStack slot : temp) {
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
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

    private void insertOutputs(List<ItemStack> outputs) {
        for (ItemStack out : outputs) {
            int remaining = out.getCount();
            for (int slotId : new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 }) {
                ItemStack slot = items.get(slotId);
                if (!slot.isEmpty() && ItemStack.canCombine(slot, out)) {
                    int add = Math.min(remaining, slot.getMaxCount() - slot.getCount());
                    if (add > 0) {
                        slot.increment(add);
                        remaining -= add;
                    }
                }
            }
            for (int slotId : new int[] { SLOT_OUTPUT_0, SLOT_OUTPUT_1, SLOT_OUTPUT_2 }) {
                if (remaining <= 0) break;
                ItemStack slot = items.get(slotId);
                if (slot.isEmpty()) {
                    ItemStack placed = out.copy();
                    int add = Math.min(remaining, placed.getMaxCount());
                    placed.setCount(add);
                    items.set(slotId, placed);
                    remaining -= add;
                }
            }
        }
    }

    @Override protected int getInputSlot() { return SLOT_INPUT; }
    @Override protected int getOutputSlot() { return SLOT_OUTPUT_0; }
    @Override protected int getDischargeSlot() { return SLOT_DISCHARGE; }
    @Override protected int getFirstUpgradeSlot() { return SLOT_UPGRADE_0; }
    @Override protected int getUpgradeSlotCount() { return UPGRADE_SLOTS; }
    @Override protected int[] getTopSlots() { return TOP_SLOTS; }
    @Override protected int[] getSideSlots() { return SIDE_SLOTS; }
    @Override protected int[] getBottomSlots() { return BOTTOM_SLOTS; }

    @Override
    protected boolean isOutputSlot(int slot) {
        return slot == SLOT_OUTPUT_0 || slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2;
    }

    @Override
    protected boolean canExtractFromMachineSlot(int slot, ItemStack stack, Direction dir) {
        return isOutputSlot(slot);
    }

    @Override
    protected int getExtraGuiProperty(int index) {
        return switch (index) {
            case 4 -> heat;
            case 5 -> workHeat;
            default -> 0;
        };
    }

    @Override
    protected void setExtraGuiProperty(int index, int value) {
        switch (index) {
            case 4 -> heat = Math.max(0, Math.min(MAX_HEAT, value));
            case 5 -> workHeat = Math.max(1, value);
            default -> { }
        }
    }

    @Override
    protected void writeMachineNbt(NbtCompound nbt) {
        nbt.putInt("heat", heat);
        nbt.putInt("workHeat", workHeat);
    }

    @Override
    protected void readMachineNbt(NbtCompound nbt) {
        heat = Math.max(0, Math.min(MAX_HEAT, nbt.getInt("heat")));
        workHeat = Math.max(1, nbt.getInt("workHeat"));
    }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.thermal_centrifuge"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) { return new ThermalCentrifugeScreenHandler(syncId, inv, this); }
}
