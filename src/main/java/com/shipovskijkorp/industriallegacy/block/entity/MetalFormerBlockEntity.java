package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.MetalFormerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.recipe.MetalFormerRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import com.shipovskijkorp.industriallegacy.screen.MetalFormerScreenHandler;
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
import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MetalFormerBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    public enum Mode {
        EXTRUDING,
        ROLLING,
        CUTTING;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

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

    private static final int TIER = 1;
    private static final long CAPACITY = 2000L;
    private static final int EU_PER_TICK = 10;
    private static final int BASE_TICKS = 200;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy = 0L;
    private int progress = 0;
    private int maxProgress = BASE_TICKS;
    private Mode mode = Mode.EXTRUDING;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> mode.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0, Math.min(CAPACITY, (long) value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                case 4 -> setModeByOrdinal(value);
                default -> { }
            }
        }
    };

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MetalFormerBlockEntity be) {
        if (world.isClient) return;

        boolean didWork = be.processTick(world);
        boolean lit = state.get(MetalFormerBlock.LIT);
        if (lit != didWork) {
            world.setBlockState(pos, state.with(MetalFormerBlock.LIT, didWork), 3);
        }
        if (didWork) be.markDirty();
    }

    private boolean processTick(World world) {
        MetalFormerRecipe recipe = findRecipe(world);
        if (recipe == null) {
            if (progress != 0) progress = 0;
            return false;
        }

        ItemStack in = items.get(SLOT_INPUT);
        if (in.isEmpty()) {
            progress = 0;
            return false;
        }

        ItemStack result = recipe.getOutput(world.getRegistryManager()).copy();
        if (!canOutput(result)) return false;
        if (energy < EU_PER_TICK) return false;

        energy -= EU_PER_TICK;
        maxProgress = recipe.getTicks() <= 0 ? BASE_TICKS : recipe.getTicks();
        progress++;

        if (progress >= maxProgress) {
            int inputCount = Math.max(1, recipe.getInputCount());
            in.decrement(inputCount);
            insertOutput(result);
            progress = 0;
        }

        return true;
    }

    @Nullable
    private MetalFormerRecipe findRecipe(World world) {
        Optional<?> opt = world.getRecipeManager().getFirstMatch(ModRecipes.typeForMode(mode), this, world);
        if (opt.isEmpty()) return null;

        Object o = opt.get();
        if (o instanceof MetalFormerRecipe r) return r;

        // Compat helper for versions where getFirstMatch returns RecipeEntry<R>
        try {
            Method m = o.getClass().getMethod("value");
            Object v = m.invoke(o);
            if (v instanceof MetalFormerRecipe r) return r;
        } catch (Throwable ignored) {
        }

        return null;
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

    public void cycleMode() {
        this.mode = this.mode.next();
        this.progress = 0;
        markDirty();
    }

    public Mode getMode() {
        return mode;
    }

    public void setModeByOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= Mode.values().length) ordinal = 0;
        this.mode = Mode.values()[ordinal];
    }

    public PropertyDelegate getGuiProps() {
        return props;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
        nbt.putString("mode", mode.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.EXTRUDING;
        }
    }

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

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot != SLOT_OUTPUT;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return slot == SLOT_OUTPUT;
    }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0) return 0;
        long accepted = Math.min(amount, CAPACITY - energy);
        if (!simulate && accepted > 0) {
            energy += accepted;
            markDirty();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        return 0;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.industrial_legacy.metal_former");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MetalFormerScreenHandler(syncId, playerInventory, this, props, pos);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}
