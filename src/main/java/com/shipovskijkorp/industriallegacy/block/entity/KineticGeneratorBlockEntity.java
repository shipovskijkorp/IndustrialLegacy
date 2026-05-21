package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.KineticGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.kinetic.IKineticSource;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.KineticGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
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

public class KineticGeneratorBlockEntity extends BlockEntity implements Inventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    private static final long CAPACITY = 10_000L;
    private static final int TIER = 2;

    private long energy;
    private int lastKuAvailable;
    private int lastKuDrawn;
    private int lastEuProduced;
    private final double euPerKu;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return KineticGeneratorScreenHandler.PROP_COUNT; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, CAPACITY);
                case 2 -> lastKuAvailable;
                case 3 -> lastEuProduced;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.max(0L, Math.min(CAPACITY, value));
                case 2 -> lastKuAvailable = Math.max(0, value);
                case 3 -> lastEuProduced = Math.max(0, value);
                default -> {}
            }
        }
    };

    public KineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KINETIC_GENERATOR, pos, state);
        this.euPerKu = 0.25D * Math.max(0.0D, ILConfig.getFloat("balance/energy/generator/Kinetic", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, KineticGeneratorBlockEntity be) {
        if (world.isClient) return;
        be.convertKineticEnergy(state);
        be.emitToNeighbors(state);
        be.setLit(be.lastEuProduced > 0 || be.energy > 0);
    }

    private void convertKineticEnergy(BlockState state) {
        lastKuAvailable = 0;
        lastKuDrawn = 0;
        lastEuProduced = 0;
        if (world == null || euPerKu <= 0.0D || energy >= CAPACITY || !state.contains(KineticGeneratorBlock.FACING)) {
            return;
        }

        Direction facing = state.get(KineticGeneratorBlock.FACING);
        BlockEntity sourceBe = world.getBlockEntity(pos.offset(facing));
        if (!(sourceBe instanceof IKineticSource source)) {
            return;
        }

        Direction sideFromSource = facing.getOpposite();
        int bandwidth = Math.max(0, source.getConnectionBandwidth(sideFromSource));
        if (bandwidth <= 0) {
            return;
        }

        int offeredKu = Math.max(0, source.drawKineticEnergy(sideFromSource, bandwidth, true));
        lastKuAvailable = offeredKu;
        if (offeredKu <= 0) {
            return;
        }

        long free = Math.max(0L, CAPACITY - energy);
        int maxKuToDraw = Math.min(offeredKu, (int) Math.ceil((double) free / euPerKu));
        if (maxKuToDraw <= 0) {
            return;
        }

        int drawn = Math.max(0, source.drawKineticEnergy(sideFromSource, maxKuToDraw, false));
        long produced = Math.min(free, (long) Math.floor((double) drawn * euPerKu));
        if (produced > 0L) {
            energy += produced;
            lastKuDrawn = drawn;
            lastEuProduced = (int) Math.min(Integer.MAX_VALUE, produced);
            markDirty();
        }
    }

    private void emitToNeighbors(BlockState state) {
        if (world == null || energy <= 0L) return;
        Direction inputSide = state.contains(KineticGeneratorBlock.FACING) ? state.get(KineticGeneratorBlock.FACING) : Direction.NORTH;
        long remaining = Math.min(energy, EuUtil.powerFromTier(TIER));
        for (Direction dir : Direction.values()) {
            if (remaining <= 0L) break;
            if (dir == inputSide) continue;
            long spent = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= spent;
        }
    }

    private void setLit(boolean lit) {
        if (world == null) return;
        BlockState state = getCachedState();
        if (!state.contains(KineticGeneratorBlock.LIT) || state.get(KineticGeneratorBlock.LIT) == lit) return;
        world.setBlockState(pos, state.with(KineticGeneratorBlock.LIT, lit), Block.NOTIFY_ALL);
    }

    public PropertyDelegate getGuiProperties() { return props; }
    public int getLastKuAvailable() { return lastKuAvailable; }
    public int getLastKuDrawn() { return lastKuDrawn; }
    public int getLastEuProduced() { return lastEuProduced; }

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("energy", energy);
        nbt.putInt("lastKuAvailable", lastKuAvailable);
        nbt.putInt("lastKuDrawn", lastKuDrawn);
        nbt.putInt("lastEuProduced", lastEuProduced);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        energy = Math.max(0L, Math.min(CAPACITY, nbt.getLong("energy")));
        lastKuAvailable = Math.max(0, nbt.getInt("lastKuAvailable"));
        lastKuDrawn = Math.max(0, nbt.getInt("lastKuDrawn"));
        lastEuProduced = Math.max(0, nbt.getInt("lastEuProduced"));
    }

    @Override public int size() { return 0; }
    @Override public boolean isEmpty() { return true; }
    @Override public ItemStack getStack(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeStack(int slot, int amount) { return ItemStack.EMPTY; }
    @Override public ItemStack removeStack(int slot) { return ItemStack.EMPTY; }
    @Override public void setStack(int slot, ItemStack stack) {}
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D; }
    @Override public void clear() {}

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.kinetic_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new KineticGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    @Override public long getEuStored() { return energy; }
    @Override public long getEuCapacity() { return CAPACITY; }
    @Override public int getSinkTier() { return TIER; }
    @Override public int getSourceTier() { return TIER; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { return 0L; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) {
        if (!canExtract(to)) return 0L;
        long extracted = Math.min(Math.max(0L, amount), energy);
        if (!simulate && extracted > 0L) {
            energy -= extracted;
            markDirty();
        }
        return extracted;
    }
    @Override public boolean canInsert(Direction from) { return false; }
    @Override public boolean canExtract(Direction to) {
        BlockState state = getCachedState();
        return !state.contains(KineticGeneratorBlock.FACING) || to != state.get(KineticGeneratorBlock.FACING);
    }
}
