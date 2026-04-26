package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.RecyclerBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.RecyclerScreenHandler;
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
import net.minecraft.registry.Registries;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RecyclerBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
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
    private static final long CAPACITY = 45L;
    private static final int EU_PER_TICK = 1;
    private static final int BASE_TICKS = 45;
    private static final int RECYCLE_CHANCE = 8;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private long energy;
    private int progress;
    private int maxProgress = BASE_TICKS;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 4; }
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) CAPACITY;
                case 2 -> progress;
                case 3 -> maxProgress;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> progress = Math.max(0, value);
                case 3 -> maxProgress = Math.max(1, value);
                default -> {}
            }
        }
    };

    public RecyclerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECYCLER, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RecyclerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processTick(world);
        if (state.get(RecyclerBlock.LIT) != active) {
            world.setBlockState(pos, state.with(RecyclerBlock.LIT, active), 3);
        }
        if (dirty || active) be.markDirty();
    }

    private boolean chargeFromDischargeSlot() {
        ItemStack discharge = items.get(SLOT_DISCHARGE);
        if (discharge.isEmpty() || !ElectricItemManager.isElectric(discharge) || discharge.getCount() != 1) return false;
        long free = CAPACITY - energy;
        if (free <= 0L) return false;
        long maxMove = Math.min(free, EuUtil.powerFromTier(TIER));
        long extracted = ElectricItemManager.discharge(discharge, maxMove, false);
        if (extracted > 0L) {
            energy += extracted;
            return true;
        }
        return false;
    }

    private boolean processTick(World world) {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.isEmpty() || isRecyclerBlacklisted(input)) {
            if (progress != 0) progress = 0;
            return false;
        }
        if (energy < EU_PER_TICK) return false;
        if (!canPotentiallyOutputScrap()) return false;

        energy -= EU_PER_TICK;
        maxProgress = BASE_TICKS;
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

    private void insertOutput(ItemStack stack) {
        ItemStack out = items.get(SLOT_OUTPUT);
        if (out.isEmpty()) items.set(SLOT_OUTPUT, stack);
        else if (ItemStack.canCombine(out, stack)) out.increment(stack.getCount());
    }

    public PropertyDelegate getGuiProps() { return props; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("progress", progress);
        nbt.putInt("maxProgress", maxProgress);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        progress = Math.max(0, nbt.getInt("progress"));
        maxProgress = Math.max(1, nbt.getInt("maxProgress"));
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack result = Inventories.splitStack(items, slot, amount); if (!result.isEmpty()) markDirty(); return result; }
    @Override public ItemStack removeStack(int slot) { ItemStack result = Inventories.removeStack(items, slot); markDirty(); return result; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0; }
    @Override public void clear() { for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY); }
    @Override public int[] getAvailableSlots(Direction side) { if (side == Direction.UP) return TOP_SLOTS; if (side == Direction.DOWN) return BOTTOM_SLOTS; return SIDE_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { if (slot == SLOT_OUTPUT) return false; if (slot == SLOT_INPUT) return !isRecyclerBlacklisted(stack); return true; }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_OUTPUT; }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return 0; }
    @Override public boolean canInsert(Direction from) { return true; }
    @Override public boolean canExtract(Direction to) { return false; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { long acc = Math.min(Math.max(0L, amount), CAPACITY - energy); if (!simulate && acc > 0L) { energy += acc; markDirty(); } return acc; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { return 0L; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.recycler"); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new RecyclerScreenHandler(syncId, playerInventory, this); }
}
