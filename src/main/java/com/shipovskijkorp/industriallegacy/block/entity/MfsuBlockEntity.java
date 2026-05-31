package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.screen.MfsuScreenHandler;
import com.shipovskijkorp.industriallegacy.block.MfsuBlock;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricSlotHelper;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerEntity;

/**
 * IL Exp-style MFSU (tier 4, 2048 EU/t output, 40 000 000 EU capacity).
 *
 * This is a minimal implementation for now:
 * - internal buffer
 * - output on the facing side
 * - input on all other sides
 * - 1 packet per tick (full packets only, like IL's "fullEnergy" behavior)
 */
public class MfsuBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory, RedstoneModeCycleTarget {
    // Inventory layout (IL-ish):
    // 0 = charge (top)
    // 1 = discharge (bottom)
    private static final int SLOT_CHARGE = 0;
    private static final int SLOT_DISCHARGE = 1;
    private static final int[] TOP_SLOTS = new int[] { SLOT_CHARGE };
    private static final int[] BOTTOM_SLOTS = new int[] { SLOT_DISCHARGE };
    private static final int[] SIDE_SLOTS = new int[] {};

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);

    private final int tier = 4;
    private final long capacity = 40000000L;
    private final int outputEuT = 2048;
    private final long packet = EuUtil.powerFromTier(tier); // 512

    private long energy = 0L;

    // IL: 0..6, total 7 modes.
    public byte redstoneMode = 0;
    public static final byte REDSTONE_MODES = 7;

    private int cachedRedstoneOut = -1;

    // GUI sync (euStored, euCap, outputEUt, redstoneMode)
    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, MfsuBlockEntity.this.energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, MfsuBlockEntity.this.capacity);
                case 2 -> MfsuBlockEntity.this.outputEuT;
                case 3 -> MfsuBlockEntity.this.redstoneMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> MfsuBlockEntity.this.energy = Math.min(MfsuBlockEntity.this.capacity, Math.max(0L, value));
                case 3 -> MfsuBlockEntity.this.redstoneMode = (byte) Math.max(0, Math.min(REDSTONE_MODES - 1, value));
                default -> {
                }
            }
        }

        @Override
        public int size() {
            return MfsuScreenHandler.PROP_COUNT;
        }
    };

    public MfsuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MFSU, pos, state);
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

    public static void tick(World world, BlockPos pos, BlockState state, MfsuBlockEntity be) {
        if (world.isClient) return;

        // Charge/discharge items (slots).
        be.chargeDischargeItems();

        // IL-style: output may be disabled by redstone mode.
        be.emit();

        // Redstone output (modes 1..4) depends on energy level; update neighbors only when it changes.
        be.updateRedstoneOutput();
    }

    private void emit() {
        if (world == null) return;

        if (!shouldEmitEnergy()) return;

        Direction out = getCachedState().get(MfsuBlock.FACING);
        if (out == null) return;

        // IL "fullEnergy" behavior: only offer energy if we can send a full packet.
        if (energy < packet) return;

        // Try direct neighbor first, otherwise route through cable blocks.
        EuNetwork.route(world, pos, this, out, packet);
    }

    /** Mirrors IL TileEntityElectricBlock.shouldEmitEnergy(). */
    private boolean shouldEmitEnergy() {
        if (world == null) return true;
        boolean hasRedstone = world.isReceivingRedstonePower(pos);
        if (redstoneMode == 5) {
            return !hasRedstone;
        }
        if (redstoneMode == 6) {
            // Do not output unless full when powered.
            return !hasRedstone || energy > (capacity - (double) outputEuT * 20.0);
        }
        return true;
    }

    /** Mirrors IL TileEntityElectricBlock.shouldEmitRedstone(). */
    public boolean shouldEmitRedstone() {
        return switch (redstoneMode) {
            case 1 -> energy >= capacity - (double) outputEuT * 20.0;
            case 2 -> energy > outputEuT && energy < capacity - (double) outputEuT;
            case 3 -> energy < capacity - (double) outputEuT;
            case 4 -> energy < outputEuT;
            default -> false;
        };
    }

    public int getRedstoneOutputLevel() {
        return shouldEmitRedstone() ? 15 : 0;
    }

    private void updateRedstoneOutput() {
        if (world == null) return;
        int out = getRedstoneOutputLevel();
        if (out == cachedRedstoneOut) return;
        cachedRedstoneOut = out;
        world.updateNeighborsAlways(pos, getCachedState().getBlock());
    }

    private long getEuFree() {
        return Math.max(0L, capacity - energy);
    }


    private void chargeDischargeItems() {
        if (world == null) return;

        // Slot 0: charge (storage -> item), IL InvSlotCharge.
        ItemStack charge = items.get(SLOT_CHARGE);
        long charged = ElectricSlotHelper.chargeFromStorage(charge, energy, tier, false);
        if (charged > 0L) {
            energy -= charged;
            markDirtyAndSync();
        }

        // Slot 1: discharge (item/single-use energy item -> storage), IL InvSlotDischarge.
        ItemStack discharge = items.get(SLOT_DISCHARGE);
        long extracted = ElectricSlotHelper.dischargeIntoStorage(discharge, getEuFree(), tier, true, false);
        if (extracted > 0L) {
            energy = Math.min(capacity, energy + extracted);
            markDirtyAndSync();
        }
    }


    // --- Saving / loading ---
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
        redstoneMode = nbt.getByte("redstoneMode");
        if (redstoneMode < 0 || redstoneMode >= REDSTONE_MODES) redstoneMode = 0;
    }

    // --- Inventory ---
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public int getMaxCountPerStack() {
        // IL BatBox slots effectively hold one electric item at a time.
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
    public boolean isValid(int slot, ItemStack stack) {
        if (slot == SLOT_CHARGE) return ElectricSlotHelper.canCharge(stack, tier);
        if (slot == SLOT_DISCHARGE) return ElectricSlotHelper.canDischarge(stack, tier, true);
        return false;
    }

    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirtyAndSync();
    }

    @Override
    public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        if (world == null) return false;
        if (world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
    }

    // --- Sided inventory rules (IL-like) ---
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (!isValid(slot, stack)) return false;
        // IL TileEntityElectricBlock: charge slot prefers UP, discharge slot prefers BOTTOM.
        if (slot == SLOT_CHARGE) return dir == null || dir == Direction.UP;
        if (slot == SLOT_DISCHARGE) return dir == null || dir == Direction.DOWN;
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        if (slot == SLOT_CHARGE) return dir == Direction.UP;
        if (slot == SLOT_DISCHARGE) return dir == Direction.DOWN;
        return false;
    }

    // --- EU storage ---
    @Override
    public void setStoredEnergyFromItem(long amount) {
        this.energy = Math.max(0L, Math.min(this.capacity, amount));
        markDirtyAndSync();
    }

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
    public boolean isFullEnergyOutput() {
        // IL storage blocks emit full packets only.
        return true;
    }

    @Override
    public boolean canInsert(Direction from) {
        Direction out = getCachedState().get(MfsuBlock.FACING);
        return from != out;
    }

    @Override
    public boolean canExtract(Direction to) {
        Direction out = getCachedState().get(MfsuBlock.FACING);
        return to == out;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        if (amount <= 0) return 0;
        if (!canInsert(from)) return 0;

        long accepted = Math.min(amount, getEuFree());
        if (!simulate && accepted > 0) {
            energy += accepted;
            markDirtyAndSync();
        }
        return accepted;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0) return 0;
        if (!canExtract(to)) return 0;

        // IL "fullEnergy" behavior:
        // - Only start emitting when we have at least one full tier packet available.
        // - But the EnergyNet may still draw a *partial* amount (e.g. topping off a nearly-full sink).
        //   In that case, we allow extracting <= one packet, as long as we had >= one packet to begin with.
        if (energy < packet) return 0;

        long extracted = Math.min(Math.min(amount, packet), energy);
        if (extracted <= 0) return 0;
        if (!simulate) {
            energy -= extracted;
            markDirtyAndSync();
        }
        return extracted;
    }

    public PropertyDelegate getGuiProperties() {
        return guiProps;
    }

    public String getRedstoneModeTranslationKey() {
        return "il.EUStorage.gui.mod.redstone" + redstoneMode;
    }

    public void cycleRedstoneMode(@Nullable ServerPlayerEntity player) {
        redstoneMode++;
        if (redstoneMode >= REDSTONE_MODES) redstoneMode = 0;
        markDirtyAndSync();
        updateRedstoneOutput();
        if (player != null) {
            player.sendMessage(Text.translatable(getRedstoneModeTranslationKey()), false);
        }
    }


// --- GUI / menu ---

@Override
public Text getDisplayName() {
    return Text.translatable("block.industrial_legacy.mfsu");
}

@Override
public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
    buf.writeBlockPos(this.pos);
}

@Override
public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
    return new com.shipovskijkorp.industriallegacy.screen.MfsuScreenHandler(syncId, inv, this);
}
}