package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;

/**
 * IC2 Experimental NanoSuit armor (electric).
 *
 * IMPORTANT: Damage absorption is handled in a mixin (IC2-like),
 * not via vanilla armor protection values.
 */
public class NanoArmorItem extends ArmorItem implements IElectricItem {

    public static final long CAPACITY_EU = 1_000_000L; // IC2 Exp NanoSuit
    public static final long TRANSFER_LIMIT_EU_T = 1600L;
    public static final int TIER = 3;

    public static final double DAMAGE_ABSORPTION_RATIO = 0.9; // IC2: ItemArmorNanoSuit.getDamageAbsorptionRatio()
    public static final int ENERGY_PER_DAMAGE = 5000;         // IC2: ItemArmorNanoSuit.getEnergyPerDamage()

    private static final String NBT_ENERGY = "energy";

    public NanoArmorItem(Type type, Settings settings) {
        super(NanoArmorMaterial.INSTANCE, type, settings.maxCount(1));
    }

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
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
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
    public float getChargeRatio(ItemStack stack) {
        return (float) getEnergy(stack) / (float) CAPACITY_EU;
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
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, 3)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", TIER).formatted(Formatting.DARK_GRAY));
    }
}