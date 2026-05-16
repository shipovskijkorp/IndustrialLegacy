package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.FoamConcreteBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.net.EuNetwork;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IC2-style painter/paint roller. The uncolored painter is colorless; colored painters have 32 uses. */
public final class PainterItem extends Item implements IModeSwitchableItem {
    private static final String NBT_AUTO_REFILL = "autoRefill";
    private static final String[] COLOR_PREFIXES = new String[] {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    @Nullable
    private final net.minecraft.util.DyeColor color;

    public PainterItem(Settings settings, @Nullable net.minecraft.util.DyeColor color) {
        super(color == null ? settings.maxCount(1) : settings.maxCount(1).maxDamage(32));
        this.color = color;
    }

    @Nullable
    public net.minecraft.util.DyeColor getColor() {
        return color;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        if (color == null) return ActionResult.PASS;

        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock && world.getBlockEntity(pos) instanceof CableBlockEntity cableBe) {
            if (!cableBe.canBeColored()) return ActionResult.PASS;
            if (!world.isClient && cableBe.recolor(color)) {
                EuNetwork.invalidate(world, pos);
                damagePainter(ctx.getPlayer(), ctx.getHand(), ctx.getStack());
            }
            return ActionResult.success(world.isClient);
        }

        if (state.getBlock() instanceof FoamConcreteBlock && state.contains(FoamConcreteBlock.COLOR)) {
            if (state.get(FoamConcreteBlock.COLOR) == color) return ActionResult.PASS;
            if (!world.isClient && world.setBlockState(pos, state.with(FoamConcreteBlock.COLOR, color), Block.NOTIFY_ALL)) {
                damagePainter(ctx.getPlayer(), ctx.getHand(), ctx.getStack());
            }
            return ActionResult.success(world.isClient);
        }

        Block targetBlock = getColoredBlock(state.getBlock(), color);
        if (targetBlock == null || targetBlock == state.getBlock()) return ActionResult.PASS;

        if (!world.isClient) {
            boolean changed;
            if (state.getBlock() instanceof BedBlock) {
                changed = paintBed(world, pos, state, targetBlock);
            } else {
                BlockState painted = copySharedProperties(state, targetBlock.getDefaultState());
                changed = painted != state && world.setBlockState(pos, painted, Block.NOTIFY_ALL);
            }

            if (changed) {
                damagePainter(ctx.getPlayer(), ctx.getHand(), ctx.getStack());
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (color == null) return ActionResult.PASS;
        if (!(entity instanceof SheepEntity sheep) || sheep.getColor() == color) return ActionResult.PASS;

        if (!user.getWorld().isClient) {
            sheep.setColor(color);
            damagePainter(user, hand, stack);
        }
        return ActionResult.success(user.getWorld().isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.pass(user.getStackInHand(hand));
    }

    /**
     * BuildCraft-style suffix recoloring: white_wool -> red_wool, blue_bed -> lime_bed, etc.
     * A suffix is accepted only if the registry contains the full 16-color family, which prevents
     * false positives like brown_mushroom -> red_mushroom.
     */
    @Nullable
    private static Block getColoredBlock(Block block, net.minecraft.util.DyeColor color) {
        Identifier id = Registries.BLOCK.getId(block);
        String namespace = id.getNamespace();
        String path = id.getPath();
        String targetPrefix = color.asString();

        String specialSuffix = uncoloredSuffix(path);
        if (specialSuffix != null) {
            return findBlock(namespace, targetPrefix + "_" + specialSuffix);
        }

        String suffix = stripColorPrefix(path);
        if (suffix == null) return null;
        if (!hasFullColorFamily(namespace, suffix)) return null;

        return findBlock(namespace, targetPrefix + "_" + suffix);
    }

    @Nullable
    private static String uncoloredSuffix(String path) {
        return switch (path) {
            case "glass" -> "stained_glass";
            case "glass_pane" -> "stained_glass_pane";
            case "terracotta" -> "terracotta";
            case "shulker_box" -> "shulker_box";
            case "candle" -> "candle";
            default -> null;
        };
    }

    @Nullable
    private static String stripColorPrefix(String path) {
        for (String prefix : COLOR_PREFIXES) {
            String needle = prefix + "_";
            if (path.startsWith(needle) && path.length() > needle.length()) {
                return path.substring(needle.length());
            }
        }
        return null;
    }

    private static boolean hasFullColorFamily(String namespace, String suffix) {
        for (String prefix : COLOR_PREFIXES) {
            if (findBlock(namespace, prefix + "_" + suffix) == null) return false;
        }
        return true;
    }

    @Nullable
    private static Block findBlock(String namespace, String path) {
        Identifier id = new Identifier(namespace, path);
        Block block = Registries.BLOCK.get(id);
        return Registries.BLOCK.getId(block).equals(id) ? block : null;
    }

    private static boolean paintBed(World world, BlockPos clickedPos, BlockState clickedState, Block targetBlock) {
        Direction facing = clickedState.get(BedBlock.FACING);
        BedPart clickedPart = clickedState.get(BedBlock.PART);
        BlockPos footPos = clickedPart == BedPart.FOOT ? clickedPos : clickedPos.offset(facing.getOpposite());
        BlockPos headPos = clickedPart == BedPart.FOOT ? clickedPos.offset(facing) : clickedPos;

        BlockState footState = world.getBlockState(footPos);
        BlockState headState = world.getBlockState(headPos);
        if (!(footState.getBlock() instanceof BedBlock) || !(headState.getBlock() instanceof BedBlock)) return false;
        if (footState.get(BedBlock.PART) != BedPart.FOOT || headState.get(BedBlock.PART) != BedPart.HEAD) return false;
        if (footState.get(BedBlock.FACING) != facing || headState.get(BedBlock.FACING) != facing) return false;

        BlockState newFoot = copySharedProperties(footState, targetBlock.getDefaultState()).with(BedBlock.PART, BedPart.FOOT);
        BlockState newHead = copySharedProperties(headState, targetBlock.getDefaultState()).with(BedBlock.PART, BedPart.HEAD);
        world.setBlockState(footPos, newFoot, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        world.setBlockState(headPos, newHead, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copySharedProperties(BlockState from, BlockState to) {
        BlockState result = to;
        for (Property property : from.getProperties()) {
            if (result.contains(property)) {
                Comparable value = from.get(property);
                if (property.getValues().contains(value)) {
                    result = result.with(property, value);
                }
            }
        }
        return result;
    }

    public void damagePainter(@Nullable PlayerEntity player, Hand hand, ItemStack stack) {
        if (player == null || player.getAbilities().creativeMode || color == null) return;

        if (stack.getDamage() >= stack.getMaxDamage() - 1) {
            if (isAutoRefill(stack) && consumeDye(player, color)) {
                stack.setDamage(0);
            } else {
                player.setStackInHand(hand, new ItemStack(ModItems.PAINTER));
            }
        } else {
            stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
        }
    }

    private static boolean consumeDye(PlayerEntity player, net.minecraft.util.DyeColor color) {
        Item dye = dyeItem(color);
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack candidate = player.getInventory().getStack(i);
            if (!candidate.isEmpty() && candidate.isOf(dye)) {
                candidate.decrement(1);
                return true;
            }
        }
        return false;
    }

    private static boolean isAutoRefill(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_AUTO_REFILL);
    }

    @Override
    public int cycleMode(ItemStack stack, net.minecraft.server.network.ServerPlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        boolean newValue = !nbt.getBoolean(NBT_AUTO_REFILL);
        nbt.putBoolean(NBT_AUTO_REFILL, newValue);
        return newValue ? 1 : 0;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable(isAutoRefill(stack)
                ? "message.industrial_legacy.painter.auto_refill.enabled"
                : "message.industrial_legacy.painter.auto_refill.disabled");
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        if (color != null) {
            tooltip.add(Text.translatable(dyeItem(color).getTranslationKey()));
            tooltip.add(Text.translatable("tooltip.industrial_legacy.painter.auto_refill", getModeName(stack)));
        }
    }

    private static Item dyeItem(net.minecraft.util.DyeColor color) {
        return switch (color) {
            case WHITE -> net.minecraft.item.Items.WHITE_DYE;
            case ORANGE -> net.minecraft.item.Items.ORANGE_DYE;
            case MAGENTA -> net.minecraft.item.Items.MAGENTA_DYE;
            case LIGHT_BLUE -> net.minecraft.item.Items.LIGHT_BLUE_DYE;
            case YELLOW -> net.minecraft.item.Items.YELLOW_DYE;
            case LIME -> net.minecraft.item.Items.LIME_DYE;
            case PINK -> net.minecraft.item.Items.PINK_DYE;
            case GRAY -> net.minecraft.item.Items.GRAY_DYE;
            case LIGHT_GRAY -> net.minecraft.item.Items.LIGHT_GRAY_DYE;
            case CYAN -> net.minecraft.item.Items.CYAN_DYE;
            case PURPLE -> net.minecraft.item.Items.PURPLE_DYE;
            case BLUE -> net.minecraft.item.Items.BLUE_DYE;
            case BROWN -> net.minecraft.item.Items.BROWN_DYE;
            case GREEN -> net.minecraft.item.Items.GREEN_DYE;
            case RED -> net.minecraft.item.Items.RED_DYE;
            case BLACK -> net.minecraft.item.Items.BLACK_DYE;
        };
    }
}
