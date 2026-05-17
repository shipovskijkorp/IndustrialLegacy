package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.ElectricFurnaceBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ElectricFurnaceScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

public class ElectricFurnaceBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[]{SLOT_INPUT};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    private static final int TIER = 1;
    private static final long CAPACITY = 300L;
    private static final int EU_PER_TICK = 3;
    private static final int BASE_TICKS = 100;

    private int storedXp = 0;
    private float storedXpFraction = 0.0F;

    private final PropertyDelegate furnaceGuiProps = new PropertyDelegate() {
        @Override
        public int size() {
            return 5;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> storedXp;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = clampEnergy(value);
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> storedXp = Math.max(0, value);
                default -> { }
            }
        }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processStandardMachine(world);
        if (state.get(ElectricFurnaceBlock.LIT) != active) world.setBlockState(pos, state.with(ElectricFurnaceBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    @Override
    protected MachineOperation getOperation(World world) {
        ItemStack input = items.get(SLOT_INPUT);
        SmeltingMatch match = findRecipe(world, input).orElse(null);
        if (match == null) return null;
        return operation(
                java.util.List.of(new SlotConsumption(SLOT_INPUT, 1)),
                java.util.List.of(new SlotOutput(SLOT_OUTPUT, match.output().copy())),
                operationLength,
                energyConsume,
                () -> addRecipeExperience(match.experience())
        );
    }

    private Optional<SmeltingMatch> findRecipe(World world, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        Optional<?> opt = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SimpleInventory(input.copy()), world);
        if (opt.isEmpty()) return Optional.empty();

        Object o = opt.get();
        if (o instanceof AbstractCookingRecipe recipe) {
            return Optional.of(new SmeltingMatch(recipe.getOutput(world.getRegistryManager()).copy(), recipe.getExperience()));
        }

        try {
            Method value = o.getClass().getMethod("value");
            Object recipeObj = value.invoke(o);
            if (recipeObj instanceof AbstractCookingRecipe recipe) {
                return Optional.of(new SmeltingMatch(recipe.getOutput(world.getRegistryManager()).copy(), recipe.getExperience()));
            }
        } catch (Throwable ignored) { }

        return Optional.empty();
    }

    private void addRecipeExperience(float experience) {
        if (experience <= 0.0F) return;
        storedXpFraction += experience;
        int whole = MathHelper.floor(storedXpFraction);
        if (whole > 0) {
            storedXp += whole;
            storedXpFraction -= whole;
        }
    }

    public void collectXp(ServerPlayerEntity player) {
        if (storedXp <= 0) return;
        player.addExperience(storedXp);
        storedXp = 0;
        storedXpFraction = 0.0F;
        markDirty();
    }

    @Override
    public PropertyDelegate getGuiProps() {
        return furnaceGuiProps;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("storedXp", storedXp);
        nbt.putFloat("storedXpFraction", storedXpFraction);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        storedXp = Math.max(0, nbt.getInt("storedXp"));
        storedXpFraction = Math.max(0.0F, nbt.getFloat("storedXpFraction"));
    }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.electric_furnace"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new ElectricFurnaceScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    private record SmeltingMatch(ItemStack output, float experience) { }
}
