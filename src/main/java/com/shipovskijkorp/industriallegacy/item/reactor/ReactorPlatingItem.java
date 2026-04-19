package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.item.ItemStack;

public class ReactorPlatingItem extends AbstractReactorComponentItem {
    private final int maxHeatAdd;
    private final float effectModifier;

    public ReactorPlatingItem(Settings settings, int maxHeatAdd, float effectModifier) {
        super(settings);
        this.maxHeatAdd = maxHeatAdd;
        this.effectModifier = effectModifier;
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!heatRun) return;
        reactor.setMaxHeat(reactor.getMaxHeat() + maxHeatAdd);
        reactor.setHeatEffectModifier(reactor.getHeatEffectModifier() * effectModifier);
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return effectModifier >= 1.0f ? 0.0f : effectModifier;
    }
}
