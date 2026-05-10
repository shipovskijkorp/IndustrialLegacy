package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.ChargepadBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.registry.ModParticles;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Shared IC2-style charge pad implementation. */
public abstract class AbstractChargepadBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory, RedstoneModeCycleTarget {
    protected static final int SLOT_CHARGE = 0;
    protected static final int SLOT_DISCHARGE = 1;
    private static final int[] TOP_SLOTS = new int[]{SLOT_CHARGE};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_DISCHARGE};
    private static final int[] SIDE_SLOTS = new int[]{};

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private final int tier;
    private final long capacity;
    private final int outputEuT;
    private final long packet;
    private long energy;
    private int updateTicker;
    private byte redstoneMode;
    private int cachedRedstoneOut = -1;

    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, capacity);
                case 2 -> redstoneMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energy = Math.min(capacity, Math.max(0L, value));
                case 2 -> redstoneMode = (byte) Math.max(0, Math.min(1, value));
                default -> {
                }
            }
        }

        @Override
        public int size() {
            return 3;
        }
    };

    protected AbstractChargepadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int outputEuT, long capacity) {
        super(type, pos, state);
        this.tier = tier;
        this.outputEuT = outputEuT;
        this.capacity = capacity;
        this.packet = EuUtil.powerFromTier(tier);
    }

    protected abstract String getContainerTranslationKey();

    protected int getTickRate() {
        return 2;
    }

    public void serverTick() {
        if (this.world == null || this.world.isClient) return;

        chargeDischargeSlots();
        emit();

        if ((updateTicker++ % getTickRate()) != 0) {
            return;
        }

        List<PlayerEntity> players = getPlayersOnPad();
        boolean shouldBeActive = !players.isEmpty() && this.energy >= 1L;
        setActiveState(shouldBeActive);

        if (shouldBeActive) {
            chargePlayerInventory(players.get(0));
            markDirtyAndSync();
        }
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    public void clientTick() {
        if (this.world == null || !this.world.isClient) return;
        if (this.world.random.nextInt(8) != 0) return;
        if (!isActive()) return;

        for (int i = 0; i < 20; i++) {
            double x = this.pos.getX() + this.world.random.nextFloat();
            double y = this.pos.getY() + 0.9 + this.world.random.nextFloat();
            double z = this.pos.getZ() + this.world.random.nextFloat();
            this.world.addParticle(ModParticles.CHARGEPAD, x, y, z, 0.0, 0.1, 0.0);
        }
    }

    private List<PlayerEntity> getPlayersOnPad() {
        if (this.world == null) return List.of();
        Box box = new Box(this.pos.getX(), this.pos.getY(), this.pos.getZ(),
                this.pos.getX() + 1.0, this.pos.getY() + 1.25, this.pos.getZ() + 1.0);
        return this.world.getEntitiesByClass(PlayerEntity.class, box, player -> player != null && player.isAlive());
    }

    private void emit() {
        if (this.world == null) return;
        Direction out = getOutputSide();
        if (this.energy < packet) return;
        EuNetwork.route(this.world, this.pos, this, out, packet);
    }

    private Direction getOutputSide() {
        return this.getCachedState().get(ChargepadBlock.FACING);
    }

    private void chargeDischargeSlots() {
        ItemStack charge = items.get(SLOT_CHARGE);
        if (!charge.isEmpty() && charge.getCount() == 1 && ElectricItemManager.isElectric(charge)) {
            long maxMove = Math.min((long) outputEuT, ElectricItemManager.getTransferLimit(charge));
            long move = Math.min(Math.min(maxMove, energy), ElectricItemManager.getFree(charge));
            if (move > 0L) {
                long accepted = ElectricItemManager.charge(charge, move, false);
                if (accepted > 0L) {
                    energy -= accepted;
                    markDirtyAndSync();
                }
            }
        }

        ItemStack discharge = items.get(SLOT_DISCHARGE);
        if (!discharge.isEmpty() && discharge.getCount() == 1 && ElectricItemManager.isElectric(discharge)) {
            long maxMove = Math.min((long) outputEuT, ElectricItemManager.getTransferLimit(discharge));
            long move = Math.min(maxMove, Math.min(getEuFree(), ElectricItemManager.getEnergy(discharge)));
            if (move > 0L) {
                long extracted = ElectricItemManager.discharge(discharge, move, false);
                if (extracted > 0L) {
                    energy = Math.min(capacity, energy + extracted);
                    markDirtyAndSync();
                }
            }
        }
    }

    private void chargePlayerInventory(PlayerEntity player) {
        if (player == null) return;
        long perItemCap = (long) outputEuT * (long) getTickRate();

        for (ItemStack stack : player.getInventory().armor) {
            if (stack == null || stack.isEmpty()) continue;
            chargePlayerStack(stack, perItemCap);
        }
        for (ItemStack stack : player.getInventory().main) {
            if (stack == null || stack.isEmpty()) continue;
            chargePlayerStack(stack, perItemCap);
        }
    }

    private void chargePlayerStack(ItemStack stack, long perItemCap) {
        if (!ElectricItemManager.isElectric(stack) || stack.getCount() != 1 || perItemCap <= 0L || energy <= 0L) return;
        long move = Math.min(perItemCap, ElectricItemManager.getFree(stack));
        move = Math.min(move, energy);
        if (move <= 0L) return;

        long accepted = ElectricItemManager.charge(stack, move, false);
        if (accepted > 0L) {
            energy -= accepted;
        }
    }

    private void setActiveState(boolean active) {
        if (this.world == null) return;
        BlockState state = this.getCachedState();
        if (state.getBlock() instanceof ChargepadBlock && state.get(ChargepadBlock.ACTIVE) != active) {
            this.world.setBlockState(this.pos, state.with(ChargepadBlock.ACTIVE, active), Block.NOTIFY_ALL);
            markDirtyAndSync();
        }
        updateRedstoneOutput();
    }

    public boolean isActive() {
        return this.getCachedState().contains(ChargepadBlock.ACTIVE) && this.getCachedState().get(ChargepadBlock.ACTIVE);
    }

    public String getRedstoneModeTranslationKey() {
        return "il.chargepad.gui.mod.redstone" + redstoneMode;
    }

    public int getRedstoneOutputLevel() {
        return shouldEmitRedstone() ? 15 : 0;
    }

    private boolean shouldEmitRedstone() {
        return (redstoneMode == 0 && isActive()) || (redstoneMode == 1 && !isActive());
    }

    private void updateRedstoneOutput() {
        if (this.world == null) return;
        int out = getRedstoneOutputLevel();
        if (out == cachedRedstoneOut) return;
        cachedRedstoneOut = out;
        this.world.updateNeighborsAlways(this.pos, this.getCachedState().getBlock());
    }

    @Override
    public void cycleRedstoneMode(ServerPlayerEntity player) {
        redstoneMode++;
        if (redstoneMode >= 2) redstoneMode = 0;
        markDirtyAndSync();
        updateRedstoneOutput();
        if (player != null) {
            player.sendMessage(Text.translatable(getRedstoneModeTranslationKey()), false);
        }
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getContainerTranslationKey());
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public PropertyDelegate getGuiProperties() {
        return guiProps;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putByte("redstoneMode", redstoneMode);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.min(capacity, Math.max(0L, nbt.getLong("energy")));
        redstoneMode = (byte) Math.max(0, Math.min(1, nbt.getByte("redstoneMode")));
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) markDirtyAndSync();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirtyAndSync();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (!stack.isEmpty()) stack.setCount(1);
        items.set(slot, stack);
        markDirtyAndSync();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return ElectricItemManager.isElectric(stack) && (slot == SLOT_CHARGE || slot == SLOT_DISCHARGE);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    @Override
    public long getEuStored() {
        return energy;
    }

    @Override
    public long getEuCapacity() {
        return capacity;
    }

    @Override
    public int getSinkTier() {
        return tier;
    }

    @Override
    public int getSourceTier() {
        return tier;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0L || !canInsert(from)) return 0L;
        long accepted = Math.min(amount, getEuFree());
        if (!simulate && accepted > 0L) {
            energy += accepted;
            markDirtyAndSync();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0L || !canExtract(to)) return 0L;
        if (energy < packet) return 0L;
        long extracted = Math.min(Math.min(amount, packet), energy);
        if (!simulate && extracted > 0L) {
            energy -= extracted;
            markDirtyAndSync();
        }
        return extracted;
    }

    @Override
    public boolean canInsert(Direction from) {
        return from != Direction.UP && from != getOutputSide();
    }

    @Override
    public boolean canExtract(Direction to) {
        return to == getOutputSide();
    }

    @Override
    public boolean isFullEnergyOutput() {
        return true;
    }

    @Override
    public void setStoredEnergyFromItem(long amount) {
        this.energy = Math.min(capacity, Math.max(0L, amount));
        markDirtyAndSync();
    }

    private long getEuFree() {
        return Math.max(0L, capacity - energy);
    }
}
