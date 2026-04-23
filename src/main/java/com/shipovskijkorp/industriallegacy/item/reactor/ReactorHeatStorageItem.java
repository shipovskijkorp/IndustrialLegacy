package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReactorHeatStorageItem extends AbstractDamageableReactorComponentItem {
    public ReactorHeatStorageItem(Settings settings, int maxHeat) {
        super(settings, maxHeat);
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return true;
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
        int myHeat = getCurrentHeat(stack, reactor, x, y);
        myHeat += heat;
        int max = getMaxHeat(stack, reactor, x, y);

        if (myHeat > max) {
            reactor.setItemAt(x, y, null);
            return max - myHeat + 1;
        }

        if (myHeat < 0) {
            int leftover = myHeat;
            myHeat = 0;
            setCustomDamage(stack, myHeat);
            return leftover;
        }

        setCustomDamage(stack, myHeat);
        return 0;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        if (getCustomDamage(stack) > 0) {
            tooltip.add(Text.translatable("industrial_legacy.reactoritem.heatwarning.line1").formatted(Formatting.RED));
            tooltip.add(Text.translatable("industrial_legacy.reactoritem.heatwarning.line2").formatted(Formatting.RED));
        }
    }
}
