package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.ExtractorBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.recipe.ExtractorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ExtractorScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ExtractorBlockEntity extends AbstractStandardMachineBlockEntity {
    public static final int SLOT_INPUT = AbstractStandardMachineBlockEntity.SLOT_INPUT;
    public static final int SLOT_OUTPUT = AbstractStandardMachineBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_DISCHARGE = AbstractStandardMachineBlockEntity.SLOT_DISCHARGE;
    public static final int SLOT_UPGRADE_0 = AbstractStandardMachineBlockEntity.SLOT_UPGRADE_0;
    public static final int UPGRADE_SLOTS = AbstractStandardMachineBlockEntity.UPGRADE_SLOTS;
    public static final int INV_SIZE = AbstractStandardMachineBlockEntity.SIMPLE_INV_SIZE;

    private static final int TIER = 1;
    private static final long CAPACITY = 600L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 300;

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXTRACTOR, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 4);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ExtractorBlockEntity be) {
        be.tickElectricMachine(world, state, ExtractorBlock.LIT);
    }

    @Nullable
    @Override
    protected MachineOperation findOperation(World world) {
        ExtractorRecipe recipe = MachineRecipeManager.findExtractorRecipe(this).orElse(null);
        if (recipe == null) return null;
        return operation(recipe.getOutput(world.getRegistryManager()), Math.max(1, recipe.getIngredientCount()), recipe.getTicks());
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.extractor");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ExtractorScreenHandler(syncId, inv, this);
    }
}
