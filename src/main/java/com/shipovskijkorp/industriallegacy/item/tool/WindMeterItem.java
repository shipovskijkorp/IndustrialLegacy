package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.block.entity.WindKineticGeneratorBlockEntity;
import com.shipovskijkorp.industriallegacy.world.WindSimulation;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** IC2 Experimental wind meter: 50 EU/use, 10k EU capacity, tier 1. */
public final class WindMeterItem extends AbstractElectricToolItem {
    private static final long COST = 50L;

    public WindMeterItem(Settings settings) {
        super(settings, COST, 0, 10_000L, 100L, 1, 1.0f);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient || ctx.getPlayer() == null || ctx.getPlayer().isSneaking()) {
            return ActionResult.PASS;
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        BlockPos pos = ctx.getBlockPos();
        if (world.getBlockEntity(pos) instanceof WindKineticGeneratorBlockEntity wind) {
            ItemStack stack = ctx.getStack();
            PlayerEntity player = ctx.getPlayer();
            if (!canUse(stack, COST)) {
                return ActionResult.PASS;
            }
            if (!wind.isActiveForWindMeter()) {
                if (wind.hasRotor()) {
                    player.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.rotor_blocked"), true);
                } else {
                    player.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.rotor_none"), true);
                }
                return ActionResult.FAIL;
            }

            useEnergy(stack, player, COST);
            if (wind.getObstructions() >= 0) {
                double displayWind = roundWind(wind.calculateWindStrength(serverWorld));
                if (displayWind <= 0.0D) {
                    player.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.obstructed", wind.getObstructions()), true);
                } else {
                    player.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.effective", displayWind), true);
                }
            } else {
                player.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.blocked", wind.getRotorDiameter() * 3), true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        if (!(world instanceof ServerWorld serverWorld)) {
            return TypedActionResult.pass(stack);
        }
        if (!canUse(stack, COST)) {
            return TypedActionResult.pass(stack);
        }
        useEnergy(stack, user, COST);
        double wind = Math.max(0.0D, WindSimulation.get(serverWorld).getWindAt(serverWorld, user.getY()));
        user.sendMessage(Text.translatable("message.industrial_legacy.wind_meter.wind", roundWind(wind)), true);
        return TypedActionResult.success(stack);
    }

    private static double roundWind(double windStrength) {
        return Math.round(windStrength * 100.0D) / 100.0D;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        return true;
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return false;
    }
}
