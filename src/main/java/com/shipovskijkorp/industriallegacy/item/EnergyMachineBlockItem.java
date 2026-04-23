package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Block item with IC2-style power-tier tooltip and optional stored-energy tooltip.
 */
public class EnergyMachineBlockItem extends BlockItem {
    public static final String NBT_ENERGY = "energy";

    private final int powerTier;
    private final long chargeCapacity;
    private final boolean chargeable;

    public EnergyMachineBlockItem(Block block, Settings settings, int powerTier) {
        this(block, settings, powerTier, 0L, false);
    }

    public EnergyMachineBlockItem(Block block, Settings settings, int powerTier, long chargeCapacity, boolean chargeable) {
        super(block, settings);
        this.powerTier = powerTier;
        this.chargeCapacity = Math.max(0L, chargeCapacity);
        this.chargeable = chargeable && chargeCapacity > 0L;
    }

    public boolean isChargeable() {
        return chargeable;
    }

    public long getChargeCapacity() {
        return chargeCapacity;
    }

    public long getStoredEnergy(ItemStack stack) {
        if (!chargeable || stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long energy = nbt.getLong(NBT_ENERGY);
        return Math.max(0L, Math.min(chargeCapacity, energy));
    }

    public void setStoredEnergy(ItemStack stack, long energy) {
        if (!chargeable || stack == null || stack.isEmpty()) return;
        long clamped = Math.max(0L, Math.min(chargeCapacity, energy));
        if (clamped == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }

        stack.getOrCreateNbt().putLong(NBT_ENERGY, clamped);
    }

    public ItemStack createChargedStack() {
        ItemStack stack = new ItemStack(this);
        setStoredEnergy(stack, chargeCapacity);
        return stack;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (!chargeable || chargeCapacity <= 0L) return 0;
        double ratio = (double) getStoredEnergy(stack) / (double) chargeCapacity;
        return (int) Math.round(ratio * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", powerTier).formatted(Formatting.DARK_GRAY));
        if (chargeable) {
            tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getStoredEnergy(stack), chargeCapacity, 4)).formatted(Formatting.GRAY));
        }
    }
}