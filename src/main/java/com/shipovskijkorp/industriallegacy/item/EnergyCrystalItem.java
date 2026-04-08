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
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;

/**
 * IL Energy Crystal (energy_crystal).
 *
 * IC2 1.12.2 Experimental source-of-truth (BlocksItems):
 * - capacity: 1,000,000 EU
 * - transferLimit: 2,048 EU/t
 * - tier: 3
 * - stack: 1
 *
 * Energy is stored in NBT. Newly crafted crystal is empty (0 EU), matching IC2 durability-state @27.
 */
public final class EnergyCrystalItem extends Item implements IElectricItem {

    public static final long CAPACITY_EU = 1_000_000L;
    public static final long TRANSFER_LIMIT_EU_T = 2_048L;
    public static final int TIER = 3;

    private static final String NBT_ENERGY = "energy";

    public EnergyCrystalItem(Settings settings) {
        super(settings);
    }

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
            // Keep empty crystals without NBT where possible.
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

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        long cap = getCapacity(stack);
        if (cap <= 0L) return false;
        return getEnergy(stack) < cap;
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
        long e = getEnergy(stack);
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(e, CAPACITY_EU, 3)).formatted(Formatting.GRAY));
    }
}
