package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IC2-style painter/paint roller. The uncolored painter is colorless; colored painters have 32 uses. */
public final class PainterItem extends Item implements IModeSwitchableItem {
    private static final String NBT_AUTO_REFILL = "autoRefill";
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
        BlockState painted = colorBlock(state, color);
        if (painted == null || painted == state) return ActionResult.PASS;

        if (!world.isClient) {
            world.setBlockState(pos, painted, Block.NOTIFY_ALL);
            damagePainter(ctx.getPlayer(), ctx.getHand(), ctx.getStack());
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

    @Nullable
    private static BlockState colorBlock(BlockState state, net.minecraft.util.DyeColor newColor) {

        Block block = state.getBlock();
        Block target = null;

        if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
            target = stainedGlass(newColor);
        } else if (block == Blocks.GLASS_PANE || block instanceof PaneBlock && isStainedGlassPane(block)) {
            target = stainedGlassPane(newColor);
        } else if (block == Blocks.TERRACOTTA || isColoredTerracotta(block)) {
            target = terracotta(newColor);
        } else if (isWool(block)) {
            target = wool(newColor);
        } else if (isCarpet(block)) {
            target = carpet(newColor);
        } else if (isConcrete(block)) {
            target = concrete(newColor);
        } else if (isConcretePowder(block)) {
            target = concretePowder(newColor);
        }

        return target == null || target == block ? null : target.getDefaultState();
    }

