package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * IC2 Experimental MOX rod semantics for the electric reactor path used in IL.
 *
 * In classic electric operation, MOX heat generation matches uranium rods.
 * Their special behavior is higher EU output based on current reactor hull heat.
 */
public class MoxFuelRodItem extends UraniumFuelRodItem {
    public MoxFuelRodItem(Settings settings, int numberOfCells, int duration, @Nullable Item depletedItem) {
        super(settings, numberOfCells, duration, depletedItem);
    }

    @Override
    protected float getPulseEnergy(ItemStack stack, IReactor reactor, int x, int y) {
        float breederEffectiveness = reactor.getHeat() / (float) Math.max(1, reactor.getMaxHeat());
        return 4.0f * breederEffectiveness + 1.0f;
    }
}
