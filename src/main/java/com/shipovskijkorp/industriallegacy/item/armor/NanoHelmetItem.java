package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * NanoSuit helmet provides night vision (IC2-like) when toggled on.
 *
 * IC2 Exp behaviour:
 * - consumes 1 EU/t when active
 * - in bright light (> 8), applies blindness instead of night vision
 */
public final class NanoHelmetItem extends NanoArmorItem implements INightVisionModule {

    private static final String NBT_NIGHTVISION = "Nightvision";

    public NanoHelmetItem(Settings settings) {
        super(Type.HELMET, settings);
    }

    @Override
    public boolean isNightVisionActive(ItemStack stack) {
        return stack.hasNbt() && stack.getNbt().getBoolean(NBT_NIGHTVISION);
    }

    @Override
    public void setNightVisionActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean(NBT_NIGHTVISION, active);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        if (head != stack) return;

        if (!isNightVisionActive(stack)) return;

        // 1 EU/t
        long used = drainIgnoreLimit(stack, 1L, false);
        if (used < 1L) {
            setNightVisionActive(stack, false);
            player.sendMessage(Text.translatable("message.industrial_legacy.nightvision.no_power").formatted(Formatting.GRAY), true);
            return;
        }

        BlockPos pos = player.getBlockPos();
        int skylight = player.getEntityWorld().getLightLevel(pos);

        if (skylight > 8) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, true, true));
        } else {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, true));
        }
    }

    private static long drainIgnoreLimit(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof com.shipovskijkorp.industriallegacy.energy.item.IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }
}
