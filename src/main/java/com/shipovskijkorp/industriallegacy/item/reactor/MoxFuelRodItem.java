package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MoxFuelRodItem extends UraniumFuelRodItem {
    public MoxFuelRodItem(Settings settings, int numberOfCells, int duration, @Nullable Item depletedItem) {
        super(settings, numberOfCells, duration, depletedItem);
    }

    @Override
    protected int getFinalHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        if (reactor.isFluidCooled()) {
            float breederEffectiveness = reactor.getHeat() / (float) Math.max(1, reactor.getMaxHeat());
            if (breederEffectiveness > 0.5f) {
                heat *= 2;
            }
        }
        return heat;
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        if (!heatRun) {
            float breederEffectiveness = reactor.getHeat() / (float) Math.max(1, reactor.getMaxHeat());
            reactor.addOutput(4.0f * breederEffectiveness + 1.0f);
        }
        return true;
    }
}
