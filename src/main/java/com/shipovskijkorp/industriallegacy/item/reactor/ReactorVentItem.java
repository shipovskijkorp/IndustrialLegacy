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

        if (this.reactorVent > 0) {
            int reactorDrain = reactor.getHeat();
            int reactorHeat = reactorDrain;

            if (reactorDrain > this.reactorVent) {
                reactorDrain = this.reactorVent;
            }

            reactorHeat -= reactorDrain;
            if ((reactorDrain = this.alterHeat(stack, reactor, x, y, reactorDrain)) > 0) {
                return;
            }

            reactor.setHeat(reactorHeat);
        }

        int self = this.alterHeat(stack, reactor, x, y, -this.selfVent);
        if (self <= 0) {
            reactor.addEmitHeat(self + this.selfVent);
        }
    }
}
