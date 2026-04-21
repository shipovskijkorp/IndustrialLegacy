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
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        if (!heatRun) {
            float heatRatio = reactor.getHeat() / (float) Math.max(1, reactor.getMaxHeat());
            reactor.addOutput(1.0f + 4.0f * heatRatio);
        }
        return true;
    }
}
