package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.util.RadiationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;


/**
 * Simple IC2-like radioactive inventory item.
 *
 * Radiation is applied while carried by a living entity unless the entity wears
 * a full hazmat-equivalent suit.
 */
public class RadioactiveItem extends Item {
    private final int radiationDurationTicks;
    private final int radiationAmplifier;

    public RadioactiveItem(Settings settings, int radiationDurationTicks, int radiationAmplifier) {
        super(settings);
        this.radiationDurationTicks = Math.max(1, radiationDurationTicks);
        this.radiationAmplifier = Math.max(0, radiationAmplifier);
    }

    public int getRadiationDurationTicks() {
        return radiationDurationTicks;
    }

    public int getRadiationAmplifier() {
        return radiationAmplifier;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (!(entity instanceof LivingEntity living)) return;
        RadiationUtil.apply(living, radiationDurationTicks, radiationAmplifier);
    }

}
