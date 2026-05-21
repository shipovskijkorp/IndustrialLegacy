package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.WindKineticGeneratorBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.energy.kinetic.IKineticSource;
import com.shipovskijkorp.industriallegacy.item.WindRotorItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.WindKineticGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.world.WindSimulation;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WindKineticGeneratorBlockEntity extends BlockEntity implements SidedInventory, IKineticSource, ExtendedScreenHandlerFactory {
    public static final int SLOT_ROTOR = 0;
    public static final int INV_SIZE = 1;
    private static final int[] ALL_SLOTS = new int[] { SLOT_ROTOR };
    private static final int TICK_RATE = 32;
    private static final Identifier WOODEN_ROTOR_TEXTURE = new Identifier("industrial_legacy", "textures/item/rotor/wood_rotor_model.png");

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(INV_SIZE, ItemStack.EMPTY);
    private double windStrength;
    private int obstructedCrossSection;
    private int crossSection;
    private int updateTicker = -1;
    private float rotationSpeed;
    private float angle;
    private long lastCheck;
    private final float outputModifier;

    private int guiWindStrength;
    private int guiKuOutput;
    private int guiRotorHealth;
    private int guiObstructedCrossSection;
    private int guiRotorOverloaded;
    private int guiHasRotor;
    private int guiRotorHasSpace;
    private int guiWindStrongEnough;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return WindKineticGeneratorScreenHandler.PROP_COUNT; }

        @Override
        public int get(int index) {
            if (world == null || !world.isClient) {
                refreshGuiProperties();
            }
            return switch (index) {
                case 0 -> guiWindStrength;
                case 1 -> guiKuOutput;
                case 2 -> guiRotorHealth;
                case 3 -> guiObstructedCrossSection;
                case 4 -> guiRotorOverloaded;
                case 5 -> guiHasRotor;
                case 6 -> guiRotorHasSpace;
                case 7 -> guiWindStrongEnough;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> guiWindStrength = value;
                case 1 -> guiKuOutput = value;
                case 2 -> guiRotorHealth = value;
                case 3 -> guiObstructedCrossSection = value;
                case 4 -> guiRotorOverloaded = value;
                case 5 -> guiHasRotor = value;
                case 6 -> guiRotorHasSpace = value;
                case 7 -> guiWindStrongEnough = value;
                default -> {}
            }
        }
    };

    private void refreshGuiProperties() {
        boolean hasRotor = hasRotor();
        boolean rotorHasSpace = hasRotor && checkSpace(1, true) == 0;
        boolean windStrongEnough = hasRotor && rotorHasSpace && windStrength >= (double) getMinWindStrength();
        boolean overloaded = hasRotor && rotorHasSpace && windStrongEnough && windStrength > (double) getMaxWindStrength();

        guiWindStrength = (int) windStrength;
        guiKuOutput = getKuOutput();
        guiRotorHealth = getRotorHealthPercent();
        guiObstructedCrossSection = obstructedCrossSection;
        guiRotorOverloaded = overloaded ? 1 : 0;
        guiHasRotor = hasRotor ? 1 : 0;
        guiRotorHasSpace = rotorHasSpace ? 1 : 0;
        guiWindStrongEnough = windStrongEnough ? 1 : 0;
    }

    public WindKineticGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WIND_KINETIC_GENERATOR, pos, state);
        this.outputModifier = 10.0f * Math.max(0.0f, ILConfig.getFloat("balance/energy/kineticgenerator/wind", 1.0f));
    }

    public static void tick(World world, BlockPos pos, BlockState state, WindKineticGeneratorBlockEntity be) {
        if (world.isClient) return;
        if (!(world instanceof ServerWorld serverWorld)) return;

        if (be.updateTicker < 0) {
            be.updateTicker = world.random.nextInt(TICK_RATE);
        }
        if (++be.updateTicker % TICK_RATE != 0) {
            return;
        }

        float oldRotationSpeed = be.rotationSpeed;
        boolean active = be.hasRotor() && be.rotorHasSpace();
        be.setActive(active);

        if (active) {
            int diameter = be.getRotorDiameter();
            be.crossSection = square(diameter / 2 * 2 * 2 + 1);
            be.obstructedCrossSection = be.checkSpace(diameter * 3, false);
            if (be.obstructedCrossSection > 0 && be.obstructedCrossSection <= (diameter + 1) / 2) {
                be.obstructedCrossSection = 0;
            }
            if (be.obstructedCrossSection < 0) {
                be.windStrength = 0.0D;
                be.rotationSpeed = 0.0f;
            } else {
                be.windStrength = be.calculateWindStrength(serverWorld);
                float speed = (float) MathHelper.clamp((be.windStrength - (double) be.getMinWindStrength()) / (double) Math.max(1, be.getMaxWindStrength()), 0.0D, 2.0D);
                be.rotationSpeed = speed;
                if (be.windStrength >= (double) be.getMinWindStrength()) {
                    be.damageRotor(be.windStrength <= (double) be.getMaxWindStrength() ? 1 : 4);
                }
            }
        } else {
            be.windStrength = 0.0D;
            be.rotationSpeed = 0.0f;
        }
        be.refreshGuiProperties();
        if (oldRotationSpeed != be.rotationSpeed) {
            be.markDirtyAndSync();
        } else {
            be.markDirty();
        }
    }

    public boolean isActiveForWindMeter() {
        return hasRotor() && getCachedState().contains(WindKineticGeneratorBlock.LIT) && getCachedState().get(WindKineticGeneratorBlock.LIT);
    }

    public double calculateWindStrength(ServerWorld serverWorld) {
        double windStr = WindSimulation.get(serverWorld).getWindAt(serverWorld, pos.getY());
        int section = Math.max(1, crossSection);
        double obstructionRatio = (double) obstructedCrossSection / (double) section;
        return Math.max(0.0D, windStr * (1.0D - obstructionRatio * obstructionRatio));
    }

    public int checkSpace(int length, boolean onlyRotor) {
        if (world == null) return -1;
        int box = getRotorDiameter() / 2;
        int lenTemp = 0;
        if (onlyRotor) {
            length = 1;
            lenTemp = length + 1;
        }
        if (!onlyRotor) {
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
                    if (state.isAir()) continue;
                    occupied = true;
                    if (up == 0 && right == 0 && fwd == 0) continue;
                    if (!onlyRotor && world.getBlockEntity(mutable) instanceof WindKineticGeneratorBlockEntity) {
                        return -1;
                    }
                }
                if (occupied) ++ret;
            }
        }
        return ret;
    }

    private static Direction rotateAroundYClockwise(Direction dir) {
        return switch (dir) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    public boolean hasRotor() {
        return !items.get(SLOT_ROTOR).isEmpty() && items.get(SLOT_ROTOR).getItem() instanceof WindRotorItem;
    }

    public boolean rotorHasSpace() {
        return hasRotor() && checkSpace(1, true) == 0;
    }

    public boolean isWindStrongEnough() {
        return windStrength >= (double) getMinWindStrength();
    }

    public boolean isRotorOverloaded() {
        return hasRotor() && rotorHasSpace() && isWindStrongEnough() && windStrength > (double) getMaxWindStrength();
    }

    public int getKuOutput() {
        if (windStrength >= (double) getMinWindStrength() && isActiveForWindMeter()) {
            return (int) (windStrength * (double) outputModifier * (double) getEfficiency());
        }
        return 0;
    }

    public int getRotorDiameter() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getDiameter(stack) : 0;
    }

    public float getEfficiency() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getEfficiency(stack) : 0.0f;
    }

    public int getMinWindStrength() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getMinWindStrength(stack) : 0;
    }

    public int getMaxWindStrength() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getMaxWindStrength(stack) : 0;
    }

    public int getRotorHealthPercent() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getHealthPercent(stack) : 0;
    }

    public Identifier getRotorRenderTexture() {
        ItemStack stack = items.get(SLOT_ROTOR);
        return stack.getItem() instanceof WindRotorItem rotor ? rotor.getRotorModelTexture(stack) : WOODEN_ROTOR_TEXTURE;
    }

    public int getObstructions() { return obstructedCrossSection; }
    public double getWindStrength() { return windStrength; }
    public float getRotationSpeed() { return rotationSpeed; }

    public float getAngle() {
        if (rotationSpeed != 0.0f) {
            long now = System.currentTimeMillis();
            if (lastCheck != 0L) {
                angle += (float) (now - lastCheck) * rotationSpeed;
                angle %= 360.0f;
            }
            lastCheck = now;
        } else {
            lastCheck = System.currentTimeMillis();
        }
        return angle;
    }

    public PropertyDelegate getGuiProperties() { return props; }

    private Direction getFacing() {
        BlockState state = getCachedState();
        return state.contains(WindKineticGeneratorBlock.FACING) ? state.get(WindKineticGeneratorBlock.FACING) : Direction.NORTH;
    }

    private void setActive(boolean active) {
        if (world == null) return;
        BlockState state = getCachedState();
        if (!state.contains(WindKineticGeneratorBlock.LIT) || state.get(WindKineticGeneratorBlock.LIT) == active) return;
        world.setBlockState(pos, state.with(WindKineticGeneratorBlock.LIT, active), Block.NOTIFY_ALL);
    }

    private void damageRotor(int amount) {
        ItemStack stack = items.get(SLOT_ROTOR);
        if (stack.isEmpty() || !stack.isDamageable()) return;
        stack.setDamage(stack.getDamage() + amount);
        if (stack.getDamage() >= stack.getMaxDamage()) {
            items.set(SLOT_ROTOR, ItemStack.EMPTY);
        }
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

    @Override protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items);
        nbt.putDouble("windStrength", windStrength);
        nbt.putInt("obstructedCrossSection", obstructedCrossSection);
        nbt.putInt("crossSection", crossSection);
        nbt.putInt("updateTicker", updateTicker);
        nbt.putFloat("rotationSpeed", rotationSpeed);
    }

    @Override public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, items);
        windStrength = Math.max(0.0D, nbt.getDouble("windStrength"));
        obstructedCrossSection = nbt.getInt("obstructedCrossSection");
        crossSection = Math.max(0, nbt.getInt("crossSection"));
        updateTicker = nbt.contains("updateTicker") ? nbt.getInt("updateTicker") : -1;
        rotationSpeed = nbt.getFloat("rotationSpeed");
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
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == SLOT_ROTOR && stack.getItem() instanceof WindRotorItem; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return isValid(slot, stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot == SLOT_ROTOR; }

    @Override public Text getDisplayName() { return Text.translatable("container.industrial_legacy.wind_kinetic_generator"); }
    @Override public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new WindKineticGeneratorScreenHandler(syncId, playerInventory, this); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }

    private static int square(int value) {
        return value * value;
    }
}
