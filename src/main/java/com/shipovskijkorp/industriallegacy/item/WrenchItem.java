package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * IC2 Experimental wrench behaviour.
 *
 * <p>Right click first tries to rotate an IC2 machine:</p>
 * <ul>
 *     <li>normal use: make the machine face the clicked side;</li>
 *     <li>sneak use: make the machine face the opposite of the clicked side;</li>
 *     <li>horizontal-only machines reject vertical facings, like IC2 machines whose supported
 *     facing set does not contain UP/DOWN.</li>
 * </ul>
 *
 * <p>If rotation does not change the block, the wrench attempts IC2-style machine removal.</p>
 * <ul>
 *     <li>rotation costs 1 durability;</li>
 *     <li>removal costs 10 durability;</li>
 *     <li>wrenched IL machines drop themselves instead of their fallback casing/drop.</li>
 * </ul>
 */
public class WrenchItem extends Item {
    private static final float MACHINE_MINING_SPEED = 12.0f;
    private static final int ROTATE_DAMAGE = 1;
    private static final int REMOVE_DAMAGE = 10;

    public WrenchItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(120));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        ItemStack stack = ctx.getStack();
        PlayerEntity player = ctx.getPlayer();

        if (player == null || !hasDamageBudget(stack, ROTATE_DAMAGE)) {
            return ActionResult.FAIL;
        }

        WrenchResult result = wrenchBlock(ctx, hasDamageBudget(stack, REMOVE_DAMAGE));
        if (result == WrenchResult.NOTHING) {
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            int damage = result == WrenchResult.ROTATED ? ROTATE_DAMAGE : REMOVE_DAMAGE;
            damageWrench(stack, damage, player, ctx.getHand());
        }
        return ActionResult.success(world.isClient);
    }

    public static WrenchResult wrenchBlock(ItemUsageContext ctx, boolean allowRemove) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        PlayerEntity player = ctx.getPlayer();
        if (state.isAir() || player == null) return WrenchResult.NOTHING;

        BlockState rotated = getWrenchRotatedState(state, ctx.getSide(), player.isSneaking());
        if (rotated != null && rotated.canPlaceAt(world, pos)) {
            if (!world.isClient) {
                world.setBlockState(pos, rotated, Block.NOTIFY_ALL);
                playWrenchSound(world, pos);
            }
            return WrenchResult.ROTATED;
        }

        if (allowRemove && wrenchCanRemove(state)) {
            if (!world.isClient) {
                removeWithWrench(world, pos, state, player);
                playWrenchSound(world, pos);
            }
            return WrenchResult.REMOVED;
        }

        return WrenchResult.NOTHING;
    }

    private static BlockState getWrenchRotatedState(BlockState state, Direction clickedSide, boolean sneaking) {
        Direction target = sneaking ? clickedSide.getOpposite() : clickedSide;

        if (state.contains(Properties.FACING)) {
            return rotateFacingProperty(state, Properties.FACING, target);
        }
        if (state.contains(HorizontalFacingBlock.FACING)) {
            if (!target.getAxis().isHorizontal()) return null;
            return rotateFacingProperty(state, HorizontalFacingBlock.FACING, target);
        }
        return null;
    }

    private static BlockState rotateFacingProperty(BlockState state, DirectionProperty property, Direction target) {
        if (!property.getValues().contains(target)) return null;
        Direction current = state.get(property);
        if (current == target) return null;
        return state.with(property, target);
    }

    public static boolean wrenchCanRemove(BlockState state) {
        return isWrenchMineable(state);
    }

    public static void removeWithWrench(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        Block block = state.getBlock();
        if (!player.getAbilities().creativeMode) {
            Block.dropStack(world, pos, new ItemStack(block.asItem()));
        }
        world.removeBlock(pos, false);
    }

    private static boolean hasDamageBudget(ItemStack stack, int cost) {
        return stack.isDamageable() && stack.getDamage() + cost <= stack.getMaxDamage();
    }

    public static void damageWrench(ItemStack stack, int damage, PlayerEntity player, Hand hand) {
        stack.damage(damage, player, p -> p.sendToolBreakStatus(hand));
    }

    public static void playWrenchSound(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.35f, 1.85f);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return isWrenchMineable(state) ? MACHINE_MINING_SPEED : super.getMiningSpeedMultiplier(stack, state);
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return isWrenchMineable(state);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && isWrenchMineable(state)) {
            stack.damage(ROTATE_DAMAGE, miner, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    public static boolean isWrenchMineable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof GeneratorBlock
                || block instanceof GeoGeneratorBlock
                || block instanceof SolarPanelBlock
                || block instanceof RTGeneratorBlock
                || block instanceof BatBoxBlock
                || block instanceof CesuBlock
                || block instanceof MfeBlock
                || block instanceof MfsuBlock
                || block instanceof LvTransformerBlock
                || block instanceof MvTransformerBlock
                || block instanceof HvTransformerBlock
                || block instanceof EvTransformerBlock
                || block instanceof MaceratorBlock
                || block instanceof CompressorBlock
                || block instanceof RecyclerBlock
                || block instanceof MetalFormerBlock
                || block instanceof ElectricFurnaceBlock
                || block instanceof IronFurnaceBlock
                || block instanceof CannerBlock
                || block instanceof SolidCannerBlock
                || block instanceof ThermalCentrifugeBlock
                || block instanceof NuclearReactorBlock
                || block instanceof ReactorChamberBlock
                || block instanceof ChargepadBlock
                || block instanceof LuminatorBlock;
    }

    public enum WrenchResult {
        ROTATED,
        REMOVED,
        NOTHING
    }
}
