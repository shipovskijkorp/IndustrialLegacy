package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.MagnetizerBlock;
import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractElectricMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.MagnetizerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
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

import java.util.EnumSet;
import java.util.Set;

public class MagnetizerBlockEntity extends AbstractElectricMachineBlockEntity {
    public static final int SLOT_DISCHARGE = 0;
    public static final int SLOT_UPGRADE_0 = 1;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = 5;

    public static final long DEFAULT_MAX_ENERGY = 100L;
    public static final int DEFAULT_TIER = 1;
    public static final double BOOST_ENERGY = 2.0D;
    public static final int MAX_FENCE_RANGE = 20;

    private double fractionalBoostEnergy = 0.0D;

    private final PropertyDelegate magnetizerProps = new PropertyDelegate() {
        @Override public int size() { return MagnetizerScreenHandler.PROP_COUNT; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> sinkTier;
                case 3 -> getMagnetizerRange();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = clampEnergy(value);
                case 2 -> sinkTier = Math.max(0, value);
                default -> { }
            }
        }
    };

    public MagnetizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGNETIZER, pos, state,
                INV_SIZE,
                DEFAULT_MAX_ENERGY,
                DEFAULT_TIER,
                1,
                SLOT_DISCHARGE,
                SLOT_UPGRADE_0,
                UPGRADE_SLOTS,
                new int[]{SLOT_DISCHARGE},
                new int[]{SLOT_DISCHARGE},
                new int[]{SLOT_DISCHARGE},
                new int[]{});
    }

    public static void tick(World world, BlockPos pos, BlockState state, MagnetizerBlockEntity be) {
        if (world.isClient) return;
        boolean changed = be.chargeFromDischargeSlot();
        changed |= be.tickUpgrades();
        if (changed) be.markDirty();
    }

    @Override
    protected Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
                UpgradableProperty.Augmentable,
                UpgradableProperty.RedstoneSensitive,
                UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage
        );
    }

    public boolean canBoost() {
        return energy >= BOOST_ENERGY;
    }

    public void boost(double multiplier) {
        if (multiplier <= 0.0D || energy <= 0L) return;

        double requested = BOOST_ENERGY * multiplier + fractionalBoostEnergy;
        long eu = (long) Math.floor(requested);
        fractionalBoostEnergy = requested - eu;

        if (eu <= 0L) return;

        energy = Math.max(0L, energy - eu);
        markDirty();
    }

    public Direction getFacing() {
        BlockState state = getCachedState();
        return state.contains(MagnetizerBlock.FACING) ? state.get(MagnetizerBlock.FACING) : Direction.NORTH;
    }

    public int getMagnetizerRange() {
        return MAX_FENCE_RANGE;
    }

    public PropertyDelegate getMagnetizerProps() {
        return magnetizerProps;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == SLOT_DISCHARGE) return super.isValid(slot, stack);
        if (isUpgradeSlot(slot)) return super.isValid(slot, stack);
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putDouble("fractionalBoostEnergy", fractionalBoostEnergy);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        fractionalBoostEnergy = nbt.contains("fractionalBoostEnergy") ? nbt.getDouble("fractionalBoostEnergy") : 0.0D;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.industrial_legacy.magnetizer");
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MagnetizerScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }
}
