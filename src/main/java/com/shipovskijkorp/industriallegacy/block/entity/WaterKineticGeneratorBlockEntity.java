package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.WaterKineticGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.kinetic.IKineticSource;
import com.shipovskijkorp.industriallegacy.item.WindRotorItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.WaterKineticGeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class WaterKineticGeneratorBlockEntity extends BlockEntity implements SidedInventory, IKineticSource, ExtendedScreenHandlerFactory {
    public static final int SLOT_ROTOR = 0;
    public static final int INV_SIZE = 1;
    private static final int[] ALL_SLOTS = new int[] { SLOT_ROTOR };
    private static final int TICK_RATE = 20;
    private static final float ROTATION_MODIFIER = 0.1f;
    private static final double EFFICIENCY_ROLL_OFF_EXPONENT = 2.0D;
    private static final Identifier WOODEN_ROTOR_TEXTURE = new Identifier("industrial_legacy", "textures/item/rotor/wood_rotor_model.png");

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private final float outputModifier;

    private BiomeState biomeState = BiomeState.UNKNOWN;
    private int updateTicker = -1;
    private boolean rightFacing;
    private int distanceToNormalBiome;
    private int crossSection;
    private int obstructedCrossSection;
    private int waterFlow;
    private long lastCheck;
    private float angle;
    private float rotationSpeed;
    private Direction lastFacing = Direction.NORTH;

    private int guiKuOutput;
    private int guiBiomeState;
    private int guiRotorHealth;
    private int guiHasRotor;
    private int guiRotorHasSpace;
    private int guiWaterFlow;
    private int guiObstructedCrossSection;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return WaterKineticGeneratorScreenHandler.PROP_COUNT; }

        @Override
        public int get(int index) {
            if (world == null || !world.isClient) {
                refreshGuiProperties();
            }
            return switch (index) {
                case 0 -> guiKuOutput;
                case 1 -> guiBiomeState;
                case 2 -> guiRotorHealth;
                case 3 -> guiHasRotor;
                case 4 -> guiRotorHasSpace;
                case 5 -> guiWaterFlow;
                case 6 -> guiObstructedCrossSection;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> guiKuOutput = value;
                case 1 -> guiBiomeState = value;
                case 2 -> guiRotorHealth = value;
                case 3 -> guiHasRotor = value;
                case 4 -> guiRotorHasSpace = value;
                case 5 -> guiWaterFlow = value;
                case 6 -> guiObstructedCrossSection = value;
                default -> {}
            }
        }
    };

    private void refreshGuiProperties() {
        guiKuOutput = getKuOutput();
        guiBiomeState = biomeState.ordinal();
        guiRotorHealth = getRotorHealthPercent();
        guiHasRotor = hasRotor() ? 1 : 0;
        guiRotorHasSpace = rotorHasSpace() ? 1 : 0;
        guiWaterFlow = waterFlow;
        guiObstructedCrossSection = obstructedCrossSection;
    }

    public WaterKineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_KINETIC_GENERATOR, pos, state);
        this.outputModifier = 0.2f * Math.max(0.0f, ILConfig.getFloat("balance/energy/kineticgenerator/water", 1.0f));
        this.lastFacing = getFacing();
    }

    public static void tick(World world, BlockPos pos, BlockState state, WaterKineticGeneratorBlockEntity be) {
        if (world.isClient) return;

        if (be.updateTicker < 0) {
            be.updateTicker = world.random.nextInt(TICK_RATE);
        }
        if (be.updateTicker++ % TICK_RATE != 0) {
            return;
        }

        Direction facing = be.getFacing();
        if (be.biomeState == BiomeState.UNKNOWN || be.lastFacing != facing) {
            be.biomeState = be.getBiomeState(world, pos);
            be.updateSeaInfo();
            be.lastFacing = facing;
            if (be.biomeState == BiomeState.INVALID) {
                be.setActive(false);
                be.stopSpinning();
                be.refreshGuiProperties();
                be.markDirtyAndSync();
                return;
            }
        }

        float oldRotationSpeed = be.rotationSpeed;
        boolean needsInvUpdate = false;
        boolean nextActive = be.isActive();

        if (be.hasRotor() && be.checkSpace(1, true) == 0) {
            if (!nextActive) {
                needsInvUpdate = true;
                nextActive = true;
            }
        } else if (nextActive) {
            nextActive = false;
            needsInvUpdate = true;
        }

        if (nextActive) {
            int diameter = be.getRotorDiameter();
            be.crossSection = square(diameter / 2 * 2 * 2 + 1);
            be.obstructedCrossSection = be.checkSpace(diameter * 3, false);
            if (be.obstructedCrossSection > 0 && be.obstructedCrossSection <= (diameter + 1) / 2) {
                be.obstructedCrossSection = 0;
            }

            int rotorDamage = 0;
            if (be.obstructedCrossSection < 0) {
                be.stopSpinning();
            } else if (be.biomeState == BiomeState.OCEAN) {
                float diff = (float) Math.sin((double) world.getTime() * Math.PI / 6000.0D);
                diff *= Math.abs(diff);
                be.rotationSpeed = (float) ((double) (diff * (float) be.distanceToNormalBiome / 100.0f)
                        * (1.0D - Math.pow((double) be.obstructedCrossSection / (double) Math.max(1, be.crossSection), EFFICIENCY_ROLL_OFF_EXPONENT)));
                be.waterFlow = (int) (be.rotationSpeed * 3000.0f);
                if (be.rightFacing) {
                    be.rotationSpeed *= -1.0f;
                }
                be.waterFlow = (int) ((float) be.waterFlow * be.getEfficiency());
                rotorDamage = 2;
            } else if (be.biomeState == BiomeState.RIVER) {
                be.rotationSpeed = (float) MathHelper.clamp(be.distanceToNormalBiome, 20, 50) / 50.0f;
                be.waterFlow = (int) (be.rotationSpeed * 1000.0f);
                if (facing == Direction.EAST || facing == Direction.NORTH) {
                    be.rotationSpeed *= -1.0f;
                }
                be.waterFlow = (int) ((float) be.waterFlow * (be.getEfficiency()
                        * (1.0f - 0.3f * world.random.nextFloat() - 0.1f * ((float) be.obstructedCrossSection / (float) Math.max(1, be.crossSection)))));
                rotorDamage = 1;
            }
            be.damageRotor(rotorDamage);
        } else {
            be.stopSpinning();
        }

        be.setActive(nextActive);
        be.refreshGuiProperties();
        if (needsInvUpdate || oldRotationSpeed != be.rotationSpeed) {
            be.markDirtyAndSync();
        } else {
            be.markDirty();
        }
    }

    private void stopSpinning() {
        rotationSpeed = 0.0f;
        waterFlow = 0;
    }

    public int getRotorDiameter() {
        ItemStack stack = items.get(SLOT_ROTOR);
        if (stack.getItem() instanceof WindRotorItem rotor) {
            if (biomeState == BiomeState.OCEAN) {
                return rotor.getDiameter(stack);
            }
            return (rotor.getDiameter(stack) + 1) * 2 / 3;
        }
        return 0;
    }

    public int checkSpace(int length, boolean onlyRotor) {
        if (world == null) return -1;
        int box = getRotorDiameter() / 2;
        int lenTemp = 0;
        if (onlyRotor) {
            length = 1;
            lenTemp = length + 1;
        } else {
            box *= 2;
        }

        Direction fwdDir = getFacing();
        Direction rightDir = rotateAroundYClockwise(fwdDir);
        int ret = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int up = -box; up <= box; ++up) {
            int y = pos.getY() + up;
            for (int right = -box; right <= box; ++right) {
                boolean occupied = false;
                for (int fwd = lenTemp - length; fwd <= length; ++fwd) {
                    int x = pos.getX() + fwd * fwdDir.getOffsetX() + right * rightDir.getOffsetX();
                    int z = pos.getZ() + fwd * fwdDir.getOffsetZ() + right * rightDir.getOffsetZ();
                    mutable.set(x, y, z);
                    BlockState state = world.getBlockState(mutable);
                    if (state.isOf(Blocks.WATER)) continue;
                    occupied = true;
                    if ((up == 0 && right == 0 && fwd == 0)
                            || !(world.getBlockEntity(mutable) instanceof WaterKineticGeneratorBlockEntity)
                            || onlyRotor) {
                        continue;
                    }
                    return -1;
                }
                if (occupied) ++ret;
            }
        }
        return ret;
    }

    private void updateSeaInfo() {
        if (world == null) return;
        Direction facing = getFacing();
        for (int distance = 1; distance < 200; ++distance) {
            if (!isValidBiome(world.getBiome(pos.offset(facing, distance)))) {
                distanceToNormalBiome = distance;
                rightFacing = true;
                return;
            }
            if (!isValidBiome(world.getBiome(pos.offset(facing, -distance)))) {
                distanceToNormalBiome = distance;
                rightFacing = false;
                return;
            }
        }
        distanceToNormalBiome = 200;
        rightFacing = true;
    }

    private BiomeState getBiomeState(World world, BlockPos pos) {
        RegistryEntry<Biome> biome = world.getBiome(pos);
        if (biome.isIn(BiomeTags.IS_OCEAN)) return BiomeState.OCEAN;
        if (biome.isIn(BiomeTags.IS_RIVER)) return BiomeState.RIVER;
        return BiomeState.INVALID;
    }

    private boolean isValidBiome(RegistryEntry<Biome> biome) {
        return biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_RIVER);
    }

    public boolean hasRotor() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem && stack.getItem() != ModItems.ROTOR_WOOD;
    }

    public boolean rotorHasSpace() {
        return hasRotor() && checkSpace(1, true) == 0;
    }

    public int getKuOutput() {
        if (isActive()) {
            return (int) Math.abs((float) waterFlow * outputModifier);
        }
        return 0;
    }

    public float getEfficiency() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getEfficiency(stack) : 0.0f;
    }

    public int getRotorHealthPercent() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getHealthPercent(stack) : 0;
    }

    public Identifier getRotorRenderTexture() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getRotorModelTexture(stack) : WOODEN_ROTOR_TEXTURE;
    }

    public float getAngle() {
        if (rotationSpeed != 0.0f) {
            long now = System.currentTimeMillis();
            if (lastCheck != 0L) {
                angle += (float) (now - lastCheck) * rotationSpeed * ROTATION_MODIFIER;
                angle %= 360.0f;
            }
            lastCheck = now;
        } else {
            lastCheck = System.currentTimeMillis();
        }
        return angle;
    }

    public BiomeState getBiomeState() { return biomeState; }
    public int getWaterFlow() { return waterFlow; }
    public int getObstructedCrossSection() { return obstructedCrossSection; }
    public float getRotationSpeed() { return rotationSpeed; }
    public PropertyDelegate getGuiProperties() { return props; }

    public Direction getFacing() {
        BlockState state = getCachedState();
        return state.contains(WaterKineticGeneratorBlock.FACING) ? state.get(WaterKineticGeneratorBlock.FACING) : Direction.NORTH;
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return side.getOpposite() == getFacing() ? getKuOutput() : 0;
    }

    @Override
    public int drawKineticEnergy(Direction side, int request, boolean simulate) {
        if (side.getOpposite() == getFacing()) {
            return Math.min(Math.max(0, request), getKuOutput());
        }
        return 0;
    }

    private boolean isActive() {
        BlockState state = getCachedState();
        return state.contains(WaterKineticGeneratorBlock.LIT) && state.get(WaterKineticGeneratorBlock.LIT);
    }

    private void setActive(boolean active) {
        if (world == null) return;
        BlockState state = getCachedState();
        if (!state.contains(WaterKineticGeneratorBlock.LIT) || state.get(WaterKineticGeneratorBlock.LIT) == active) return;
        world.setBlockState(pos, state.with(WaterKineticGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
    }

    private void damageRotor(int amount) {
        if (amount <= 0) return;
        ItemStack stack = items.get(SLOT_ROTOR);
        if (stack.isEmpty() || !stack.isDamageable()) return;
        stack.setDamage(stack.getDamage() + amount);
        if (stack.getDamage() >= stack.getMaxDamage()) {
            items.set(SLOT_ROTOR, ItemStack.EMPTY);
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

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putString("biomeState", biomeState.name());
        nbt.putInt("updateTicker", updateTicker);
        nbt.putBoolean("rightFacing", rightFacing);
        nbt.putInt("distanceToNormalBiome", distanceToNormalBiome);
        nbt.putInt("crossSection", crossSection);
        nbt.putInt("obstructedCrossSection", obstructedCrossSection);
        nbt.putInt("waterFlow", waterFlow);
        nbt.putFloat("rotationSpeed", rotationSpeed);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        biomeState = BiomeState.byName(nbt.getString("biomeState"));
        updateTicker = nbt.contains("updateTicker") ? nbt.getInt("updateTicker") : -1;
        rightFacing = nbt.getBoolean("rightFacing");
        distanceToNormalBiome = nbt.getInt("distanceToNormalBiome");
        crossSection = Math.max(0, nbt.getInt("crossSection"));
        obstructedCrossSection = nbt.getInt("obstructedCrossSection");
        waterFlow = nbt.getInt("waterFlow");
        rotationSpeed = nbt.getFloat("rotationSpeed");
        lastFacing = getFacing();
    }

    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); if (!r.isEmpty()) { refreshGuiProperties(); markDirtyAndSync(); } return r; }
    @Override public ItemStack removeStack(int slot) { ItemStack r = Inventories.removeStack(items, slot); refreshGuiProperties(); markDirtyAndSync(); return r; }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); if (stack.getCount() > stack.getMaxCount()) stack.setCount(stack.getMaxCount()); refreshGuiProperties(); markDirtyAndSync(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return world != null && world.getBlockEntity(pos) == this && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D; }
    @Override public void clear() { items.clear(); }
    @Override public int[] getAvailableSlots(Direction side) { return ALL_SLOTS; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_ROTOR && stack.getItem() instanceof WindRotorItem && stack.getItem() != ModItems.ROTOR_WOOD; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_ROTOR; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.water_kinetic_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new WaterKineticGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    private static Direction rotateAroundYClockwise(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static int square(int value) {
        return value * value;
    }

    public enum BiomeState {
        UNKNOWN,
        OCEAN,
        RIVER,
        INVALID;

        public static BiomeState byOrdinal(int ordinal) {
            BiomeState[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : UNKNOWN;
        }

        public static BiomeState byName(String name) {
            if (name == null || name.isEmpty()) return UNKNOWN;
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return UNKNOWN;
            }
        }
    }
}