    private void damagePainter(@Nullable PlayerEntity player, Hand hand, ItemStack stack) {
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

    private static boolean isWool(Block b) { return b == Blocks.WHITE_WOOL || b == Blocks.ORANGE_WOOL || b == Blocks.MAGENTA_WOOL || b == Blocks.LIGHT_BLUE_WOOL || b == Blocks.YELLOW_WOOL || b == Blocks.LIME_WOOL || b == Blocks.PINK_WOOL || b == Blocks.GRAY_WOOL || b == Blocks.LIGHT_GRAY_WOOL || b == Blocks.CYAN_WOOL || b == Blocks.PURPLE_WOOL || b == Blocks.BLUE_WOOL || b == Blocks.BROWN_WOOL || b == Blocks.GREEN_WOOL || b == Blocks.RED_WOOL || b == Blocks.BLACK_WOOL; }
    private static boolean isCarpet(Block b) { return b == Blocks.WHITE_CARPET || b == Blocks.ORANGE_CARPET || b == Blocks.MAGENTA_CARPET || b == Blocks.LIGHT_BLUE_CARPET || b == Blocks.YELLOW_CARPET || b == Blocks.LIME_CARPET || b == Blocks.PINK_CARPET || b == Blocks.GRAY_CARPET || b == Blocks.LIGHT_GRAY_CARPET || b == Blocks.CYAN_CARPET || b == Blocks.PURPLE_CARPET || b == Blocks.BLUE_CARPET || b == Blocks.BROWN_CARPET || b == Blocks.GREEN_CARPET || b == Blocks.RED_CARPET || b == Blocks.BLACK_CARPET; }
    private static boolean isColoredTerracotta(Block b) { return b == Blocks.WHITE_TERRACOTTA || b == Blocks.ORANGE_TERRACOTTA || b == Blocks.MAGENTA_TERRACOTTA || b == Blocks.LIGHT_BLUE_TERRACOTTA || b == Blocks.YELLOW_TERRACOTTA || b == Blocks.LIME_TERRACOTTA || b == Blocks.PINK_TERRACOTTA || b == Blocks.GRAY_TERRACOTTA || b == Blocks.LIGHT_GRAY_TERRACOTTA || b == Blocks.CYAN_TERRACOTTA || b == Blocks.PURPLE_TERRACOTTA || b == Blocks.BLUE_TERRACOTTA || b == Blocks.BROWN_TERRACOTTA || b == Blocks.GREEN_TERRACOTTA || b == Blocks.RED_TERRACOTTA || b == Blocks.BLACK_TERRACOTTA; }
    private static boolean isConcrete(Block b) { return b == Blocks.WHITE_CONCRETE || b == Blocks.ORANGE_CONCRETE || b == Blocks.MAGENTA_CONCRETE || b == Blocks.LIGHT_BLUE_CONCRETE || b == Blocks.YELLOW_CONCRETE || b == Blocks.LIME_CONCRETE || b == Blocks.PINK_CONCRETE || b == Blocks.GRAY_CONCRETE || b == Blocks.LIGHT_GRAY_CONCRETE || b == Blocks.CYAN_CONCRETE || b == Blocks.PURPLE_CONCRETE || b == Blocks.BLUE_CONCRETE || b == Blocks.BROWN_CONCRETE || b == Blocks.GREEN_CONCRETE || b == Blocks.RED_CONCRETE || b == Blocks.BLACK_CONCRETE; }
    private static boolean isConcretePowder(Block b) { return b == Blocks.WHITE_CONCRETE_POWDER || b == Blocks.ORANGE_CONCRETE_POWDER || b == Blocks.MAGENTA_CONCRETE_POWDER || b == Blocks.LIGHT_BLUE_CONCRETE_POWDER || b == Blocks.YELLOW_CONCRETE_POWDER || b == Blocks.LIME_CONCRETE_POWDER || b == Blocks.PINK_CONCRETE_POWDER || b == Blocks.GRAY_CONCRETE_POWDER || b == Blocks.LIGHT_GRAY_CONCRETE_POWDER || b == Blocks.CYAN_CONCRETE_POWDER || b == Blocks.PURPLE_CONCRETE_POWDER || b == Blocks.BLUE_CONCRETE_POWDER || b == Blocks.BROWN_CONCRETE_POWDER || b == Blocks.GREEN_CONCRETE_POWDER || b == Blocks.RED_CONCRETE_POWDER || b == Blocks.BLACK_CONCRETE_POWDER; }
    private static boolean isStainedGlassPane(Block b) { return b == Blocks.WHITE_STAINED_GLASS_PANE || b == Blocks.ORANGE_STAINED_GLASS_PANE || b == Blocks.MAGENTA_STAINED_GLASS_PANE || b == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE || b == Blocks.YELLOW_STAINED_GLASS_PANE || b == Blocks.LIME_STAINED_GLASS_PANE || b == Blocks.PINK_STAINED_GLASS_PANE || b == Blocks.GRAY_STAINED_GLASS_PANE || b == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE || b == Blocks.CYAN_STAINED_GLASS_PANE || b == Blocks.PURPLE_STAINED_GLASS_PANE || b == Blocks.BLUE_STAINED_GLASS_PANE || b == Blocks.BROWN_STAINED_GLASS_PANE || b == Blocks.GREEN_STAINED_GLASS_PANE || b == Blocks.RED_STAINED_GLASS_PANE || b == Blocks.BLACK_STAINED_GLASS_PANE; }

    private static Block wool(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_WOOL; case ORANGE -> Blocks.ORANGE_WOOL; case MAGENTA -> Blocks.MAGENTA_WOOL; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL; case YELLOW -> Blocks.YELLOW_WOOL; case LIME -> Blocks.LIME_WOOL; case PINK -> Blocks.PINK_WOOL; case GRAY -> Blocks.GRAY_WOOL; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL; case CYAN -> Blocks.CYAN_WOOL; case PURPLE -> Blocks.PURPLE_WOOL; case BLUE -> Blocks.BLUE_WOOL; case BROWN -> Blocks.BROWN_WOOL; case GREEN -> Blocks.GREEN_WOOL; case RED -> Blocks.RED_WOOL; case BLACK -> Blocks.BLACK_WOOL; }; }
    private static Block carpet(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_CARPET; case ORANGE -> Blocks.ORANGE_CARPET; case MAGENTA -> Blocks.MAGENTA_CARPET; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CARPET; case YELLOW -> Blocks.YELLOW_CARPET; case LIME -> Blocks.LIME_CARPET; case PINK -> Blocks.PINK_CARPET; case GRAY -> Blocks.GRAY_CARPET; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CARPET; case CYAN -> Blocks.CYAN_CARPET; case PURPLE -> Blocks.PURPLE_CARPET; case BLUE -> Blocks.BLUE_CARPET; case BROWN -> Blocks.BROWN_CARPET; case GREEN -> Blocks.GREEN_CARPET; case RED -> Blocks.RED_CARPET; case BLACK -> Blocks.BLACK_CARPET; }; }
    private static Block terracotta(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_TERRACOTTA; case ORANGE -> Blocks.ORANGE_TERRACOTTA; case MAGENTA -> Blocks.MAGENTA_TERRACOTTA; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_TERRACOTTA; case YELLOW -> Blocks.YELLOW_TERRACOTTA; case LIME -> Blocks.LIME_TERRACOTTA; case PINK -> Blocks.PINK_TERRACOTTA; case GRAY -> Blocks.GRAY_TERRACOTTA; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_TERRACOTTA; case CYAN -> Blocks.CYAN_TERRACOTTA; case PURPLE -> Blocks.PURPLE_TERRACOTTA; case BLUE -> Blocks.BLUE_TERRACOTTA; case BROWN -> Blocks.BROWN_TERRACOTTA; case GREEN -> Blocks.GREEN_TERRACOTTA; case RED -> Blocks.RED_TERRACOTTA; case BLACK -> Blocks.BLACK_TERRACOTTA; }; }
    private static Block stainedGlass(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_STAINED_GLASS; case ORANGE -> Blocks.ORANGE_STAINED_GLASS; case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS; case YELLOW -> Blocks.YELLOW_STAINED_GLASS; case LIME -> Blocks.LIME_STAINED_GLASS; case PINK -> Blocks.PINK_STAINED_GLASS; case GRAY -> Blocks.GRAY_STAINED_GLASS; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS; case CYAN -> Blocks.CYAN_STAINED_GLASS; case PURPLE -> Blocks.PURPLE_STAINED_GLASS; case BLUE -> Blocks.BLUE_STAINED_GLASS; case BROWN -> Blocks.BROWN_STAINED_GLASS; case GREEN -> Blocks.GREEN_STAINED_GLASS; case RED -> Blocks.RED_STAINED_GLASS; case BLACK -> Blocks.BLACK_STAINED_GLASS; }; }
    private static Block stainedGlassPane(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_STAINED_GLASS_PANE; case ORANGE -> Blocks.ORANGE_STAINED_GLASS_PANE; case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS_PANE; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE; case YELLOW -> Blocks.YELLOW_STAINED_GLASS_PANE; case LIME -> Blocks.LIME_STAINED_GLASS_PANE; case PINK -> Blocks.PINK_STAINED_GLASS_PANE; case GRAY -> Blocks.GRAY_STAINED_GLASS_PANE; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS_PANE; case CYAN -> Blocks.CYAN_STAINED_GLASS_PANE; case PURPLE -> Blocks.PURPLE_STAINED_GLASS_PANE; case BLUE -> Blocks.BLUE_STAINED_GLASS_PANE; case BROWN -> Blocks.BROWN_STAINED_GLASS_PANE; case GREEN -> Blocks.GREEN_STAINED_GLASS_PANE; case RED -> Blocks.RED_STAINED_GLASS_PANE; case BLACK -> Blocks.BLACK_STAINED_GLASS_PANE; }; }
    private static Block concrete(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_CONCRETE; case ORANGE -> Blocks.ORANGE_CONCRETE; case MAGENTA -> Blocks.MAGENTA_CONCRETE; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE; case YELLOW -> Blocks.YELLOW_CONCRETE; case LIME -> Blocks.LIME_CONCRETE; case PINK -> Blocks.PINK_CONCRETE; case GRAY -> Blocks.GRAY_CONCRETE; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE; case CYAN -> Blocks.CYAN_CONCRETE; case PURPLE -> Blocks.PURPLE_CONCRETE; case BLUE -> Blocks.BLUE_CONCRETE; case BROWN -> Blocks.BROWN_CONCRETE; case GREEN -> Blocks.GREEN_CONCRETE; case RED -> Blocks.RED_CONCRETE; case BLACK -> Blocks.BLACK_CONCRETE; }; }
    private static Block concretePowder(net.minecraft.util.DyeColor c) { return switch (c) { case WHITE -> Blocks.WHITE_CONCRETE_POWDER; case ORANGE -> Blocks.ORANGE_CONCRETE_POWDER; case MAGENTA -> Blocks.MAGENTA_CONCRETE_POWDER; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE_POWDER; case YELLOW -> Blocks.YELLOW_CONCRETE_POWDER; case LIME -> Blocks.LIME_CONCRETE_POWDER; case PINK -> Blocks.PINK_CONCRETE_POWDER; case GRAY -> Blocks.GRAY_CONCRETE_POWDER; case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE_POWDER; case CYAN -> Blocks.CYAN_CONCRETE_POWDER; case PURPLE -> Blocks.PURPLE_CONCRETE_POWDER; case BLUE -> Blocks.BLUE_CONCRETE_POWDER; case BROWN -> Blocks.BROWN_CONCRETE_POWDER; case GREEN -> Blocks.GREEN_CONCRETE_POWDER; case RED -> Blocks.RED_CONCRETE_POWDER; case BLACK -> Blocks.BLACK_CONCRETE_POWDER; }; }
}
