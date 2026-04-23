package com.shipovskijkorp.industriallegacy.reactor.api;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * IC2-like reactor core interface used by reactor components.
 */
public interface IReactor {
    @Nullable ItemStack getItemAt(int x, int y);

    void setItemAt(int x, int y, @Nullable ItemStack stack);

    int getHeat();

    void setHeat(int heat);

    int addHeat(int heat);

    int getMaxHeat();

    void setMaxHeat(int heat);

    int addEmitHeat(int heat);

    float getHeatEffectModifier();

    void setHeatEffectModifier(float modifier);

    float getReactorEnergyOutput();

    float addOutput(float amount);

    void explode();

    boolean produceEnergy();

    boolean isFluidCooled();
}
