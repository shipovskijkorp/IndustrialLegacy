package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.item.ItemStack;

public class ReactorReflectorItem extends AbstractDamageableReactorComponentItem {
    public ReactorReflectorItem(Settings settings, int maxDamage) {
        super(settings, maxDamage);
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        if (!heatRun) {
            if (pulsingStack.getItem() instanceof IReactorComponent source) {
                source.acceptUraniumPulse(pulsingStack, reactor, stack, pulseX, pulseY, youX, youY, false);
            }
        } else if (getCustomDamage(stack) + 1 >= getMaxCustomDamage(stack)) {
            reactor.setItemAt(youX, youY, null);
        } else {
            setCustomDamage(stack, getCustomDamage(stack) + 1);
        }
        return true;
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return -1.0f;
    }
}
