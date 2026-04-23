package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IL Lapotron Crystal (lapotron_crystal).
 *
 * IC2 Experimental source-of-truth (BlocksItems):
 * - capacity: 10,000,000 EU
 * - transferLimit: 8,092 EU/t
 * - tier: 4
 * - stack: 1
 */
public final class LapotronCrystalItem extends Item implements IElectricItem {
    public static final long CAPACITY_EU = 10_000_000L;
    public static final long TRANSFER_LIMIT_EU_T = 8_092L;
    public static final int TIER = 4;

    private static final String NBT_ENERGY = "energy";

    public LapotronCrystalItem(Settings settings) {
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
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        long e = getEnergy(stack);
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(e, CAPACITY_EU, 4)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", TIER).formatted(Formatting.DARK_GRAY));
    }
}