package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * IC2 ItemReactorHeatSwitch semantics.
 *
 * This component is a heat-storage component first, with extra redistribution
 * logic layered on top of the normal heat storage behaviour.
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

        int myHeat = 0;
        List<ItemStackCoord> heatAcceptors = new ArrayList<>();

        if (switchSide > 0) {
            checkHeatAcceptor(reactor, x - 1, y, heatAcceptors);
            checkHeatAcceptor(reactor, x + 1, y, heatAcceptors);
            checkHeatAcceptor(reactor, x, y - 1, heatAcceptors);
            checkHeatAcceptor(reactor, x, y + 1, heatAcceptors);
        }

        if (switchSide > 0) {
            for (ItemStackCoord stackCoord : heatAcceptors) {
                IReactorComponent heatable = (IReactorComponent) stackCoord.stack.getItem();

                double mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
                double heatablemed = heatable.getCurrentHeat(stackCoord.stack, reactor, stackCoord.x, stackCoord.y)
                        * 100.0 / heatable.getMaxHeat(stackCoord.stack, reactor, stackCoord.x, stackCoord.y);

                int add = (int) (heatable.getMaxHeat(stackCoord.stack, reactor, stackCoord.x, stackCoord.y)
                        / 100.0 * (heatablemed + mymed / 2.0));

                if (add > switchSide) add = switchSide;
                if (heatablemed + mymed / 2.0 < 1.0) add = switchSide / 2;
                if (heatablemed + mymed / 2.0 < 0.75) add = switchSide / 4;
                if (heatablemed + mymed / 2.0 < 0.5) add = switchSide / 8;
                if (heatablemed + mymed / 2.0 < 0.25) add = 1;

                double roundedHeatable = Math.round(heatablemed * 10.0) / 10.0;
                double roundedMine = Math.round(mymed * 10.0) / 10.0;

                if (roundedHeatable > roundedMine) {
                    add -= 2 * add;
                } else if (roundedHeatable == roundedMine) {
                    add = 0;
                }

                myHeat -= add;
                add = heatable.alterHeat(stackCoord.stack, reactor, stackCoord.x, stackCoord.y, add);
                myHeat += add;
            }
        }

        if (switchReactor > 0) {
            double mymed = getCurrentHeat(stack, reactor, x, y) * 100.0 / getMaxHeat(stack, reactor, x, y);
            double reactorMed = reactor.getHeat() * 100.0 / reactor.getMaxHeat();

            int add = (int) Math.round(reactor.getMaxHeat() / 100.0 * (reactorMed + mymed / 2.0));

            if (add > switchReactor) add = switchReactor;
            if (reactorMed + mymed / 2.0 < 1.0) add = switchSide / 2;
            if (reactorMed + mymed / 2.0 < 0.75) add = switchSide / 4;
            if (reactorMed + mymed / 2.0 < 0.5) add = switchSide / 8;
            if (reactorMed + mymed / 2.0 < 0.25) add = 1;

            double roundedReactor = Math.round(reactorMed * 10.0) / 10.0;
            double roundedMine = Math.round(mymed * 10.0) / 10.0;

            if (roundedReactor > roundedMine) {
                add -= 2 * add;
            } else if (roundedReactor == roundedMine) {
                add = 0;
            }

            myHeat -= add;
            reactor.setHeat(reactor.getHeat() + add);
        }

        alterHeat(stack, reactor, x, y, myHeat);
    }

    private void checkHeatAcceptor(IReactor reactor, int x, int y, List<ItemStackCoord> heatAcceptors) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp && comp.canStoreHeat(stack, reactor, x, y)) {
            heatAcceptors.add(new ItemStackCoord(stack, x, y));
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
