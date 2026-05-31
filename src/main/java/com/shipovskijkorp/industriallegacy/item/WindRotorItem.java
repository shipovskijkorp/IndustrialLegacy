package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IL Experimental wind kinetic rotor. */
public final class WindRotorItem extends Item {
    private final int diameter;
    private final float efficiency;
    private final int minWindStrength;
    private final int maxWindStrength;
    private final Identifier rotorModelTexture;

    public WindRotorItem(Settings settings, int diameter, int durability, float efficiency,
                         int minWindStrength, int maxWindStrength, String texturePath) {
        super(settings.maxCount(1).maxDamage(durability));
        this.diameter = diameter;
        this.efficiency = efficiency;
        this.minWindStrength = minWindStrength;
        this.maxWindStrength = maxWindStrength;
        this.rotorModelTexture = new Identifier(IndustrialLegacy.MOD_ID, texturePath);
    }

    public int getDiameter(ItemStack stack) {
        return diameter;
    }

    public float getEfficiency(ItemStack stack) {
        return efficiency;
    }

    public int getMinWindStrength(ItemStack stack) {
        return minWindStrength;
    }

    public int getMaxWindStrength(ItemStack stack) {
        return maxWindStrength;
    }

    public Identifier getRotorModelTexture(ItemStack stack) {
        return rotorModelTexture;
    }

    public int getHealthPercent(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getMaxDamage() <= 0) return 0;
        return Math.max(0, Math.round(100.0f - (float) stack.getDamage() * 100.0f / (float) stack.getMaxDamage()));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.industrial_legacy.wind_rotor.wind", minWindStrength, maxWindStrength).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.wind_rotor.efficiency", Math.round(efficiency * 100.0f)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.wind_rotor.diameter", diameter).formatted(Formatting.GRAY));
    }
}
