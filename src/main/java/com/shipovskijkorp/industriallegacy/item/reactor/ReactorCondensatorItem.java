package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.item.ItemStack;

/**
 * IL RSH/LZH condensator semantics.
 * Stores positive heat up to capacity, never self-destructs from overflow,
 * and returns leftover heat once full. Negative heat is ignored here and must
 * be removed via recharge recipes, matching IL behaviour.
 */
public class ReactorCondensatorItem extends AbstractDamageableReactorComponentItem {
    public ReactorCondensatorItem(Settings settings, int maxHeat) {
        super(settings, maxHeat);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getCurrentHeat(stack, reactor, x, y) < getMaxHeat(stack, reactor, x, y);
    }

    @Override
    public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getMaxCustomDamage(stack);
    }

    @Override
    public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return getCustomDamage(stack);
    }

    @Override
    public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        if (heat < 0) return heat;

        int current = getCurrentHeat(stack, reactor, x, y);
        int max = getMaxHeat(stack, reactor, x, y);
        int accepted = Math.min(heat, max - current);
        if (accepted > 0) {
            setCustomDamage(stack, current + accepted);
        }
        return heat - accepted;
    }
}
