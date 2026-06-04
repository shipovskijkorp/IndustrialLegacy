package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.registry.ModStatusEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** IC2 experimental iodine tablet port. */
public class IodineTabletItem extends Item {
    public IodineTabletItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return new TypedActionResult<>(ActionResult.PASS, stack);
        }
        return onEaten(world, player, stack);
    }

    private TypedActionResult<ItemStack> onEaten(World world, PlayerEntity player, ItemStack stack) {
        StatusEffectInstance radiation = player.getStatusEffect(ModStatusEffects.RADIATION);
        if (radiation == null) {
            return new TypedActionResult<>(ActionResult.PASS, stack);
        }

        int durationSeconds = radiation.getDuration() / 20;
        int amount = Math.min(stack.getCount(), durationSeconds);
        if (amount <= 0) {
            return new TypedActionResult<>(ActionResult.PASS, stack);
        }

        player.removeStatusEffect(ModStatusEffects.RADIATION);
        if (amount < durationSeconds) {
            player.addStatusEffect(new StatusEffectInstance(ModStatusEffects.RADIATION, (durationSeconds - amount) * 20));
        }

        stack.decrement(amount);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0F, 1.0F);
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }
}
