package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.NuclearReactorBlock;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.screen.NuclearReactorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NuclearReactorBlockEntity extends BlockEntity implements SidedInventory, ExtendedScreenHandlerFactory, IReactor, IEuEnergyStorage {
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
    private int updateTicker = -1;
    private long emitBudgetEu;
    private long emitBudgetTick = Long.MIN_VALUE;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> maxHeat;
                case 2 -> emittedHeat;
                case 3 -> cachedSize;
                case 4 -> Math.round(getOfferedEnergyEuPerTick() * 10.0f);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> heat = value;
                case 1 -> maxHeat = value;
                case 2 -> emittedHeat = value;
                case 3 -> cachedSize = value;
                case 4 -> output = value / 50.0f;
            }
        }
    };

    public NuclearReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUCLEAR_REACTOR, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, NuclearReactorBlockEntity be) {
        if (world.isClient) return;

        if (be.updateTicker < 0) {
            be.updateTicker = world.random.nextInt(be.getTickRate());
        }

        boolean processCycle = be.updateTicker++ % be.getTickRate() == 0;
        if (processCycle) {
            int oldSize = be.cachedSize;
            be.cachedSize = be.getReactorSize();
            if (oldSize != be.cachedSize) {
                be.dropUnfittingStuff();
            }
            be.sanitizeInventory();

            be.output = 0.0f;
            be.maxHeat = 10000;
            be.heatEffectModifier = 1.0f;
            be.emittedHeat = 0;

            be.processChambers(true);
            be.processChambers(false);

            if (be.calculateHeatEffects()) {
                return;
            }

            boolean active = be.heat >= 1000 || be.output > 0.0f;
            if (state.get(NuclearReactorBlock.LIT) != active) {
                world.setBlockState(pos, state.with(NuclearReactorBlock.LIT, active), 3);
            }

            if (be.heat >= be.maxHeat) {
                be.explode();
                return;
            }

            be.markDirty();
        }

        be.emitToNeighbors();
    }

    public static boolean isAllowedReactorItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IReactorComponent;
    }

    private boolean isUsefulItem(ItemStack stack) {
        return isAllowedReactorItem(stack)
                && stack.getItem() instanceof IReactorComponent component
                && component.canBePlacedIn(stack, this);
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

    private float getOfferedEnergyEuPerTick() {
        return isFluidCooled() ? 0.0f : output * 5.0f;
    }

    private void resetEmitBudgetForCurrentTick() {
        if (world == null) return;
        long time = world.getTime();
        if (emitBudgetTick != time) {
            emitBudgetTick = time;
            emitBudgetEu = Math.max(0L, (long) Math.floor(getOfferedEnergyEuPerTick()));
        }
    }

    private void emitToNeighbors() {
        if (world == null || world.isClient || isFluidCooled()) return;

        resetEmitBudgetForCurrentTick();
        if (emitBudgetEu <= 0L) return;

        emitFromPosition(pos);

        for (Direction chamberDir : Direction.values()) {
            BlockPos chamberPos = pos.offset(chamberDir);
            if (world.getBlockState(chamberPos).isOf(ModBlocks.REACTOR_CHAMBER)) {
                emitFromPosition(chamberPos);
            }
        }
    }

    private void emitFromPosition(BlockPos sourcePos) {
        for (Direction dir : Direction.values()) {
            if (emitBudgetEu <= 0L) break;
            EuNetwork.route(world, sourcePos, this, dir, emitBudgetEu);
        }
    }

    public int getReactorSize() {
        if (world == null) return COLUMNS;
        int cols = 3;
        for (Direction dir : Direction.values()) {
            if (world.getBlockState(pos.offset(dir)).isOf(ModBlocks.REACTOR_CHAMBER)) {
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

    private void sanitizeInventory() {
        if (world == null || world.isClient) return;

        boolean changed = false;
        int activeSize = getReactorSize();

        for (int slot = 0; slot < items.size(); slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;

            int x = slot % COLUMNS;
            if (x >= activeSize || !isUsefulItem(stack)) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                items.set(slot, ItemStack.EMPTY);
                changed = true;
                continue;
            }

            if (stack.getCount() > 1) {
                ItemStack extra = stack.copy();
                extra.setCount(stack.getCount() - 1);
                stack.setCount(1);
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, extra);
                changed = true;
            }
        }

        if (changed) {
            markDirty();
        }
    }

    private boolean calculateHeatEffects() {
        if (!(world instanceof ServerWorld serverWorld) || heat < 4000) {
            return false;
        }

        float power = heat / (float) Math.max(1, maxHeat);
        if (power >= 1.0f) {
            explode();
            return true;
        }

        if (power >= 0.85f && serverWorld.random.nextFloat() <= 0.2f * heatEffectModifier) {
            BlockPos target = getRandomNearbyPos(serverWorld, 2);
            if (target != null) {
                BlockState state = serverWorld.getBlockState(target);
                if (state.isAir()) {
                    serverWorld.setBlockState(target, Blocks.FIRE.getDefaultState());
                } else if (state.getHardness(serverWorld, target) >= 0.0f && serverWorld.getBlockEntity(target) == null) {
                    if (shouldMeltToLava(state)) {
                        serverWorld.setBlockState(target, Blocks.LAVA.getDefaultState());
                    } else {
                        serverWorld.setBlockState(target, Blocks.FIRE.getDefaultState());
                    }
                }
            }
        }

        if (power >= 0.7f) {
            List<LivingEntity> nearby = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    new Box(pos).expand(3.0),
                    entity -> true
            );
            for (LivingEntity entity : nearby) {
                float damage = serverWorld.random.nextInt(4) * heatEffectModifier;
                if (damage > 0.0f) {
                    entity.damage(entity.getDamageSources().magic(), damage);
                }
            }
        }

        if (power >= 0.5f && serverWorld.random.nextFloat() <= heatEffectModifier) {
            BlockPos target = getRandomNearbyPos(serverWorld, 2);
            if (target != null && serverWorld.getBlockState(target).getFluidState().isIn(FluidTags.WATER)) {
                serverWorld.removeBlock(target, false);
            }
        }

        if (power >= 0.4f && serverWorld.random.nextFloat() <= heatEffectModifier) {
            BlockPos target = getRandomNearbyPos(serverWorld, 2);
            if (target != null && serverWorld.getBlockEntity(target) == null) {
                BlockState state = serverWorld.getBlockState(target);
                if (state.isBurnable()) {
                    serverWorld.setBlockState(target, Blocks.FIRE.getDefaultState());
                }
            }
        }

        return false;
    }

    private @Nullable BlockPos getRandomNearbyPos(ServerWorld serverWorld, int radius) {
        if (radius <= 0) return null;

        BlockPos target;
        do {
            target = pos.add(
                    serverWorld.random.nextBetween(-radius, radius),
                    serverWorld.random.nextBetween(-radius, radius),
                    serverWorld.random.nextBetween(-radius, radius)
            );
        } while (target.equals(pos));

        return target;
    }

    private boolean shouldMeltToLava(BlockState state) {
        if (isProtectedFromLavaMelt(state)) {
            return false;
        }

        return state.isIn(BlockTags.PICKAXE_MINEABLE)
                || state.isIn(BlockTags.DIRT)
                || state.isOf(Blocks.CLAY)
                || state.isOf(Blocks.TERRACOTTA)
                || state.isOf(Blocks.MAGMA_BLOCK);
    }

    private boolean isProtectedFromLavaMelt(BlockState state) {
        String namespace = Registries.BLOCK.getId(state.getBlock()).getNamespace();
        if ("industrial_legacy".equals(namespace)) {
            return true;
        }

        if (state.isOf(Blocks.REDSTONE_WIRE)
                || state.isOf(Blocks.REDSTONE_TORCH)
                || state.isOf(Blocks.REDSTONE_WALL_TORCH)
                || state.isOf(Blocks.REPEATER)
                || state.isOf(Blocks.COMPARATOR)
                || state.isOf(Blocks.REDSTONE_BLOCK)
                || state.isOf(Blocks.REDSTONE_LAMP)
                || state.isOf(Blocks.OBSERVER)
                || state.isOf(Blocks.TARGET)
                || state.isOf(Blocks.DAYLIGHT_DETECTOR)
                || state.isOf(Blocks.SCULK_SENSOR)
                || state.isOf(Blocks.CALIBRATED_SCULK_SENSOR)
                || state.isOf(Blocks.TRIPWIRE)
                || state.isOf(Blocks.TRIPWIRE_HOOK)
                || state.isOf(Blocks.DISPENSER)
                || state.isOf(Blocks.DROPPER)
                || state.isOf(Blocks.PISTON)
                || state.isOf(Blocks.STICKY_PISTON)
                || state.isOf(Blocks.MOVING_PISTON)
                || state.isOf(Blocks.LECTERN)
                || state.isOf(Blocks.NOTE_BLOCK)
                || state.isIn(BlockTags.BUTTONS)
                || state.isIn(BlockTags.PRESSURE_PLATES)) {
            return true;
        }

        return state.contains(Properties.POWERED)
                || state.contains(Properties.LIT)
                || state.emitsRedstonePower();
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
        ItemStack toStore = ItemStack.EMPTY;
        if (stack != null && !stack.isEmpty() && isValid(slot, stack)) {
            toStore = stack.copy();
            toStore.setCount(1);
        }
        items.set(slot, toStore);
        markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
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
        return items.get(slot).isEmpty() && isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) { return true; }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isSlotEnabled(slot) && isUsefulItem(stack);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        heat = nbt.getInt("Heat");
        maxHeat = nbt.getInt("MaxHeat");
        emittedHeat = nbt.getInt("EmitHeat");
        cachedSize = Math.max(3, nbt.getInt("Size"));
        updateTicker = nbt.contains("UpdateTicker") ? nbt.getInt("UpdateTicker") : -1;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getCount() > 1) {
                stack.setCount(1);
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putInt("Heat", heat);
        nbt.putInt("MaxHeat", maxHeat);
        nbt.putInt("EmitHeat", emittedHeat);
        nbt.putInt("Size", cachedSize);
        nbt.putInt("UpdateTicker", updateTicker);
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
        ItemStack toStore = ItemStack.EMPTY;
        if (stack != null && !stack.isEmpty() && isUsefulItem(stack)) {
            toStore = stack.copy();
            toStore.setCount(1);
        }
        items.set(index(x, y), toStore);
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
        if (!(world instanceof ServerWorld serverWorld)) return;

        float boomPower = 10.0f;
        float boomMod = 1.0f;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp) {
                float influence = comp.influenceExplosion(stack, this);
                if (influence > 0.0f && influence < 1.0f) {
                    boomMod *= influence;
                } else {
                    boomPower += influence;
                }
            }
            items.set(i, ItemStack.EMPTY);
        }

        boomPower *= heatEffectModifier * boomMod;
        boomPower = Math.min(boomPower, 45.0f);
        boomPower = Math.max(1.0f, boomPower);

        for (Direction dir : Direction.values()) {
            BlockPos chamberPos = pos.offset(dir);
            if (serverWorld.getBlockState(chamberPos).isOf(ModBlocks.REACTOR_CHAMBER)) {
                serverWorld.removeBlock(chamberPos, false);
            }
        }

        serverWorld.removeBlock(pos, false);
        doIc2StyleNuclearExplosion(serverWorld, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, boomPower);
    }

    private void doIc2StyleNuclearExplosion(ServerWorld serverWorld, double centerX, double centerY, double centerZ, float power) {
        if (power <= 0.0f) return;

        double maxDistance = power / 0.4;
        int steps = (int) Math.ceil(Math.PI / Math.atan(1.0 / maxDistance));
        Set<BlockPos> destroyed = new HashSet<>();
        Random random = serverWorld.random;

        for (int phiN = 0; phiN < 2 * steps; phiN++) {
            for (int thetaN = 0; thetaN < steps; thetaN++) {
                double phi = (Math.PI * 2.0 / steps) * phiN;
                double theta = (Math.PI / steps) * thetaN;
                shootExplosionRay(serverWorld, centerX, centerY, centerZ, phi, theta, power, destroyed, random, 0);
            }
        }

        for (BlockPos targetPos : destroyed) {
            BlockState state = serverWorld.getBlockState(targetPos);
            if (!state.isAir()) {
                serverWorld.breakBlock(targetPos, false);
            }
        }
    }

    private void shootExplosionRay(ServerWorld world, double x, double y, double z, double phi, double theta,
                                   double powerLeft, Set<BlockPos> destroyed, Random random, int depth) {
        double deltaX = Math.sin(theta) * Math.cos(phi);
        double deltaY = Math.cos(theta);
        double deltaZ = Math.sin(theta) * Math.sin(phi);
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();

        while (powerLeft > 0.0) {
            int blockY = MathHelper.floor(y);
            if (blockY < world.getBottomY() || blockY >= world.getTopY()) {
                break;
            }

            int blockX = MathHelper.floor(x);
            int blockZ = MathHelper.floor(z);
            mutablePos.set(blockX, blockY, blockZ);

            BlockState state = world.getBlockState(mutablePos);
            double absorption = getIc2ExplosionAbsorption(state, world, mutablePos);
            if (absorption < 0.0) {
                break;
            }

            if (absorption > 1000.0) {
                absorption = 0.5;
            } else {
                if (absorption > powerLeft) {
                    break;
                }
                if (!state.isAir() && !state.isOf(Blocks.BEDROCK)) {
                    destroyed.add(mutablePos.toImmutable());
                }
            }

            if (absorption > 10.0 && depth < 3) {
                for (int i = 0; i < 5; i++) {
                    shootExplosionRay(world, x, y, z,
                            random.nextDouble() * Math.PI * 2.0,
                            random.nextDouble() * Math.PI,
                            absorption * 0.4,
                            destroyed,
                            random,
                            depth + 1);
                }
            }

            powerLeft -= absorption;
            x += deltaX;
            y += deltaY;
            z += deltaZ;
        }
    }

    private double getIc2ExplosionAbsorption(BlockState state, World world, BlockPos pos) {
        double ret = 0.5;
        if (state.isAir()) {
            return ret;
        }

        if (state.getFluidState().isIn(FluidTags.WATER)) {
            return ret + 1.0;
        }

        float resistance = state.getBlock().getBlastResistance();
        if (resistance < 0.0f) {
            return resistance;
        }

        ret += (resistance + 4.0f) * 0.3;
        return ret;
    }

    @Override
    public long getEuStored() {
        resetEmitBudgetForCurrentTick();
        return emitBudgetEu;
    }

    @Override
    public long getEuCapacity() {
        return Math.max(0L, (long) Math.ceil(getOfferedEnergyEuPerTick()));
    }

    @Override
    public int getSinkTier() {
        return 0;
    }

    @Override
    public int getSourceTier() {
        return 5;
    }

    @Override
    public long insertEu(long amount, Direction from, boolean simulate) {
        return 0L;
    }

    @Override
    public long extractEu(long amount, Direction to, boolean simulate) {
        if (amount <= 0L || !canExtract(to)) return 0L;

        resetEmitBudgetForCurrentTick();
        long extracted = Math.min(amount, emitBudgetEu);
        if (!simulate) {
            emitBudgetEu -= extracted;
        }
        return extracted;
    }

    @Override
    public boolean canInsert(Direction from) {
        return false;
    }

    @Override
    public boolean canExtract(Direction to) {
        return !isFluidCooled() && getOfferedEnergyEuPerTick() > 0.0f;
    }

    @Override
    public boolean sendMultipleEnergyPackets() {
        return true;
    }

    @Override
    public int getMaxEnergyPacketCount() {
        long offered = Math.max(0L, (long) Math.floor(getOfferedEnergyEuPerTick()));
        long packet = EuUtil.powerFromTier(getSourceTier());
        if (packet <= 0L) return 1;
        long count = (offered + packet - 1L) / packet;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, count));
    }

    @Override
    public double getOfferedEnergy() {
        return Math.max(0.0f, getOfferedEnergyEuPerTick());
    }

    @Override
    public int getTickRate() {
        return 20;
    }

    @Override
    public boolean produceEnergy() {
        if (world == null) return false;
        if (world.isReceivingRedstonePower(pos)) return true;

        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.offset(dir);
            if (world.getBlockState(adjacent).isOf(ModBlocks.REACTOR_CHAMBER)
                    && world.isReceivingRedstonePower(adjacent)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isFluidCooled() {
        return false;
    }
}
