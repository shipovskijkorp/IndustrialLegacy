package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public abstract class AbstractReactorComponentItem extends Item implements IReactorComponent {
    protected AbstractReactorComponentItem(Settings settings) {
        super(settings);
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
    }
}
