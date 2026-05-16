package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.block.ConstructionFoamBlock;
import com.shipovskijkorp.industriallegacy.block.ScaffoldBlock;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.item.armor.FoamPackItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** IC2 Experimental CF sprayer: 8000 mB capacity, 100 mB per foam block, normal/single mode. */
public class FoamSprayerItem extends Item implements IModeSwitchableItem {
    private static final String NBT_FOAM = "foam";
    private static final String NBT_MODE = "mode";

    public static final int CAPACITY_MB = 8_000;
    public static final int FLUID_PER_FOAM_MB = 100;
    private static final int NORMAL_MODE_MAX_BLOCKS = 10;
    private static final int SINGLE_MODE_MAX_BLOCKS = 1;

    public FoamSprayerItem(Settings settings) {
        super(settings.maxCount(1));
    }

    public static int getFoam(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : Math.max(0, Math.min(CAPACITY_MB, nbt.getInt(NBT_FOAM)));
    }

    public static void setFoam(ItemStack stack, int amountMb) {
        int clamped = Math.max(0, Math.min(CAPACITY_MB, amountMb));
        NbtCompound nbt = stack.getOrCreateNbt();
        if (clamped <= 0) {
            nbt.remove(NBT_FOAM);
            if (nbt.getKeys().isEmpty()) stack.setNbt(null);
        } else {
            nbt.putInt(NBT_FOAM, clamped);
        }
    }

    public static boolean canFill(ItemStack stack) {
        return stack.getItem() instanceof FoamSprayerItem && getFoam(stack) < CAPACITY_MB;
    }

    public static int fill(ItemStack stack, int availableMb) {
        int fill = Math.min(Math.max(0, availableMb), CAPACITY_MB - getFoam(stack));
        if (fill > 0) setFoam(stack, getFoam(stack) + fill);
        return fill;
    }

    public static int drain(ItemStack stack, int maxDrainMb) {
        int drain = Math.min(Math.max(0, maxDrainMb), getFoam(stack));
        if (drain > 0) setFoam(stack, getFoam(stack) - drain);
        return drain;
    }

    public static ItemStack createFilledStack() {
        ItemStack stack = new ItemStack(com.shipovskijkorp.industriallegacy.registry.ModItems.FOAM_SPRAYER);
        setFoam(stack, CAPACITY_MB);
        return stack;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null) return ActionResult.PASS;

        World world = ctx.getWorld();
        ItemStack stack = ctx.getStack();
        int availableBlocks = getAvailableFoam(player, stack) / FLUID_PER_FOAM_MB;
        if (availableBlocks <= 0) return ActionResult.FAIL;

        int maxBlocks = Math.min(availableBlocks, isSingleMode(stack) ? SINGLE_MODE_MAX_BLOCKS : NORMAL_MODE_MAX_BLOCKS);
        BlockPos clicked = ctx.getBlockPos();
        BlockState clickedState = world.getBlockState(clicked);
        Target target;
        BlockPos start;

        if (canPlaceFoam(world, clicked, Target.SCAFFOLD)) {
            target = Target.SCAFFOLD;
            start = clicked;
        } else {
            target = Target.ANY;
            start = clickedState.isReplaceable() ? clicked : clicked.offset(ctx.getSide());
        }

