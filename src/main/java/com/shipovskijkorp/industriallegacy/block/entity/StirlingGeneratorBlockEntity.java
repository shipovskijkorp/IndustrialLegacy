package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.StirlingGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.heat.IHeatSource;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.StirlingGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class StirlingGeneratorBlockEntity extends BlockEntity implements IEuEnergyStorage, ExtendedScreenHandlerFactory {
    private static final int MIN_SOURCE_TIER = 2;
    private final double euPerHeat;
    private long offeredEnergy;
    private long lastMaxEuOffered;
    private int lastHeatAvailable;
    private int lastEuProduced;
    private int sourceTier = MIN_SOURCE_TIER;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return StirlingGeneratorScreenHandler.PROP_COUNT; }
        @Override public int get(int index) { return switch (index) { case 0 -> lastHeatAvailable; case 1 -> lastEuProduced; case 2 -> (int)Math.min(Integer.MAX_VALUE, lastMaxEuOffered); default -> 0; }; }
        @Override public void set(int index, int value) { switch (index) { case 0 -> lastHeatAvailable = Math.max(0, value); case 1 -> lastEuProduced = Math.max(0, value); case 2 -> lastMaxEuOffered = Math.max(0, value); default -> { } } }
    };

    public StirlingGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STIRLING_GENERATOR, pos, state);
        this.euPerHeat = 0.5D * ILConfig.getFloat("balance/energy/generator/Stirling", 1.0f);
    }

    public static void tick(World world, BlockPos pos, BlockState state, StirlingGeneratorBlockEntity be) {
        if (world.isClient) return;
        be.convertAndEmitHeat(state);
    }

    private void convertAndEmitHeat(BlockState state) {
        offeredEnergy = 0L;
        lastMaxEuOffered = 0L;
        lastHeatAvailable = 0;
        lastEuProduced = 0;
        sourceTier = MIN_SOURCE_TIER;

        if (world == null || euPerHeat <= 0.0D || !state.contains(StirlingGeneratorBlock.FACING)) {
            setLit(false);
            return;
        }

        Direction facing = state.get(StirlingGeneratorBlock.FACING);
        BlockEntity sourceBe = world.getBlockEntity(pos.offset(facing));
        if (!(sourceBe instanceof IHeatSource source)) {
            setLit(false);
            return;
        }

        Direction sideFromSource = facing.getOpposite();
        int bandwidth = source.getConnectionBandwidth(sideFromSource);
        if (bandwidth <= 0) { setLit(false); return; }

        int availableHeat = source.drawHeat(sideFromSource, bandwidth, true);
        lastHeatAvailable = Math.max(0, availableHeat);
        if (lastHeatAvailable <= 0) { setLit(false); return; }

        double maxProduction = (double) lastHeatAvailable * euPerHeat;
        long maxEu = maxProduction <= 0.0D ? 0L : (long) Math.floor(maxProduction);
        lastMaxEuOffered = maxEu;
        sourceTier = Math.max(EuUtil.tierFromPower(maxProduction), MIN_SOURCE_TIER);
        setLit(maxProduction > 0.0D);
        if (maxEu <= 0L) return;

        offeredEnergy = maxEu;
        long spentTotal = 0L;
        for (Direction dir : Direction.values()) {
            if (offeredEnergy <= 0L) break;
            if (dir == facing) continue;
            long spent = EuNetwork.route(world, pos, this, dir, offeredEnergy);
            spentTotal += Math.max(0L, spent);
        }

        if (spentTotal > 0L) {
            int heatToDraw = (int) Math.ceil((double) spentTotal / euPerHeat);
            source.drawHeat(sideFromSource, heatToDraw, false);
            lastEuProduced = (int) Math.min(Integer.MAX_VALUE, spentTotal);
            markDirty();
        }
        offeredEnergy = 0L;
    }

    private void setLit(boolean lit) { if (world == null) return; BlockState state = getCachedState(); if (state.contains(StirlingGeneratorBlock.LIT) && state.get(StirlingGeneratorBlock.LIT) != lit) world.setBlockState(pos, state.with(StirlingGeneratorBlock.LIT, lit), Block.NOTIFY_ALL); }
    public PropertyDelegate getGuiProperties() { return props; }
    @Override protected void writeNbt(NbtCompound nbt) { super.writeNbt(nbt); nbt.putInt("lastHeatAvailable", lastHeatAvailable); nbt.putInt("lastEuProduced", lastEuProduced); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); lastHeatAvailable = Math.max(0, nbt.getInt("lastHeatAvailable")); lastEuProduced = Math.max(0, nbt.getInt("lastEuProduced")); }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.stirling_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new StirlingGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
    @Override public long getEuStored() { return offeredEnergy; }
    @Override public long getEuCapacity() { return lastMaxEuOffered; }
    @Override public int getSinkTier() { return MIN_SOURCE_TIER; }
    @Override public int getSourceTier() { return sourceTier; }
    @Override public long insertEu(long amount, Direction from, boolean simulate) { return 0L; }
    @Override public long extractEu(long amount, Direction to, boolean simulate) { if (!canExtract(to)) return 0L; long extracted = Math.min(Math.max(0L, amount), offeredEnergy); if (!simulate && extracted > 0L) offeredEnergy -= extracted; return extracted; }
    @Override public boolean canInsert(Direction from) { return false; }
    @Override public boolean canExtract(Direction to) { BlockState state = getCachedState(); return !state.contains(StirlingGeneratorBlock.FACING) || to != state.get(StirlingGeneratorBlock.FACING); }
    @Override public double getOfferedEnergy() { return offeredEnergy; }
    @Override public double getOfferedEnergy(Direction to) { return canExtract(to) ? offeredEnergy : 0.0D; }
}
