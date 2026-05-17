package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.CompressorBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.CompressorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.CompressorScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CompressorBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = AbstractStandardMachineBlockEntity.SLOT_INPUT;
    public static final int SLOT_OUTPUT = AbstractStandardMachineBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_DISCHARGE = AbstractStandardMachineBlockEntity.SLOT_DISCHARGE;
    public static final int SLOT_UPGRADE_0 = AbstractStandardMachineBlockEntity.SLOT_UPGRADE_0;
    public static final int UPGRADE_SLOTS = AbstractStandardMachineBlockEntity.UPGRADE_SLOTS;
    public static final int INV_SIZE = AbstractStandardMachineBlockEntity.SIMPLE_INV_SIZE;

    private static final Object PUMP_WATER_CONTEXT = new Object();

    private static final int TIER = 1;
    private static final long CAPACITY = 600L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 300;

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 4);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CompressorBlockEntity be) {
        be.tickElectricMachine(world, state, CompressorBlock.LIT);
    }

    @Nullable
    @Override
    protected MachineOperation findOperation(World world) {
        CompressorRecipe recipe = MachineRecipeManager.findCompressorRecipe(this).orElse(null);
        if (recipe != null) {
            return operation(recipe.getOutput(world.getRegistryManager()), Math.max(1, recipe.getIngredientCount()), recipe.getTicks());
        }

        if (canUseAdjacentPumpRecipe(world)) {
            return operation(new ItemStack(Items.SNOWBALL), 0, BASE_TICKS, PUMP_WATER_CONTEXT);
        }

        return null;
    }

    @Override
    protected boolean beforeCompleteOperation(World world, MachineOperation operation) {
        if (operation.context() == PUMP_WATER_CONTEXT) {
            return drainWaterFromAdjacentPumps(world, 1000, false);
        }
        return true;
    }

    /**
     * IC2 pump shortcut: if an adjacent pump can supply 1000 mB of water and the compressor
     * input slot is empty, the compressor can compress that water into one snowball.
     */
    private boolean canUseAdjacentPumpRecipe(World world) {
        return items.get(SLOT_INPUT).isEmpty()
                && canOutput(new ItemStack(Items.SNOWBALL))
                && drainWaterFromAdjacentPumps(world, 1000, true);
    }

    private boolean drainWaterFromAdjacentPumps(World world, int amountMb, boolean simulate) {
        int needed = amountMb;
        for (Direction side : Direction.values()) {
            if (world.getBlockEntity(pos.offset(side)) instanceof PumpBlockEntity pump) {
                int drained = pump.drainTank(UniversalFluidCellItem.CellFluid.WATER, needed, simulate);
                needed -= drained;
                if (needed <= 0) return true;
            }
        }
        return false;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.compressor");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new CompressorScreenHandler(syncId, inv, this);
    }
}
