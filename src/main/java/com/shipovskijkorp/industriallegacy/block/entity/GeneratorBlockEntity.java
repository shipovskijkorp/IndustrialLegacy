package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.GeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.EuUtil;
import com.shipovskijkorp.industriallegacy.energy.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.Block;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import com.shipovskijkorp.industriallegacy.screen.GeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerEntity;

/**
 * IC2-like Generator tile.
 *
 * Matches the important parts of IC2 1.12.2 logic:
 * - production = round(10 * balance/energy/generator/generator)
 * - internal buffer = 4000 EU
 * - tier = 1 (packet size 32 EU)
 * - fuel ticks = (vanilla burn time) / 4
 * - burns fuel while fuel>0 (even if the last tick doesn't fully fit into storage)
 */
public class GeneratorBlockEntity extends BlockEntity implements SidedInventory, IEuEnergyStorage, ExtendedScreenHandlerFactory {
    // Inventory layout (kept IC2-ish):
    // 0 = charge (not yet used, but reserved for later electric items)
    // 1 = fuel
    /** Slot indices are public so blocks/menus can interact without reflection. */
    public static final int SLOT_CHARGE = 0;
    public static final int SLOT_FUEL = 1;
    private static final int[] TOP_SLOTS = new int[] { SLOT_CHARGE };
    private static final int[] SIDE_SLOTS = new int[] { SLOT_FUEL };
    private static final int[] BOTTOM_SLOTS = new int[] {};

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);

    // EU
    private final int tier = 1;
    private final long capacity = 4000L;
    private long energy = 0L;

    // burn state
    public int fuel = 0;
    public int totalFuel = 0;

    // GUI sync (euStored, euCap, fuel, fuelMax)
    private final PropertyDelegate guiProps = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, GeneratorBlockEntity.this.energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, GeneratorBlockEntity.this.capacity);
                case 2 -> GeneratorBlockEntity.this.fuel;
                case 3 -> GeneratorBlockEntity.this.totalFuel;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> GeneratorBlockEntity.this.energy = Math.min(GeneratorBlockEntity.this.capacity, Math.max(0L, value));
                case 2 -> GeneratorBlockEntity.this.fuel = Math.max(0, value);
                case 3 -> GeneratorBlockEntity.this.totalFuel = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int size() {
            return GeneratorScreenHandler.PROP_COUNT;
        }
    };

    private final long production;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR, pos, state);
        float factor = ILConfig.getFloat("balance/energy/generator/generator", 1.0f);
        this.production = Math.max(0L, Math.round(10.0f * factor));
    }

    /**
     * IC2 generator fuel logic:
     * vanilla burn time / 4 (so coal 1600 -> 400 ticks -> 4000 EU at 10 EU/t).
     */
    public static int getFuelTicksForStack(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // IC2 generator disallows lava by default.
        if (stack.isOf(Items.LAVA_BUCKET)) return 0;

        Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
        if (burnTime == null || burnTime <= 0) return 0;

        return burnTime / 4;
    }

    public static boolean isValidFuel(ItemStack stack) {
        return getFuelTicksForStack(stack) > 0;
    }

    public static void tick(World world, BlockPos pos, BlockState state, GeneratorBlockEntity be) {
        if (world.isClient) return;

        boolean invChanged = false;

        if (be.needsFuel()) {
            invChanged = be.gainFuel();
        }

        boolean active = be.gainEnergy();

        // Very simple direct-adjacency output (temporary until full cable/grid net exists).
        be.emitToNeighbors();

        if (invChanged) {
            be.markDirty();
        }

        be.setLit(active);
    }

    private boolean needsFuel() {
        return fuel <= 0 && getEuFree() >= production;
    }

    /** Equivalent of TileEntityGenerator.gainFuel(). */
    private boolean gainFuel() {
        ItemStack stack = items.get(SLOT_FUEL);
        if (stack.isEmpty()) return false;

        int fuelValue = getFuelTicksForStack(stack);
        if (fuelValue <= 0) return false;

        // consume 1
        ItemStack remainder = stack.getRecipeRemainder();
        stack.decrement(1);
        if (stack.isEmpty() && !remainder.isEmpty()) {
            items.set(SLOT_FUEL, remainder.copy());
        }

        this.fuel += fuelValue;
        this.totalFuel = fuelValue;
        return true;
    }

    /** Equivalent of TileEntityBaseGenerator.gainEnergy() with TileEntityGenerator.isConverting(). */
    private boolean gainEnergy() {
        if (fuel > 0 && production > 0) {
            // energy.addEnergy(production)
            long add = Math.min(getEuFree(), production);
            this.energy += add;
            this.fuel--;
            return true;
        }
        return false;
    }

    private long getEuFree() {
        return Math.max(0L, capacity - energy);
    }

    private void emitToNeighbors() {
        if (this.world == null) return;
        if (this.energy <= 0) return;

        long packet = Math.min(this.energy, EuUtil.powerFromTier(this.tier));
        if (packet <= 0) return;

        long remaining = packet;
        for (Direction dir : Direction.values()) {
            if (remaining <= 0) break;
            long moved = EuNetwork.route(world, pos, this, dir, remaining);
            remaining -= moved;
        }
    }

    private void setLit(boolean lit) {
        if (this.world == null) return;
        BlockState state = getCachedState();
        if (!state.contains(GeneratorBlock.LIT)) return;
        if (state.get(GeneratorBlock.LIT) == lit) return;

        world.setBlockState(pos, state.with(GeneratorBlock.LIT, lit), Block.NOTIFY_ALL);
    }

    // --- Saving / loading ---
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putLong("energy", energy);
        nbt.putInt("fuel", fuel);
        nbt.putInt("totalFuel", totalFuel);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        energy = Math.min(capacity, Math.max(0L, nbt.getLong("energy")));
        fuel = Math.max(0, nbt.getInt("fuel"));
        totalFuel = Math.max(0, nbt.getInt("totalFuel"));
    }

    // --- Inventory ---
    @Override
    public int size() {
        return items.size();
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
        if (!result.isEmpty()) markDirty();
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(items, slot);
        markDirty();
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
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

    // --- Sided inventory rules (IC2-like) ---
    @Override
    public int[] getAvailableSlots(Direction side) {
        if (side == Direction.UP) return TOP_SLOTS;
        if (side == Direction.DOWN) return BOTTOM_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        if (slot == SLOT_FUEL) {
            // fuel from sides only
            if (dir == null || dir == Direction.UP || dir == Direction.DOWN) return false;
            Integer burnTime = FuelRegistry.INSTANCE.get(stack.getItem());
            return burnTime != null && burnTime > 0 && !stack.isOf(Items.LAVA_BUCKET);
        }
        if (slot == SLOT_CHARGE) {
            // reserve for later electric items
            return dir == Direction.UP;
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        // allow taking charge item from top
        if (slot == SLOT_CHARGE) {
            return dir == Direction.UP;
        }
        return false;
    }

    // --- EU storage ---
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
        return 0; // generator is a source only
    }

    @Override
    public int getSourceTier() {
        return tier; // emits to all sides
    }

    @Override
    public boolean canInsert(Direction from) {
        return false;
    }

    @Override
    public boolean canExtract(Direction to) {
        return true;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        return 0; // source-only
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0) return 0;
        if (energy <= 0) return 0;
        // Single-packet source like IC2: at most one packet per extraction.
        long maxPacket = EuUtil.powerFromTier(this.tier);
        long extracted = Math.min(Math.min(amount, maxPacket), energy);
        if (!simulate) {
            energy -= extracted;
            markDirty();
        }
        return extracted;
    }

    // --- Getters for debug / future GUI ---
    public int getFuel() {
        return fuel;
    }

    public int getTotalFuel() {
        return totalFuel;
    }

    public long getProduction() {
        return production;
    }

    public PropertyDelegate getGuiProperties() {
        return guiProps;
    }


// --- GUI / menu ---

@Override
public Text getDisplayName() {
    return Text.translatable("block.industrial_legacy.generator");
}

@Override
public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
    buf.writeBlockPos(this.pos);
}

@Override
public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
    return new GeneratorScreenHandler(syncId, inv, this);
}
}