package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import com.shipovskijkorp.industriallegacy.util.RadiationUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2 Experimental QuantumSuit electric armor base.
 */
public class QuantumArmorItem extends ArmorItem implements IElectricItem {
    public static final long CAPACITY_EU = 10_000_000L;
    public static final long TRANSFER_LIMIT_EU_T = 12_000L;
    public static final int TIER = 4;
    public static final int ENERGY_PER_DAMAGE = 20_000;

    private static final String NBT_ENERGY = "energy";

    public QuantumArmorItem(Type type, Settings settings) {
        super(QuantumArmorMaterial.INSTANCE, type, settings.maxCount(1));
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
        return getEnergy(stack) < CAPACITY_EU;
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

    protected static long drainIgnoreLimit(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }

    protected static boolean canUse(ItemStack stack, long amount) {
        return drainIgnoreLimit(stack, amount, true) >= amount;
    }


    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!isEquippedInCorrectSlot(living, stack)) return;
        RadiationUtil.clearIfProtected(living);
    }

    protected boolean isEquippedInCorrectSlot(LivingEntity living, ItemStack stack) {
        return switch (this.getType()) {
            case HELMET -> living.getEquippedStack(EquipmentSlot.HEAD) == stack;
            case CHESTPLATE -> living.getEquippedStack(EquipmentSlot.CHEST) == stack;
            case LEGGINGS -> living.getEquippedStack(EquipmentSlot.LEGS) == stack;
            case BOOTS -> living.getEquippedStack(EquipmentSlot.FEET) == stack;
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, 3)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", TIER).formatted(Formatting.DARK_GRAY));
    }
}
