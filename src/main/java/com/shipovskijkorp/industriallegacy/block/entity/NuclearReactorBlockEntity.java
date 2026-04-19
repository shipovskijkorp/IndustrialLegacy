package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.NuclearReactorBlock;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.NuclearReactorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.util.ItemScatterer;
import net.minecraft.item.ItemStack;
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

public class NuclearReactorBlockEntity extends BlockEntity implements SidedInventory, ExtendedScreenHandlerFactory, IReactor {
    public static final int COLUMNS = 9;
    public static final int ROWS = 6;
    public static final int INV_SIZE = COLUMNS * ROWS;
    private static final int[] NO_SLOTS = new int[0];

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private int heat;
    private int maxHeat = 10000;
    private float heatEffectModifier = 1.0f;
    private int emittedHeat;
    private float output;
    private int cachedSize = 3;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> maxHeat;
                case 2 -> emittedHeat;
                case 3 -> cachedSize;
                case 4 -> Math.round(output * 10.0f);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> heat = value;
                case 1 -> maxHeat = value;
                case 2 -> emittedHeat = value;
                case 3 -> cachedSize = value;
                case 4 -> output = value / 10.0f;
            }
        }
    };

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEAR_REACTOR, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, NuclearReactorBlockEntity be) {
        if (world.isClient) return;

        int oldSize = be.cachedSize;
        be.cachedSize = be.getReactorSize();
        if (oldSize != be.cachedSize) {
            be.dropUnfittingStuff();
        }

        be.maxHeat = 10000;
        be.heatEffectModifier = 1.0f;
        be.emittedHeat = 0;
        be.output = 0.0f;

        be.processChambers(true);
        be.processChambers(false);

        boolean active = be.output > 0.0f || be.heat > 0 || be.emittedHeat > 0;
        if (state.get(NuclearReactorBlock.LIT) != active) {
            world.setBlockState(pos, state.with(NuclearReactorBlock.LIT, active), 3);
        }

        if (be.heat >= be.maxHeat) {
            be.explode();
            return;
        }

        be.markDirty();
    }

    private void processChambers(boolean heatRun) {
        int size = getReactorSize();
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < size; x++) {
                ItemStack stack = getItemAt(x, y);
                if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp) {
                    comp.processChamber(stack, this, x, y, heatRun);
                }
            }
        }
    }

    public int getReactorSize() {
        if (world == null) return 3;
        int cols = 3;
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.offset(dir)).isOf(com.shipovskijkorp.industriallegacy.registry.ModBlocks.REACTOR_CHAMBER)) {
                cols++;
            }
        }
        return Math.min(COLUMNS, cols);
    }

    private void dropUnfittingStuff() {
        if (world == null || world.isClient) return;
        int size = getReactorSize();
        for (int y = 0; y < ROWS; y++) {
            for (int x = size; x < COLUMNS; x++) {
                int idx = index(x, y);
                ItemStack stack = items.get(idx);
                if (!stack.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                    items.set(idx, ItemStack.EMPTY);
                }
            }
        }
    }

    private static int index(int x, int y) {
        return y * COLUMNS + x;
    }

    public boolean isSlotEnabled(int slot) {
        int x = slot % COLUMNS;
        return x < getReactorSize();
    }

    public PropertyDelegate getGuiProps() {
        return props;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.nuclear_reactor");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new NuclearReactorScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public int size() { return INV_SIZE; }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) stack.setCount(getMaxCountPerStack());
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d) <= 64.0d;
    }

    @Override
    public void clear() { items.clear(); }

    @Override
    public int[] getAvailableSlots(Direction side) { return NO_SLOTS; }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return true; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isSlotEnabled(slot) && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        heat = nbt.getInt("Heat");
        maxHeat = nbt.getInt("MaxHeat");
        emittedHeat = nbt.getInt("EmitHeat");
        cachedSize = Math.max(3, nbt.getInt("Size"));
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putInt("Heat", heat);
        nbt.putInt("MaxHeat", maxHeat);
        nbt.putInt("EmitHeat", emittedHeat);
        nbt.putInt("Size", cachedSize);
    }

    @Override
    public @Nullable ItemStack getItemAt(int x, int y) {
        if (x < 0 || y < 0 || x >= getReactorSize() || y >= ROWS) return null;
        ItemStack stack = items.get(index(x, y));
        return stack.isEmpty() ? null : stack;
    }

    @Override
    public void setItemAt(int x, int y, @Nullable ItemStack stack) {
        if (x < 0 || y < 0 || x >= COLUMNS || y >= ROWS) return;
        items.set(index(x, y), stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack);
        markDirty();
    }

    @Override
    public int getHeat() { return heat; }

    @Override
    public void setHeat(int heat) {
        this.heat = Math.max(0, heat);
    }

    @Override
    public int addHeat(int heat) {
        this.heat = Math.max(0, this.heat + heat);
        return this.heat;
    }

    @Override
    public int getMaxHeat() { return maxHeat; }

    @Override
    public void setMaxHeat(int heat) { this.maxHeat = Math.max(1000, heat); }

    @Override
    public int addEmitHeat(int heat) {
        emittedHeat += heat;
        return emittedHeat;
    }

    @Override
    public float getHeatEffectModifier() { return heatEffectModifier; }

    @Override
    public void setHeatEffectModifier(float modifier) { heatEffectModifier = modifier; }

    @Override
    public float getReactorEnergyOutput() { return output; }

    @Override
    public float addOutput(float amount) {
        output += amount;
        return output;
    }

    @Override
    public void explode() {
        if (world == null || world.isClient) return;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
            }
        }
        clear();

        for (Direction dir : Direction.values()) {
            BlockPos chamberPos = pos.offset(dir);
            if (world.getBlockState(chamberPos).isOf(com.shipovskijkorp.industriallegacy.registry.ModBlocks.REACTOR_CHAMBER)) {
                world.breakBlock(chamberPos, true);
                world.setBlockState(chamberPos, Blocks.FIRE.getDefaultState(), 3);
            }
        }

        world.setBlockState(pos, Blocks.FIRE.getDefaultState(), 3);
    }

    @Override
    public boolean produceEnergy() {
        return true;
    }
}
