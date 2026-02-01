package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2 RE-Battery (re_battery)
 *
 * Canon parameters (IC2 1.12.2):
 * - capacity: 10,000 EU
 * - transferLimit: 100 EU/t
 * - tier: 1
 * - max stack: 16
 *
 * Energy is stored in NBT. Newly crafted battery is empty (0 EU), like IC2.
 */
public final class ReBatteryItem extends Item implements IElectricItem {

    public static final long CAPACITY_EU = 10_000L;
    public static final long TRANSFER_LIMIT_EU_T = 100L;
    public static final int TIER = 1;

    private static final String NBT_ENERGY = "energy";

    public ReBatteryItem(Settings settings) {
        super(settings);
    }

    // --- IElectricItem ---

    @Override
    public long getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > CAPACITY_EU) e = CAPACITY_EU;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        if (stack == null || stack.isEmpty()) return;
        long e = Math.max(0L, Math.min(CAPACITY_EU, energy));

        if (e == 0L) {
            // Keep empty batteries stackable with other empty batteries (no NBT).
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(NBT_ENERGY, e);
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

    // --- UI ---

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        // Show charge bar always (IC2-like).
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        // 0..13
        double r = (double) getEnergy(stack) / (double) CAPACITY_EU;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        long e = getEnergy(stack);
        tooltip.add(Text.literal(e + " / " + CAPACITY_EU + " EU").formatted(Formatting.GRAY));
    }
}
