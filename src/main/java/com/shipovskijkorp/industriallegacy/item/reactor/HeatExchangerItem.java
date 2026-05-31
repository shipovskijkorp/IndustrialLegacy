package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * IL ItemReactorHeatSwitch port (1.12.2 Experimental semantics).
 *
 * Params:
 * - maxHeat: internal heat storage
 * - switchSide: heat moved between this item and adjacent components per tick
 * - switchReactor: heat moved between this item and reactor hull per tick
 */
public class HeatExchangerItem extends ReactorHeatStorageItem {

    public final int switchSide;
    public final int switchReactor;

    public HeatExchangerItem(Settings settings, int maxHeat, int switchSide, int switchReactor) {
        super(settings, maxHeat);
        this.switchSide = switchSide;
        this.switchReactor = switchReactor;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;

        int myHeatDelta = 0;

        List<ItemStackCoord> heatAcceptors = new ArrayList<>();
        if (switchSide > 0) {
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors);
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors);
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors);
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors);
        }

        if (switchSide > 0) {
            for (ItemStackCoord sc : heatAcceptors) {
                IReactorComponent heatable = (IReactorComponent) sc.stack.getItem();

                double mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
                double heatablemed = heatable.getCurrentHeat(sc.stack, reactor, sc.x, sc.y) * 100.0 / heatable.getMaxHeat(sc.stack, reactor, sc.x, sc.y);

                int add = (int) (heatable.getMaxHeat(sc.stack, reactor, sc.x, sc.y) / 100.0 * (heatablemed + mymed / 2.0));

                if (add > switchSide) add = switchSide;
                if (heatablemed + mymed / 2.0 < 1.0) add = switchSide / 2;
                if (heatablemed + mymed / 2.0 < 0.75) add = switchSide / 4;
                if (heatablemed + mymed / 2.0 < 0.5) add = switchSide / 8;
                if (heatablemed + mymed / 2.0 < 0.25) add = 1;

                double heatableMedRounded = Math.round(heatablemed * 10.0) / 10.0;
                double myMedRounded = Math.round(mymed * 10.0) / 10.0;

                if (heatableMedRounded > myMedRounded) {
                    add = -add;
                } else if (heatableMedRounded == myMedRounded) {
                    add = 0;
                }

                myHeatDelta -= add;
                add = heatable.alterHeat(sc.stack, reactor, sc.x, sc.y, add);
                myHeatDelta += add;
            }
        }

        if (switchReactor > 0) {
            double mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
            double reactormed = reactor.getHeat() * 100.0 / reactor.getMaxHeat();

            int add = (int) Math.round(reactor.getMaxHeat() / 100.0 * (reactormed + mymed / 2.0));

            if (add > switchReactor) add = switchReactor;
            if (reactormed + mymed / 2.0 < 1.0) add = switchSide / 2;
            if (reactormed + mymed / 2.0 < 0.75) add = switchSide / 4;
            if (reactormed + mymed / 2.0 < 0.5) add = switchSide / 8;
            if (reactormed + mymed / 2.0 < 0.25) add = 1;

            double reactorMedRounded = Math.round(reactormed * 10.0) / 10.0;
            double myMedRounded = Math.round(mymed * 10.0) / 10.0;

            if (reactorMedRounded > myMedRounded) {
                add = -add;
            } else if (reactorMedRounded == myMedRounded) {
                add = 0;
            }

            myHeatDelta -= add;
            reactor.setHeat(reactor.getHeat() + add);
        }

        alterHeat(stack, reactor, x, y, myHeatDelta);
    }

    private void checkHeatAcceptor(IReactor reactor, int x, int y, List<ItemStackCoord> heatAcceptors) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp) {
            if (comp.canStoreHeat(stack, reactor, x, y)) {
                heatAcceptors.add(new ItemStackCoord(stack, x, y));
            }
        }
    }

    private static final class ItemStackCoord {
        final ItemStack stack;
        final int x;
        final int y;

        ItemStackCoord(ItemStack stack, int x, int y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }
}