        if (!world.isClient) {
            Direction excluded = getExcludedDirection(player, ctx.getSide());
            int placed = sprayFoam(world, start, excluded, target, maxBlocks);
            if (placed <= 0) return ActionResult.PASS;
            if (!player.getAbilities().creativeMode) drainFoam(player, stack, placed * FLUID_PER_FOAM_MB);
        }
        return ActionResult.success(world.isClient);
    }

    private static int getAvailableFoam(PlayerEntity player, ItemStack sprayer) {
        int amount = getFoam(sprayer);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof FoamPackItem) amount += FoamPackItem.getFoam(chest);
        return amount;
    }

    /** IC2 drains the worn CF pack first, then the sprayer itself. */
    private static void drainFoam(PlayerEntity player, ItemStack sprayer, int amountMb) {
        int remaining = Math.max(0, amountMb);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof FoamPackItem) {
            remaining -= FoamPackItem.drain(chest, remaining);
        }
        if (remaining > 0) drain(sprayer, remaining);
    }

    private static Direction getExcludedDirection(PlayerEntity player, Direction fallback) {
        Vec3d look = player.getRotationVec(1.0f);
        Direction view = Direction.getFacing(look.x, look.y, look.z);
        return view == null ? fallback.getOpposite() : view.getOpposite();
    }

    private static int sprayFoam(World world, BlockPos start, Direction excludedDir, Target target, int maxFoamBlocks) {
        if (!canPlaceFoam(world, start, target)) return 0;

        ArrayDeque<BlockPos> toCheck = new ArrayDeque<>();
        Set<BlockPos> positions = new HashSet<>();
        toCheck.add(start);

        while (!toCheck.isEmpty() && positions.size() < maxFoamBlocks) {
            BlockPos current = toCheck.removeFirst();
            if (!canPlaceFoam(world, current, target) || !positions.add(current)) continue;
            for (Direction direction : Direction.values()) {
                if (direction != excludedDir) toCheck.add(current.offset(direction));
            }
        }

        int placed = 0;
        for (BlockPos pos : positions) {
            if (placeFoam(world, pos, target)) placed++;
        }
        return placed;
    }

    private static boolean placeFoam(World world, BlockPos pos, Target target) {
        BlockState state = world.getBlockState(pos);
        if (target == Target.SCAFFOLD && state.getBlock() instanceof ScaffoldBlock scaffold) {
            ScaffoldBlock.ScaffoldType type = scaffold.getScaffoldType();
            if (type == ScaffoldBlock.ScaffoldType.WOOD || type == ScaffoldBlock.ScaffoldType.REINFORCED_WOOD) {
                Block.dropStack(world, pos, new ItemStack(state.getBlock().asItem()));
                return world.setBlockState(pos, ModBlocks.FOAM.getDefaultState(), Block.NOTIFY_ALL);
            }
            if (type == ScaffoldBlock.ScaffoldType.REINFORCED_IRON) {
                Block.dropStack(world, pos, new ItemStack(ModBlocks.IRON_FENCE));
            }
            if (type == ScaffoldBlock.ScaffoldType.IRON || type == ScaffoldBlock.ScaffoldType.REINFORCED_IRON) {
                return world.setBlockState(pos, ModBlocks.REINFORCED_FOAM.getDefaultState(), Block.NOTIFY_ALL);
            }
            return false;
        }

        return world.setBlockState(pos, ModBlocks.FOAM.getDefaultState(), Block.NOTIFY_ALL);
    }

    private static boolean canPlaceFoam(World world, BlockPos pos, Target target) {
        BlockState state = world.getBlockState(pos);
        if (target == Target.SCAFFOLD) return state.getBlock() instanceof ScaffoldBlock;
        return state.isAir() || state.isReplaceable();
    }

    private static boolean isSingleMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getInt(NBT_MODE) == 1;
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int next = isSingleMode(stack) ? 0 : 1;
        nbt.putInt(NBT_MODE, next);
        return next;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable(isSingleMode(stack)
                ? "message.industrial_legacy.foam_sprayer.mode.single"
                : "message.industrial_legacy.foam_sprayer.mode.normal");
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getFoam(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0f * getFoam(stack) / CAPACITY_MB);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return UniversalFluidCellItem.CellFluid.CONSTRUCTION_FOAM.tintArgb() & 0x00FFFFFF;
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(getTranslationKey());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.industrial_legacy.foam_sprayer.foam", getFoam(stack), CAPACITY_MB).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.foam_sprayer.mode", getModeName(stack)).formatted(Formatting.GRAY));
    }

    private enum Target {
        ANY,
        SCAFFOLD
    }
}
