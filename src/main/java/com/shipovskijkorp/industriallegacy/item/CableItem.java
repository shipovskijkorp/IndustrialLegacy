package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import com.shipovskijkorp.industriallegacy.item.tool.PainterItem;

public class CableItem extends Item {
    public static final String NBT_KIND = "kind";
    public static final String NBT_INSULATION = "insulation";
    public static final String NBT_COLOR = "color";

    // 0..3, only for COPPER + insulation=0
    public static final String NBT_OXIDATION = "ox";

    private static final int[] OX_LOSS_MULT = {1, 2, 3, 10};

    private static final DecimalFormat LOSS_FORMAT =
            new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));

    public CableItem(Settings settings) {
        super(settings);
    }

    public static int getOxidation(ItemStack stack) {
        if (!stack.hasNbt()) return 0;
        return Math.max(0, Math.min(3, stack.getNbt().getInt(NBT_OXIDATION)));
    }

    private static int lossMultiplier(ItemStack stack) {
        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);
        if (kind == CableKind.COPPER && ins == 0) {
            return OX_LOSS_MULT[getOxidation(stack)];
        }
        return 1;
    }

    private static String oxidationKey(int ox) {
        return switch (ox) {
            case 1 -> "tooltip." + IndustrialLegacy.MOD_ID + ".oxidation.exposed";
            case 2 -> "tooltip." + IndustrialLegacy.MOD_ID + ".oxidation.weathered";
            case 3 -> "tooltip." + IndustrialLegacy.MOD_ID + ".oxidation.oxidized";
            default -> "tooltip." + IndustrialLegacy.MOD_ID + ".oxidation.clean";
        };
    }

    public static ItemStack createStack(Item cableItem, CableKind kind, int insulation) {
        return createStack(cableItem, kind, insulation, 0, -1);
    }

    public static ItemStack createStack(Item cableItem, CableKind kind, int insulation, int oxidation) {
        return createStack(cableItem, kind, insulation, oxidation, -1);
    }

    public static ItemStack createStack(Item cableItem, CableKind kind, int insulation, int oxidation, int color) {
        ItemStack stack = new ItemStack(cableItem);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_KIND, kind.id());
        int clampedInsulation = Math.max(0, Math.min(kind.maxInsulation, insulation));
        nbt.putInt(NBT_INSULATION, clampedInsulation);
        setColor(stack, color);
        setOxidation(stack, oxidation);
        syncVisualVariant(stack);
        return stack;
    }

    public static void setOxidation(ItemStack stack, int oxidation) {
        if (stack == null || stack.isEmpty()) return;
        CableKind kind = getKind(stack);
        int insulation = getInsulation(stack);
        NbtCompound nbt = stack.getOrCreateNbt();
        if (kind == CableKind.COPPER && insulation == 0) {
            nbt.putInt(NBT_OXIDATION, Math.max(0, Math.min(3, oxidation)));
        } else {
            nbt.remove(NBT_OXIDATION);
        }
        syncVisualVariant(stack);
    }

    public static void syncVisualVariant(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        CableKind kind = getKind(stack);
        int insulation = getInsulation(stack);
        int oxidation = (kind == CableKind.COPPER && insulation == 0) ? getOxidation(stack) : 0;
        int variant = CableVariants.variantId(kind, insulation, oxidation);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putInt(CableVariants.NBT_VARIANT, variant);
        nbt.putInt("CustomModelData", variant);
    }

    public static CableKind getKind(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return CableKind.COPPER;
        return CableKind.fromId(nbt.getString(NBT_KIND));
    }

    public static int getInsulation(ItemStack stack) {
        CableKind kind = getKind(stack);
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return 0;
        return Math.max(0, Math.min(kind.maxInsulation, nbt.getInt(NBT_INSULATION)));
    }

    public static int getColor(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return -1;
        int color = nbt.contains(NBT_COLOR) ? nbt.getInt(NBT_COLOR) : -1;
        return color < 0 ? -1 : DyeColor.byId(color).getId();
    }

    public static void setColor(ItemStack stack, int color) {
        if (stack == null || stack.isEmpty()) return;
        CableKind kind = getKind(stack);
        int insulation = getInsulation(stack);
        NbtCompound nbt = stack.getOrCreateNbt();
        if (color >= 0 && kind.canBeColored(insulation)) {
            nbt.putInt(NBT_COLOR, DyeColor.byId(color).getId());
        } else {
            nbt.remove(NBT_COLOR);
        }
    }

    public static String colorName(int color) {
        return color < 0 ? "black" : DyeColor.byId(color).asString();
    }

    public static String colorTexturePath(String baseTexturePath, int color) {
        if (color < 0) return baseTexturePath;
        String suffix = "_" + colorName(color);
        if (baseTexturePath.endsWith("_black")) {
            return baseTexturePath.substring(0, baseTexturePath.length() - "_black".length()) + suffix;
        }
        return baseTexturePath;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);
        String model = CableVariants.modelName(kind, ins);
        return "item." + IndustrialLegacy.MOD_ID + ".cable." + model;
    }

    /**
     * ✅ Dynamic name: adds oxidation stage for copper (uninsulated) stacks.
     * Example: "Copper Cable" -> "Copper Cable (Exposed)"
     */
    @Override
    public Text getName(ItemStack stack) {
        Text base = Text.translatable(getTranslationKey(stack));

        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);

        MutableText out = base.copy();

        if (kind == CableKind.COPPER && ins == 0) {
            int ox = getOxidation(stack);
            // Always show stage, even clean, to make it obvious in creative
            out.append(Text.literal(" (").formatted(Formatting.DARK_GRAY));
            out.append(Text.translatable(oxidationKey(ox)).formatted(Formatting.GRAY));
            out.append(Text.literal(")").formatted(Formatting.DARK_GRAY));
        }

        int color = getColor(stack);
        if (color >= 0) {
            out.append(Text.literal(" (").formatted(Formatting.DARK_GRAY));
            out.append(Text.translatable("color.minecraft." + DyeColor.byId(color).asString()).formatted(Formatting.GRAY));
            out.append(Text.literal(")").formatted(Formatting.DARK_GRAY));
        }

        return out;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);

        syncVisualVariant(stack);

        // capacity
        tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.capacity", kind.capacity)
                .formatted(Formatting.GRAY));

        // loss (effective)
        int mult = lossMultiplier(stack);
        double effectiveLoss = kind.loss * mult;

        int color = getColor(stack);
        if (color >= 0) {
            tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.color",
                            Text.translatable("color.minecraft." + DyeColor.byId(color).asString()))
                    .formatted(Formatting.GRAY));
        }

        if (kind == CableKind.COPPER && ins == 0) {
            int ox = getOxidation(stack);
            tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.oxidation",
                            Text.translatable(oxidationKey(ox)))
                    .formatted(Formatting.GRAY));

            tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.loss",
                            LOSS_FORMAT.format(effectiveLoss))
                    .formatted(Formatting.GRAY));

            tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.loss_mult", mult)
                    .formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.loss",
                            LOSS_FORMAT.format(kind.loss))
                    .formatted(Formatting.GRAY));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        syncVisualVariant(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        ItemStack stack = context.getStack();

        BlockState clicked = world.getBlockState(pos);
        ItemPlacementContext placementContext = new ItemPlacementContext(context);
        if (!clicked.canReplace(placementContext)) {
            pos = pos.offset(side);
        }

        BlockState targetState = world.getBlockState(pos);
        if (targetState.getBlock() instanceof com.shipovskijkorp.industriallegacy.block.CableBlock) {
            return ActionResult.FAIL;
        }

        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);

        Block cableBlock = ModBlocks.getCableBlock(kind, ins);
        BlockState placeState = cableBlock.getPlacementState(placementContext);
        if (placeState == null) placeState = cableBlock.getDefaultState();

        if (!world.canSetBlock(pos)) return ActionResult.FAIL;
        if (!placeState.canPlaceAt(world, pos)) return ActionResult.FAIL;

        if (!world.setBlockState(pos, placeState, Block.NOTIFY_ALL)) {
            return ActionResult.FAIL;
        }

        BlockSoundGroup snd = placeState.getSoundGroup();
        world.playSound(
                context.getPlayer(),
                pos,
                snd.getPlaceSound(),
                SoundCategory.BLOCKS,
                (snd.getVolume() + 1.0f) / 2.0f,
                snd.getPitch() * 0.8f
        );

        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof CableBlockEntity cableBe) {
                if (kind == CableKind.COPPER && ins == 0) {
                    cableBe.setOxidationLevel(getOxidation(stack));
                }
                int color = getColor(stack);
                boolean colorAppliedFromStack = color >= 0 && cableBe.setColor(color);
                if (!colorAppliedFromStack && color < 0 && context.getHand() == Hand.MAIN_HAND && context.getPlayer() != null) {
                    ItemStack offStack = context.getPlayer().getOffHandStack();
                    if (offStack.getItem() instanceof PainterItem painter && painter.getColor() != null && cableBe.recolor(painter.getColor())) {
                        painter.damagePainter(context.getPlayer(), Hand.OFF_HAND, offStack);
                    }
                }
                cableBe.refreshDerivedState();
            }
            EuNetwork.invalidate(world, pos);
            for (Direction dir : Direction.values()) {
                EuNetwork.invalidate(world, pos.offset(dir));
            }
        }

        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.success(world.isClient);
    }
}
