package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.RecyclerBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.RecyclerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RecyclerBlockEntity extends AbstractElectricMachineBlockEntity {
    public static final int SLOT_INPUT = AbstractElectricMachineBlockEntity.SLOT_INPUT;
    public static final int SLOT_OUTPUT = AbstractElectricMachineBlockEntity.SLOT_OUTPUT;
    public static final int SLOT_DISCHARGE = AbstractElectricMachineBlockEntity.SLOT_DISCHARGE;
    public static final int SLOT_UPGRADE_0 = AbstractElectricMachineBlockEntity.SLOT_UPGRADE_0;
    public static final int UPGRADE_SLOTS = AbstractElectricMachineBlockEntity.UPGRADE_SLOTS;
    public static final int INV_SIZE = AbstractElectricMachineBlockEntity.SIMPLE_INV_SIZE;

    private static final int TIER = 1;
    private static final long CAPACITY = 45L;
    private static final int EU_PER_TICK = 1;
    private static final int BASE_TICKS = 45;
    private static final int RECYCLE_CHANCE = 8;

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECYCLER, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS, 4);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RecyclerBlockEntity be) {
        be.tickElectricMachine(world, state, RecyclerBlock.LIT);
    }

    @Override
    protected boolean processMachineTick(World world) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || isRecyclerBlacklisted(input)) {
            resetProgress();
            return false;
        }
        if (!canPotentiallyOutputScrap()) return false;
        if (energy < energyConsume) return false;

        energy -= energyConsume;
        maxProgress = operationLength;
        progress++;

        if (progress >= maxProgress) {
            input.decrement(1);
            if (world.random.nextInt(RECYCLE_CHANCE) == 0) {
                insertOutput(new ItemStack(ModItems.SCRAP));
            }
            progress = 0;
        }
        return true;
    }

    public static boolean isRecyclerBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stack.isOf(ModItems.SCRAP) || stack.isOf(ModItems.SCRAP_BOX)) return true;
        if (stack.isOf(Items.GLASS_PANE) || stack.isOf(Items.STICK) || stack.isOf(Items.SNOWBALL)) return true;
        Identifier id = Registries.ITEM.getId(stack.getItem());
        String path = id.getPath();
        return "snow".equals(path)
                || "snow_layer".equals(path)
                || "scaffold".equals(path)
                || path.endsWith("_scaffold");
    }

    private boolean canPotentiallyOutputScrap() {
        ItemStack out = items.get(SLOT_OUTPUT);
        return out.isEmpty() || (out.isOf(ModItems.SCRAP) && out.getCount() < out.getMaxCount());
    }

    @Override
    protected boolean canInsertIntoMachineSlot(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_INPUT) return !isRecyclerBlacklisted(stack);
        return super.canInsertIntoMachineSlot(slot, stack, dir);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.recycler");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new RecyclerScreenHandler(syncId, playerInventory, this);
    }
}
