package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2 Experimental Nightvision Goggles (ported semantics):
 * - Electric item: 200,000 EU, tier 1, transfer 200 EU/t
 * - When active: consumes 1 EU/t and applies Night Vision in darkness,
 *   but applies Blindness in bright conditions (skylight > 8), like IC2.
 *
 * Toggle is controlled by a global keybind (default N) via a server packet.
 */
public final class NightVisionGogglesItem extends ArmorItem implements IElectricItem, INightVisionModule {

    public static final long CAPACITY_EU = 200_000L;
    public static final long TRANSFER_LIMIT_EU_T = 200L;
    public static final int TIER = 1;

    private static final String NBT_ENERGY = "energy";

    public NightVisionGogglesItem(Settings settings) {
        super(ModArmorMaterials.NIGHTVISION, Type.HELMET, settings);
    }

    // ----- IElectricItem -----

    @Override
    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > CAPACITY_EU) e = CAPACITY_EU;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long e = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (e == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, e);
    }

    @Override
    public long getCapacity(ItemStack stack) {
        return CAPACITY_EU;
    }

    @Override
    public long getTransferLimit(ItemStack stack) {
        return TRANSFER_LIMIT_EU_T;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

        // ----- UI -----

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        double r = (double) getEnergy(stack) / (double) CAPACITY_EU;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(getEnergy(stack) + " / " + CAPACITY_EU + " EU").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(isNightVisionActive(stack)
                ? "message.industrial_legacy.nightvision.state_on"
                : "message.industrial_legacy.nightvision.state_off").formatted(Formatting.DARK_GRAY));
    }

// ----- behavior -----

        @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        // Only works when equipped in helmet slot
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        if (head != stack) return;

        if (!isNightVisionActive(stack)) return;

        // Consume 1 EU per tick (IC2-like)
        long used = ElectricItemManager.discharge(stack, 1L, false);
        if (used < 1L) {
            // Out of power: auto-disable
            setNightVisionActive(stack, false);
            player.sendMessage(Text.translatable("message.industrial_legacy.nightvision.no_power").formatted(Formatting.GRAY), true);
            return;
        }

        // Apply Night Vision (refresh continuously)
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, true));
    }

}
