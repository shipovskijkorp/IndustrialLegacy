package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.MetalFormerBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.MetalFormerRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.MetalFormerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MetalFormerBlockEntity extends AbstractStandardMachineBlockEntity {
    public enum Mode {
        EXTRUDING,
        ROLLING,
        CUTTING;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final int SLOT_INPUT = AbstractStandardMachineBlockEntity.SLOT_INPUT;
    public static final int SLOT_OUTPUT = AbstractStandardMachineBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_DISCHARGE = AbstractStandardMachineBlockEntity.SLOT_DISCHARGE;
    public static final int SLOT_UPGRADE_0 = AbstractStandardMachineBlockEntity.SLOT_UPGRADE_0;
    public static final int UPGRADE_SLOTS = AbstractStandardMachineBlockEntity.UPGRADE_SLOTS;
    public static final int INV_SIZE = AbstractStandardMachineBlockEntity.SIMPLE_INV_SIZE;

    private static final int TIER = 1;
    private static final long CAPACITY = 2000L;
    private static final int EU_PER_TICK = 10;
    private static final int BASE_TICKS = 200;

    private Mode mode = Mode.EXTRUDING;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 5);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MetalFormerBlockEntity be) {
        be.tickElectricMachine(world, state, MetalFormerBlock.LIT);
    }

    @Nullable
    @Override
    protected MachineOperation findOperation(World world) {
        MetalFormerRecipe recipe = MachineRecipeManager.findMetalFormerRecipe(this, mode).orElse(null);
        if (recipe == null) return null;
        return operation(recipe.getOutput(world.getRegistryManager()), Math.max(1, recipe.getInputCount()), recipe.getTicks());
    }

    public void cycleMode() {
        this.mode = this.mode.next();
        resetProgress();
        markDirty();
    }

    public Mode getMode() {
        return mode;
    }

    public void setModeByOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= Mode.values().length) ordinal = 0;
        this.mode = Mode.values()[ordinal];
    }

    @Override
    protected int getExtraGuiProperty(int index) {
        return index == 4 ? mode.ordinal() : 0;
    }

    @Override
    protected void setExtraGuiProperty(int index, int value) {
        if (index == 4) setModeByOrdinal(value);
    }

    @Override
    protected void writeMachineNbt(NbtCompound nbt) {
        nbt.putString("mode", mode.name());
    }

    @Override
    protected void readMachineNbt(NbtCompound nbt) {
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.EXTRUDING;
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.industrial_legacy.metal_former");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MetalFormerScreenHandler(syncId, playerInventory, this, getGuiProps(), pos);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}
