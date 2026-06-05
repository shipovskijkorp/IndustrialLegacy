package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.RTHeatGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.heat.IHeatSource;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.RTHeatGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
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

public class RTHeatGeneratorBlockEntity extends BlockEntity implements SidedInventory, IHeatSource, ExtendedScreenHandlerFactory {
    public static final int SLOT_COUNT = 6;
    private static final int[] ALL_SLOTS = new int[]{0, 1, 2, 3, 4, 5};
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
    private int heatBuffer;
    private int transmitHeat;
    private int maxHeatEmitPerTick;
    private final float outputMultiplier;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return RTHeatGeneratorScreenHandler.PROP_COUNT; }
        @Override public int get(int index) { return switch (index) { case 0 -> transmitHeat; case 1 -> maxHeatEmitPerTick; case 2 -> heatBuffer; case 3 -> countPellets(); default -> 0; }; }
        @Override public void set(int index, int value) { switch (index) { case 0 -> transmitHeat = Math.max(0, value); case 1 -> maxHeatEmitPerTick = Math.max(0, value); case 2 -> heatBuffer = Math.max(0, value); default -> { } } }
    };

    public RTHeatGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RT_HEAT_GENERATOR, pos, state);
        this.outputMultiplier = 2.0f * ILConfig.getFloat("balance/energy/heatgenerator/radioisotope", 1.0f);
    }

    public static void tick(World world, BlockPos pos, BlockState state, RTHeatGeneratorBlockEntity be) {
        if (world.isClient) return;
        be.maxHeatEmitPerTick = be.getMaxHeatEmittedPerTick();
        int amount = be.maxHeatEmitPerTick - be.heatBuffer;
        if (amount > 0) be.heatBuffer += be.fillHeatBuffer(amount);
        boolean active = be.heatBuffer > 0;
        if (state.get(RTHeatGeneratorBlock.LIT) != active) world.setBlockState(pos, state.with(RTHeatGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
        if (active) be.markDirty();
    }

    protected int fillHeatBuffer(int maxAmount) { return maxAmount >= getMaxHeatEmittedPerTick() ? getMaxHeatEmittedPerTick() : maxAmount; }

    public int getMaxHeatEmittedPerTick() {
        int counter = countPellets();
        return counter == 0 ? 0 : (int) (Math.pow(2.0, counter - 1) * (double) outputMultiplier);
    }

    public int countPellets() { int c = 0; for (ItemStack s : items) if (!s.isEmpty() && s.isOf(ModItems.RTG_PELLET)) c++; return c; }
    public Direction getFacing() { BlockState state = getCachedState(); return state.contains(RTHeatGeneratorBlock.FACING) ? state.get(RTHeatGeneratorBlock.FACING) : Direction.NORTH; }
    public PropertyDelegate getGuiProperties() { return props; }

    @Override public int getConnectionBandwidth(Direction side) { return side == getFacing() ? getMaxHeatEmittedPerTick() : 0; }
    @Override public int drawHeat(Direction side, int request, boolean simulate) { if (side != getFacing() || request <= 0) return 0; int drawn = Math.min(request, heatBuffer); if (!simulate) { heatBuffer -= drawn; transmitHeat = drawn; markDirty(); } return drawn; }

    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); Inventories.writeNbt(nbt, items); nbt.putInt("HeatBuffer", heatBuffer); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); Inventories.readNbt(nbt, items); heatBuffer = Math.max(0, nbt.getInt("HeatBuffer")); sanitize(); }
    private void sanitize() { for (int i = 0; i < items.size(); i++) { ItemStack s = items.get(i); if (!s.isEmpty() && !isValid(i, s)) items.set(i, ItemStack.EMPTY); else if (!s.isEmpty() && s.getCount() > 1) s.setCount(1); } }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); markDirty(); return r; }
    @Override public void setStack(int slot, ItemStack stack) { ItemStack toStore = ItemStack.EMPTY; if (stack != null && !stack.isEmpty() && isValid(slot, stack)) { toStore = stack.copy(); toStore.setCount(1); } items.set(slot, toStore); markDirty(); }
    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot >= 0 && slot < SLOT_COUNT && stack.isOf(ModItems.RTG_PELLET); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5) <= 64.0; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return ALL_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return items.get(slot).isEmpty() && isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot >= 0 && slot < SLOT_COUNT; }
    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.rt_heat_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new RTHeatGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
}
