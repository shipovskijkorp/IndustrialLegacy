package com.shipovskijkorp.industriallegacy.item;

import net.minecraft.item.Item;

/** Basic IC2-style wrench. Rotation logic intentionally not implemented yet. */
public class WrenchItem extends Item {
    public WrenchItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(120));
    }
}
