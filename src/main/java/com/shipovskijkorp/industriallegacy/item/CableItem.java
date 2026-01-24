package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * IC2-style multi-variant cable item.
 *
 * <p>Variants are encoded in NBT ({@code kind}+{@code insulation}) and additionally mirrored
 * into vanilla {@code CustomModelData} for resource-pack driven item models.</p>
 */
public class CableItem extends Item {
    public static final String NBT_KIND = "kind";            // string id ("copper", "tin", ...)
    public static final String NBT_INSULATION = "insulation"; // int
    public static final String NBT_COLOR = "color";          // int ARGB or -1 (reserved)

    private static final DecimalFormat LOSS_FORMAT = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));

    public CableItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createStack(Item cableItem, CableKind kind, int insulation) {
        ItemStack stack = new ItemStack(cableItem);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(NBT_KIND, kind.id());
        int clampedInsulation = Math.max(0, Math.min(kind.maxInsulation, insulation));
        nbt.putInt(NBT_INSULATION, clampedInsulation);

        // Keep the legacy "variant" int for item model overrides (models/item/cable.json).
        int variant = CableVariants.variantId(kind, clampedInsulation);
        nbt.putInt(CableVariants.NBT_VARIANT, variant);

        // Vanilla item model override hook.
        nbt.putInt("CustomModelData", variant);
        return stack;
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
        return nbt.contains(NBT_COLOR) ? nbt.getInt(NBT_COLOR) : -1;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        CableKind kind = getKind(stack);
        int ins = getInsulation(stack);
        String model = CableVariants.modelName(kind, ins);
        return "item." + IndustrialLegacy.MOD_ID + ".cable." + model;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        CableKind kind = getKind(stack);

        // IC2-style cable tooltip:
        //  - transfer limit (EU/t)
        //  - conduction loss (EU/Block)
        tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.capacity", kind.capacity)
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip." + IndustrialLegacy.MOD_ID + ".cable.loss", LOSS_FORMAT.format(kind.loss))
                .formatted(Formatting.GRAY));
    }

    /**
     * Place the cable block variant.
     *
     * <p>The block itself is TE-rendered (thin geometry + connection arms), matching IC2 behavior.</p>
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        Direction side = context.getSide();
        ItemStack stack = context.getStack();

        // Vanilla BlockItem-like replacement logic.
        BlockState clicked = world.getBlockState(pos);
        ItemPlacementContext placementContext = new ItemPlacementContext(context);
        if (!clicked.canReplace(placementContext)) {
            pos = pos.offset(side);
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

        // Play place sound like vanilla BlockItem.
        BlockSoundGroup snd = placeState.getSoundGroup();
        world.playSound(
                context.getPlayer(),
                pos,
                snd.getPlaceSound(),
                SoundCategory.BLOCKS,
                (snd.getVolume() + 1.0f) / 2.0f,
                snd.getPitch() * 0.8f
        );

        // Initialize BE derived state (splitter active/inactive). Detector updates via ticking.
        if (!world.isClient) {
            if (world.getBlockEntity(pos) instanceof CableBlockEntity cableBe) {
                cableBe.refreshDerivedState();
            }
        }

        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.success(world.isClient);
    }
}
