package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.item.ItemStack;

public class ReactorVentItem extends ReactorHeatStorageItem {
    public final int selfVent;
    public final int reactorVent;

    public ReactorVentItem(Settings settings, int heatStorage, int selfVent, int reactorVent) {
        super(settings, heatStorage);
        this.selfVent = selfVent;
        this.reactorVent = reactorVent;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;

        if (reactorVent > 0) {
            int reactorDrain = Math.min(reactor.getHeat(), reactorVent);
            int remaining = alterHeat(stack, reactor, x, y, reactorDrain);
            if (remaining > 0) {
                return;
            }
            reactor.setHeat(reactor.getHeat() - reactorDrain);
        }

        int vented = alterHeat(stack, reactor, x, y, -selfVent);
        if (vented <= 0) {
            reactor.addEmitHeat(vented + selfVent);
        }
    }
}
