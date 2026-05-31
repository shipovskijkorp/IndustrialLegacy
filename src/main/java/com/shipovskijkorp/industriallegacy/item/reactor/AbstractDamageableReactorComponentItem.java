package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IL-like reactor component which stores its internal heat as custom NBT damage.
 *
 * This mirrors IL's AbstractDamageableReactorComponent / ItemGradualInt semantics:
 * - durability bar is always visible
 * - more stored heat means less remaining durability in the bar
 * - tooltip always shows remaining durability
 */
public abstract class AbstractDamageableReactorComponentItem extends Item implements IReactorComponent {
    private static final String NBT_HEAT = "il_reactor_heat";
    private final int maxHeat;

    protected AbstractDamageableReactorComponentItem(Settings settings, int maxHeat) {
        super(settings);
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
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        float damageFraction = getCustomDamage(stack) / (float) maxHeat;
        return Math.max(0, 13 - Math.round(damageFraction * 13.0f));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float damageFraction = getCustomDamage(stack) / (float) maxHeat;
        return MathHelper.hsvToRgb(Math.max(0.0f, (1.0f - damageFraction) / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable(
                "industrial_legacy.reactoritem.durability",
                getMaxCustomDamage(stack) - getCustomDamage(stack),
                getMaxCustomDamage(stack)
        ).formatted(Formatting.GRAY));
    }

    @Override
    public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return false;
    }

    @Override
    public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    @Override
    public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
        return 0;
    }

    @Override
    public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        return heat;
    }
}
