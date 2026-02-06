package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2-like reactor component which stores its internal heat as "custom damage" in NBT.
 * In IC2 this is implemented via AbstractDamageableReactorComponent.
 *
 * We keep semantics:
 * - heat stored in range [0..maxHeat]
 * - if heat exceeds maxHeat, the item is destroyed in reactor slot
 */
public abstract class AbstractDamageableReactorComponentItem extends Item implements IReactorComponent {
    private static final String NBT_HEAT = "il_reactor_heat";
    private final int maxHeat;

    protected AbstractDamageableReactorComponentItem(Settings settings, int maxHeat) {
        super(settings.maxCount(1));
        this.maxHeat = maxHeat;
    }

    protected int getMaxCustomDamage(ItemStack stack) {
        return maxHeat;
    }

    protected int getCustomDamage(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return 0;
        return nbt.getInt(NBT_HEAT);
    }

    protected void setCustomDamage(ItemStack stack, int heat) {
        heat = Math.max(0, Math.min(maxHeat, heat));
        if (heat == 0) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_HEAT);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
        } else {
            stack.getOrCreateNbt().putInt(NBT_HEAT, heat);
        }
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getCustomDamage(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        double r = (double) getCustomDamage(stack) / (double) maxHeat;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0xFF5555;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        int heat = getCustomDamage(stack);
        if (heat > 0) {
            tooltip.add(Text.literal("Heat: " + heat + "/" + maxHeat).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("industrial_legacy.reactoritem.heatwarning.line1").formatted(Formatting.RED));
            tooltip.add(Text.translatable("industrial_legacy.reactoritem.heatwarning.line2").formatted(Formatting.RED));
        }
    }

    // default impls for heat storage components
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
        int myHeat = getCurrentHeat(stack, reactor, x, y) + heat;
        int max = getMaxHeat(stack, reactor, x, y);

        if (myHeat > max) {
            // destroy the item in the reactor slot, return leftover heat like IC2
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
}
