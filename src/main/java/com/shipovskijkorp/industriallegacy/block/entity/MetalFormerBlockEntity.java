package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.block.entity.base.AbstractStandardMachineBlockEntity;
import com.shipovskijkorp.industriallegacy.block.MetalFormerBlock;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.recipe.MetalFormerRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.MetalFormerScreenHandler;
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
import net.minecraft.world.World;

public class MetalFormerBlockEntity extends AbstractStandardMachineBlockEntity {
    public enum Mode {
        EXTRUDING,
        ROLLING,
        CUTTING;

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_DISCHARGE = 2;
    public static final int SLOT_UPGRADE_0 = 3;
    public static final int UPGRADE_SLOTS = 4;
    public static final int INV_SIZE = SLOT_UPGRADE_0 + UPGRADE_SLOTS;

    private static final int[] TOP_SLOTS = new int[]{SLOT_INPUT};
    private static final int[] SIDE_SLOTS = new int[]{SLOT_INPUT, SLOT_DISCHARGE, SLOT_UPGRADE_0, SLOT_UPGRADE_0 + 1, SLOT_UPGRADE_0 + 2, SLOT_UPGRADE_0 + 3};
    private static final int[] BOTTOM_SLOTS = new int[]{SLOT_OUTPUT};

    private static final int TIER = 1;
    private static final long CAPACITY = 2000L;
    private static final int EU_PER_TICK = 10;
    private static final int BASE_TICKS = 200;

    private Mode mode = Mode.EXTRUDING;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int size() { return 5; }
        @Override public int get(int i) {
            return switch (i) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, energy);
                case 1 -> (int) Math.min(Integer.MAX_VALUE, energyCapacity);
                case 2 -> progress;
                case 3 -> maxProgress;
                case 4 -> mode.ordinal();
                default -> 0;
            };
        }
        @Override public void set(int i, int v) {
            switch (i) {
                case 0 -> energy = clampEnergy(v);
                case 2 -> progress = Math.max(0, v);
                case 3 -> maxProgress = Math.max(1, v);
                case 4 -> setModeByOrdinal(v);
                default -> { }
            }
        }
    };

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER, pos, state, INV_SIZE, CAPACITY, TIER, EU_PER_TICK, BASE_TICKS,
                SLOT_DISCHARGE, SLOT_UPGRADE_0, UPGRADE_SLOTS, TOP_SLOTS, SIDE_SLOTS, BOTTOM_SLOTS, new int[]{SLOT_OUTPUT});
    }

    public static void tick(World world, BlockPos pos, BlockState state, MetalFormerBlockEntity be) {
        if (world.isClient) return;
        boolean dirty = be.chargeFromDischargeSlot();
        boolean active = be.processStandardMachine(world);
        if (state.get(MetalFormerBlock.LIT) != active) world.setBlockState(pos, state.with(MetalFormerBlock.LIT, active), 3);
        if (active || dirty) be.markDirty();
    }

    @Override
    protected MachineOperation getOperation(World world) {
        MetalFormerRecipe recipe = MachineRecipeManager.findMetalFormerRecipe(this, mode).orElse(null);
        if (recipe == null) return null;
        ItemStack result = recipe.getOutput(world.getRegistryManager()).copy();
        int ticks = recipe.getTicks() <= 0 ? operationLength : recipe.getTicks();
        return operation(SLOT_INPUT, recipe.getInputCount(), SLOT_OUTPUT, result, ticks, energyConsume);
    }

    public void cycleMode() {
        this.mode = this.mode.next();
        this.progress = 0;
        markDirty();
    }

    public Mode getMode() { return mode; }

    public void setModeByOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= Mode.values().length) ordinal = 0;
        this.mode = Mode.values()[ordinal];
    }

    @Override public PropertyDelegate getGuiProps() { return props; }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("mode", mode.name());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        try {
            mode = Mode.valueOf(nbt.getString("mode"));
        } catch (IllegalArgumentException ignored) {
            mode = Mode.EXTRUDING;
        }
    }

    @Override public Text getDisplayName() { return Text.translatable("block.industrial_legacy.metal_former"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) { return new MetalFormerScreenHandler(syncId, playerInventory, this, props, pos); }
    @Override public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) { buf.writeBlockPos(pos); }
}
