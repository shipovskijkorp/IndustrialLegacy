package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.CompressorBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.recipe.CompressorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.CompressorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

import java.util.Optional;

public class CompressorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;

    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;

    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[]{SLOT_INPUT};
    private static final int[] SIDE_SLOTS = new int[]{
            SLOT_INPUT,
            SLOT_DISCHARGE,
            SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3
    };
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    // IC2-ish constants
    private static final int TIER = 1;
    private static final long CAPACITY = 600L;
    private static final int EU_PER_TICK = 2;
    private static final int BASE_TICKS = 300;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);

    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;

    // GUI sync: [0]=energy, [1]=capacity, [2]=progress, [3]=maxProgress
    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 4; }

        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> 0;
            };
        }

        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> energy = Math.max(0, Math.min(CAPACITY, (long) v));
                case 2 -> progress = Math.max(0, v);
                case 3 -> maxProgress = Math.max(1, v);
                default -> {}
            }
        }
    };

    public CompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CompressorBlockEntity be) {
        if (world.isClient) return;

        // (опционально) разряд батарейки в discharge-slot — если у тебя есть менеджер, подключишь позже.
        // be.tryDischargeBattery();

        boolean didWork = be.processTick(world);

        boolean lit = state.get(CompressorBlock.LIT);
        if (lit != didWork) {
            world.setBlockState(pos, state.with(CompressorBlock.LIT, didWork), 3);
        }

        if (didWork) be.markDirty();
    }

    private boolean processTick(World world) {
        CompressorRecipe recipe = findRecipe(world).orElse(null);
        boolean pumpRecipe = recipe == null && canUseAdjacentPumpRecipe(world);

        if (recipe == null && !pumpRecipe) {
            if (progress != 0) progress = 0;
            return false;
        }

        ItemStack result = pumpRecipe ? new ItemStack(Items.SNOWBALL) : recipe.getOutput(world.getRegistryManager()).copy();
        if (!canOutput(result)) return false;

        if (energy < EU_PER_TICK) return false;
        energy -= EU_PER_TICK;

        maxProgress = pumpRecipe || recipe.getTicks() <= 0 ? BASE_TICKS : recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            if (pumpRecipe) {
                if (!drainWaterFromAdjacentPumps(world, 1000, false)) {
                    progress = 0;
                    return false;
                }
            } else {
                ItemStack in = items.get(SLOT_INPUT);
                if (in.isEmpty()) {
                    progress = 0;
                    return false;
                }
                in.decrement(Math.max(1, recipe.getIngredientCount()));
            }
            insertOutput(result);
            progress = 0;
        }

        return true;
    }

    private Optional<CompressorRecipe> findRecipe(World world) {
        return MachineRecipeManager.findCompressorRecipe(this);
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

    private boolean canOutput(ItemStack stack) {
        ItemStack out = items.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.canCombine(out, stack)) return false;
        return out.getCount() + stack.getCount() <= out.getMaxCount();
    }

    private void insertOutput(ItemStack stack) {
        ItemStack out = items.get(SLOT_OUTPUT);
        if (out.isEmpty()) items.set(SLOT_OUTPUT, stack);
        else out.increment(stack.getCount());
    }

    // --- NBT ---
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
    }

    // --- Inventory ---
    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack res = Inventories.splitStack(items, slot, amount);
        if (!res.isEmpty()) markDirty();
        return res;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack res = Inventories.removeStack(items, slot);
        markDirty();
        return res;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount());
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null) return false;
        if (world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override public void clear() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_OUTPUT) return false;
        return true;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    // --- EU storage (sink only) ---
    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }

    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0) return 0;
        long free = CAPACITY - energy;
        if (free <= 0) return 0;
        long acc = Math.min(amount, free);
        if (!simulate && acc > 0) {
            energy += acc;
            markDirty();
        }
        return acc;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0;
    }

    public PropertyDelegate getGuiProps() { return props; }

    // --- Screen ---
    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.compressor");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new CompressorScreenHandler(syncId, inv, this);
    }
}
