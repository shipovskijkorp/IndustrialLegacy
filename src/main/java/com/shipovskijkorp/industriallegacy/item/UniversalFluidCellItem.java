package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.registry.ModFluids;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UniversalFluidCellItem extends Item {
    private static final String NBT_FLUID = "Fluid";
    private static final int AMOUNT_MB = 1000;

    public enum CellFluid {
        EMPTY("empty", 0.0f, null),
        WATER("minecraft:water", 0.1f, Blocks.WATER),
        LAVA("minecraft:lava", 0.2f, Blocks.LAVA),
        AIR(ModFluids.AIR_ID.toString(), 0.3f, null);

        public final String id;
        public final float predicate;
        public final @Nullable Block block;

        CellFluid(String id, float predicate, @Nullable Block block) {
            this.id = id;
            this.predicate = predicate;
            this.block = block;
        }

        public static CellFluid byId(@Nullable String id) {
            if (id == null || id.isEmpty() || id.equals("empty")) return EMPTY;
            for (CellFluid value : values()) {
                if (value.id.equals(id)) return value;
            }
            return EMPTY;
        }
    }

    public UniversalFluidCellItem(Settings settings) {
        super(settings.maxCount(64));
    }

    public static ItemStack createStack(CellFluid fluid) {
        ItemStack stack = new ItemStack(com.shipovskijkorp.industriallegacy.registry.ModItems.FLUID_CELL);
        setFluid(stack, fluid);
        return stack;
    }

    public static CellFluid getFluid(ItemStack stack) {
        if (stack.isEmpty()) return CellFluid.EMPTY;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_FLUID)) return CellFluid.EMPTY;
        return CellFluid.byId(nbt.getString(NBT_FLUID));
    }

    public static void setFluid(ItemStack stack, CellFluid fluid) {
        if (fluid == CellFluid.EMPTY) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_FLUID);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putString(NBT_FLUID, fluid.id);
    }

    public static float getModelPredicate(ItemStack stack) {
        return getFluid(stack).predicate;
    }

    public static boolean matchesRequiredFluid(ItemStack stack, @Nullable String requiredFluidId) {
        if (requiredFluidId == null || requiredFluidId.isEmpty()) return true;
        CellFluid fluid = getFluid(stack);
        if ("empty".equals(requiredFluidId)) return fluid == CellFluid.EMPTY;
        return fluid.id.equals(requiredFluidId);
    }

    public static int consumeFluidFromPlayerInventory(PlayerEntity player, CellFluid fluid, int neededMb) {
        int supplied = 0;
        for (int i = 0; i < player.getInventory().main.size() && neededMb > 0; i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof UniversalFluidCellItem)) continue;
            if (getFluid(stack) != fluid) continue;

            while (!stack.isEmpty() && getFluid(stack) == fluid && neededMb > 0) {
                ItemStack emptyCell = createStack(CellFluid.EMPTY);
                stack.decrement(1);
                if (stack.isEmpty()) {
                    player.getInventory().main.set(i, emptyCell);
                    stack = player.getInventory().main.get(i);
                } else if (!player.getInventory().insertStack(emptyCell)) {
                    player.dropItem(emptyCell, false);
                }

                supplied += AMOUNT_MB;
                neededMb -= AMOUNT_MB;
            }
        }
        return supplied;
    }

    @Override
    public Text getName(ItemStack stack) {
        return switch (getFluid(stack)) {
            case WATER -> Text.translatable("item.industrial_legacy.cell.water");
            case LAVA -> Text.translatable("item.industrial_legacy.cell.lava");
            case AIR -> Text.translatable("item.industrial_legacy.cell.air");
            default -> Text.translatable("item.industrial_legacy.cell.empty");
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        CellFluid fluid = getFluid(stack);
        tooltip.add(Text.translatable("tooltip.industrial_legacy.fluid_cell.amount", AMOUNT_MB).formatted(Formatting.DARK_GRAY));
        if (fluid != CellFluid.EMPTY) {
            tooltip.add(Text.literal(fluid.id).formatted(Formatting.GRAY));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        CellFluid fluid = getFluid(stack);

        if (fluid == CellFluid.EMPTY) {
            BlockHitResult hit = raycast(world, user, RaycastContext.FluidHandling.SOURCE_ONLY);
            if (hit.getType() != HitResult.Type.BLOCK) return TypedActionResult.pass(stack);
            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (state.isOf(Blocks.WATER)) {
                if (!world.isClient) fillFromSource(user, hand, stack, CellFluid.WATER, pos);
                return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
            }
            if (state.isOf(Blocks.LAVA)) {
                if (!world.isClient) fillFromSource(user, hand, stack, CellFluid.LAVA, pos);
                return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
            }
            return TypedActionResult.pass(stack);
        }

        if (fluid.block != null) {
            BlockHitResult hit = raycast(world, user, RaycastContext.FluidHandling.NONE);
            if (hit.getType() != HitResult.Type.BLOCK) return TypedActionResult.pass(stack);
            BlockPos pos = hit.getBlockPos();
            Direction side = hit.getSide();
            BlockState state = world.getBlockState(pos);
            BlockPos placePos = state.isReplaceable() ? pos : pos.offset(side);
            BlockState placeState = world.getBlockState(placePos);
            if (!placeState.isReplaceable()) return TypedActionResult.fail(stack);
            if (!world.isClient) {
                world.setBlockState(placePos, fluid.block.getDefaultState(), Block.NOTIFY_ALL);
                replaceHeldWithEmpty(user, hand, stack);
            }
            return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
        }

        return TypedActionResult.pass(stack);
    }

    private static void fillFromSource(PlayerEntity user, Hand hand, ItemStack stack, CellFluid fluid, BlockPos pos) {
        World world = user.getWorld();
        ItemStack filled = createStack(fluid);

        world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

        if (user.getAbilities().creativeMode) {
            if (stack.getCount() == 1) {
                user.setStackInHand(hand, filled);
            } else if (!user.getInventory().insertStack(filled)) {
                user.dropItem(filled, false);
            }
            return;
        }

        stack.decrement(1);
        if (stack.isEmpty()) {
            user.setStackInHand(hand, filled);
        } else if (!user.getInventory().insertStack(filled)) {
            user.dropItem(filled, false);
        }
    }

    private static void replaceHeldWithEmpty(PlayerEntity user, Hand hand, ItemStack stack) {
        if (user.getAbilities().creativeMode) return;
        ItemStack empty = createStack(CellFluid.EMPTY);
        stack.decrement(1);
        if (stack.isEmpty()) {
            user.setStackInHand(hand, empty);
        } else if (!user.getInventory().insertStack(empty)) {
            user.dropItem(empty, false);
        }
    }
}
