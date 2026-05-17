package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.ElectricFurnaceBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
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
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

public class ElectricFurnaceBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = AbstractStandardMachineBlockEntity.SLOT_INPUT;
    public static final int SLOT_OUTPUT = AbstractStandardMachineBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_DISCHARGE = AbstractStandardMachineBlockEntity.SLOT_DISCHARGE;
    public static final int SLOT_UPGRADE_0 = AbstractStandardMachineBlockEntity.SLOT_UPGRADE_0;
    public static final int UPGRADE_SLOTS = AbstractStandardMachineBlockEntity.UPGRADE_SLOTS;
    public static final int INV_SIZE = AbstractStandardMachineBlockEntity.SIMPLE_INV_SIZE;

    private static final int TIER = 1;
    private static final long CAPACITY = 300L;
    private static final int EU_PER_TICK = 3;
    private static final int BASE_TICKS = 100;

    private double xp = 0.0;

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 5);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity be) {
        be.tickElectricMachine(world, state, ElectricFurnaceBlock.LIT);
    }

    @Nullable
    @Override
    protected MachineOperation findOperation(World world) {
        SmeltingMatch match = findRecipe(world).orElse(null);
        if (match == null) return null;
        return operation(match.output(), 1, BASE_TICKS, match);
    }

    @Override
    protected void afterCompleteOperation(World world, MachineOperation operation) {
        if (operation.context() instanceof SmeltingMatch match) {
            xp += match.experience();
        }
    }

    public int collectXp(PlayerEntity player) {
        int amount = (int) Math.floor(xp);
        if (amount > 0) {
            player.addExperience(amount);
            xp -= amount;
            markDirty();
        }
        return amount;
    }

    private Optional<SmeltingMatch> findRecipe(World world) {
        ItemStack input = items.get(SLOT_INPUT);
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
        } catch (Throwable ignored) {
        }

        return Optional.empty();
    }

    @Override
    protected int getExtraGuiProperty(int index) {
        return index == 4 ? (int) Math.floor(xp) : 0;
    }

    @Override
    protected void setExtraGuiProperty(int index, int value) {
        if (index == 4) xp = Math.max(0.0, value);
    }

    @Override
    protected void writeMachineNbt(NbtCompound nbt) {
        nbt.putDouble("xp", xp);
    }

    @Override
    protected void readMachineNbt(NbtCompound nbt) {
        xp = Math.max(0.0, nbt.getDouble("xp"));
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.electric_furnace");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ElectricFurnaceScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    private record SmeltingMatch(ItemStack output, float experience) {
    }
}
