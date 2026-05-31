package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModFluids;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
        EMPTY("empty", 0.0f, 0x00FFFFFF, false),
        WATER("minecraft:water", 0.1f, 0xFF3F76E4, true),
        LAVA("minecraft:lava", 0.2f, 0xFFFF6A00, true),
        AIR(ModFluids.AIR_ID.toString(), 0.3f, 0xFFDCDCDC, false),
        DISTILLED_WATER("industrial_legacy:distilled_water", 0.4f, 0xFF4356F5, true),
        COOLANT("industrial_legacy:coolant", 0.5f, 0xFF145A6A, true),
        BIOMASS("industrial_legacy:biomass", 0.6f, 0xFF376F25, true),
        CONSTRUCTION_FOAM("industrial_legacy:construction_foam", 0.7f, 0xFF202020, true),
        HOT_WATER("industrial_legacy:hot_water", 0.8f, 0xFF46DEFF, true),
        HOT_COOLANT("industrial_legacy:hot_coolant", 0.9f, 0xFFB52834, true),
        PAHOEHOE_LAVA("industrial_legacy:pahoehoe_lava", 1.0f, 0xFF7B746C, true),
        BIOGAS("industrial_legacy:biogas", 1.1f, 0xFFA7984C, true),
        STEAM("industrial_legacy:steam", 1.2f, 0xFFBCBCBC, true),
        SUPERHEATED_STEAM("industrial_legacy:superheated_steam", 1.3f, 0xFFCAD1D1, true),
        UU_MATTER("industrial_legacy:uu_matter", 1.4f, 0xFF3B0533, true),
        WEED_EX("industrial_legacy:weed_ex", 1.5f, 0xFF074F14, true),
        HEAVY_WATER("industrial_legacy:heavy_water", 1.6f, 0xFF4356F5, true),
        HYDROGEN("industrial_legacy:hydrogen", 1.7f, 0xFFDCDCDC, true),
        OXYGEN("industrial_legacy:oxygen", 1.8f, 0xFFDCDCDC, true),
        CREOSOTE("industrial_legacy:creosote", 1.9f, 0xFF3D390A, true),
        MILK("industrial_legacy:milk", 2.0f, 0xFFFCFCFC, true);

        public final String id;
        public final float predicate;
        private final int tintArgb;
        private final boolean placeableFromCell;

        CellFluid(String id, float predicate, int tintArgb, boolean placeableFromCell) {
            this.id = id;
            this.predicate = predicate;
            this.tintArgb = tintArgb;
            this.placeableFromCell = placeableFromCell;
        }

        public int tintArgb() {
            return tintArgb;
        }

        public String langPath() {
            return switch (this) {
                case EMPTY -> "empty";
                default -> Identifier.tryParse(id) != null ? Identifier.tryParse(id).getPath() : name().toLowerCase(java.util.Locale.ROOT);
            };
        }

        public Text fluidName() {
            if (this == EMPTY) return Text.translatable("fluid.industrial_legacy.empty");
            if (this == WATER) return Text.translatable("block.minecraft.water");
            if (this == LAVA) return Text.translatable("block.minecraft.lava");
            return Text.translatable("fluid.industrial_legacy." + langPath());
        }

        public Text cellName() {
            return Text.translatable("item.industrial_legacy.cell." + langPath());
        }

        public @Nullable Block placeableBlock() {
            if (!placeableFromCell) return null;
            if (this == WATER) return Blocks.WATER;
            if (this == LAVA) return Blocks.LAVA;
            Block block = ModFluids.getFluidBlock(id);
            return block == null || block == Blocks.AIR ? null : block;
        }

        public static CellFluid byId(@Nullable String id) {
            String normalized = normalizeFluidId(id);
            if (normalized == null || normalized.isEmpty() || normalized.equals("empty")) return EMPTY;
            for (CellFluid value : values()) {
                if (value.id.equals(normalized)) return value;
            }
            return EMPTY;
        }

        public static String normalizeFluidId(@Nullable String rawId) {
            if (rawId == null || rawId.isBlank()) return "empty";
            String token = rawId.trim();
            if (token.equals("empty")) return "empty";
            if (token.equals("water")) return "minecraft:water";
            if (token.equals("lava")) return "minecraft:lava";
            if (token.equals("industrial_legacywater")) return "minecraft:water";
            if (token.equals("industrial_legacylava")) return "minecraft:lava";
            if (token.startsWith("industrial_legacy:") && token.length() > 4) {
                return IndustrialLegacy.MOD_ID + ":" + token.substring(4);
            }
            if (token.startsWith("industrial_legacy") && token.length() > 3 && token.indexOf(':') < 0) {
                return IndustrialLegacy.MOD_ID + ":" + token.substring(3);
            }
            if (token.indexOf(':') < 0) {
                return IndustrialLegacy.MOD_ID + ":" + token;
            }
            return token;
        }

        public static CellFluid byBlock(Block block) {
            if (block == Blocks.WATER) return WATER;
            if (block == Blocks.LAVA) return LAVA;
            Identifier id = Registries.BLOCK.getId(block);
            if (IndustrialLegacy.MOD_ID.equals(id.getNamespace())) {
                return byId(id.toString());
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

    public static int getFluidTintRgb(ItemStack stack) {
        return getFluid(stack).tintArgb() & 0x00FFFFFF;
    }

    public static boolean isFilled(ItemStack stack) {
        return getFluid(stack) != CellFluid.EMPTY;
    }

    public static boolean matchesRequiredFluid(ItemStack stack, @Nullable String requiredFluidId) {
        if (requiredFluidId == null || requiredFluidId.isEmpty()) return true;
        CellFluid fluid = getFluid(stack);
        if ("empty".equals(requiredFluidId)) return fluid == CellFluid.EMPTY;
        return fluid.id.equals(CellFluid.normalizeFluidId(requiredFluidId));
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
        return getFluid(stack).cellName();
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
            FluidState fluidState = world.getFluidState(pos);

            CellFluid sourceFluid = CellFluid.byBlock(state.getBlock());
            if (sourceFluid != CellFluid.EMPTY && fluidState.isStill()) {
                if (!world.isClient) fillFromSource(user, hand, stack, sourceFluid, pos);
                return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
            }
            return TypedActionResult.pass(stack);
        }

        Block placeBlock = fluid.placeableBlock();
        if (placeBlock != null) {
            BlockHitResult hit = raycast(world, user, RaycastContext.FluidHandling.NONE);
            if (hit.getType() != HitResult.Type.BLOCK) return TypedActionResult.pass(stack);
            BlockPos pos = hit.getBlockPos();
            Direction side = hit.getSide();
            BlockState state = world.getBlockState(pos);
            BlockPos placePos = state.isReplaceable() ? pos : pos.offset(side);
            BlockState placeState = world.getBlockState(placePos);
            if (!placeState.isReplaceable()) return TypedActionResult.fail(stack);
            if (!world.isClient) {
                world.setBlockState(placePos, placeBlock.getDefaultState(), Block.NOTIFY_ALL);
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
