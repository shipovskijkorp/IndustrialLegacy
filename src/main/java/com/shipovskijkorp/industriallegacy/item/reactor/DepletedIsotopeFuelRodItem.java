package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DepletedIsotopeFuelRodItem extends Item implements IReactorComponent {
    private static final String NBT_PROGRESS = "il_reactor_progress";
    private final int duration;

    public DepletedIsotopeFuelRodItem(Settings settings, int duration) {
        super(settings.maxCount(64));
        this.duration = Math.max(1, duration);
    }

    private int getProgress(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(NBT_PROGRESS);
    }

    private void setProgress(ItemStack stack, int value) {
        value = Math.max(0, Math.min(duration, value));
        if (value == 0) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_PROGRESS);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
        } else {
            stack.getOrCreateNbt().putInt(NBT_PROGRESS, value);
        }
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0f * getProgress(stack) / (float) duration);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x75d63a;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("industrial_legacy.reactoritem.durability", duration - getProgress(stack), duration)
                .formatted(Formatting.GRAY));
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        if (heatRun) {
            int newProgress = getProgress(stack) + 1 + reactor.getHeat() / 3000;
            if (newProgress >= duration) {
                reactor.setItemAt(youX, youY, new ItemStack(ModItems.RE_ENRICHED_URANIUM));
            } else {
                setProgress(stack, newProgress);
            }
        }
        return true;
    }
}
