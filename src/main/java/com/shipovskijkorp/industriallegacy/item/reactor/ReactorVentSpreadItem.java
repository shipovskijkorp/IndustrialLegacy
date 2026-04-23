package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.item.ItemStack;

public class ReactorVentSpreadItem extends AbstractReactorComponentItem {
    public final int sideVent;

    public ReactorVentSpreadItem(Settings settings, int sideVent) {
        super(settings);
        this.sideVent = sideVent;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;

        this.cool(reactor, x - 1, y);
        this.cool(reactor, x + 1, y);
        this.cool(reactor, x, y - 1);
        this.cool(reactor, x, y + 1);
    }

    private void cool(IReactor reactor, int x, int y) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp) {
            if (comp.canStoreHeat(stack, reactor, x, y)) {
                int self = comp.alterHeat(stack, reactor, x, y, -this.sideVent);
                if (self <= 0) {
                    reactor.addEmitHeat(self + this.sideVent);
                }
            }
        }
    }
}
