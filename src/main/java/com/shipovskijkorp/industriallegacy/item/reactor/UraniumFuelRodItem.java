package com.shipovskijkorp.industriallegacy.item.reactor;

import com.shipovskijkorp.industriallegacy.reactor.api.IReactor;
import com.shipovskijkorp.industriallegacy.reactor.api.IReactorComponent;
import com.shipovskijkorp.industriallegacy.util.RadiationUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class UraniumFuelRodItem extends Item implements IReactorComponent {
    private static final String NBT_DAMAGE = "il_reactor_rod_damage";

    protected final int numberOfCells;
    protected final int duration;
    @Nullable
    protected final Item depletedItem;

    public UraniumFuelRodItem(Settings settings, int numberOfCells, int duration, @Nullable Item depletedItem) {
        super(settings.maxCount(64));
        this.numberOfCells = numberOfCells;
        this.duration = duration;
        this.depletedItem = depletedItem;
    }

    protected int getDamage(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(NBT_DAMAGE);
    }

    protected void setDamage(ItemStack stack, int value) {
        value = Math.max(0, Math.min(duration, value));
        if (value <= 0) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_DAMAGE);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
        } else {
            stack.getOrCreateNbt().putInt(NBT_DAMAGE, value);
        }
    }

    protected float getPulseEnergy(ItemStack stack, IReactor reactor, int x, int y) {
        return 1.0f;
    }

    protected int getFinalHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
        return heat;
    }

    @Nullable
    protected ItemStack getDepletedStack(ItemStack stack, IReactor reactor) {
        return depletedItem == null ? ItemStack.EMPTY : new ItemStack(depletedItem);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.max(0, 13 - Math.round(13.0f * getDamage(stack) / (float) duration));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x75d63a;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("industrial_legacy.reactoritem.durability", duration - getDamage(stack), duration)
                .formatted(Formatting.GRAY));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (entity instanceof LivingEntity living) {
            RadiationUtil.apply(living, 200, 100);
        }
    }

    @Override
    public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {
        if (!reactor.produceEnergy()) return;

        int basePulses = 1 + numberOfCells / 2;

        for (int iteration = 0; iteration < numberOfCells; iteration++) {
            int pulses = basePulses;

            if (!heatRun) {
                for (int i = 0; i < pulses; i++) {
                    acceptUraniumPulse(stack, reactor, stack, x, y, x, y, false);
                }

                pulses += checkPulseable(reactor, x - 1, y, stack, x, y, false);
                pulses += checkPulseable(reactor, x + 1, y, stack, x, y, false);
                pulses += checkPulseable(reactor, x, y - 1, stack, x, y, false);
                pulses += checkPulseable(reactor, x, y + 1, stack, x, y, false);
            } else {
                pulses += checkPulseable(reactor, x - 1, y, stack, x, y, true);
                pulses += checkPulseable(reactor, x + 1, y, stack, x, y, true);
                pulses += checkPulseable(reactor, x, y - 1, stack, x, y, true);
                pulses += checkPulseable(reactor, x, y + 1, stack, x, y, true);

                int heat = triangularNumber(pulses) * 4;
                heat = getFinalHeat(stack, reactor, x, y, heat);
                Queue<ItemStackCoord> acceptors = new ArrayDeque<>();
                checkHeatAcceptor(reactor, x - 1, y, acceptors);
                checkHeatAcceptor(reactor, x + 1, y, acceptors);
                checkHeatAcceptor(reactor, x, y - 1, acceptors);
                checkHeatAcceptor(reactor, x, y + 1, acceptors);

                while (!acceptors.isEmpty() && heat > 0) {
                    int dHeat = heat / acceptors.size();
                    heat -= dHeat;
                    ItemStackCoord acceptor = acceptors.remove();
                    IReactorComponent comp = (IReactorComponent) acceptor.stack.getItem();
                    dHeat = comp.alterHeat(acceptor.stack, reactor, acceptor.x, acceptor.y, dHeat);
                    heat += dHeat;
                }

                if (heat > 0) {
                    reactor.addHeat(heat);
                }
            }
        }

        if (!heatRun) {
            if (getDamage(stack) >= duration - 1) {
                ItemStack depleted = getDepletedStack(stack, reactor);
                reactor.setItemAt(x, y, depleted == null || depleted.isEmpty() ? ItemStack.EMPTY : depleted);
            } else {
                setDamage(stack, getDamage(stack) + 1);
            }
        }
    }

    @Override
    public boolean acceptUraniumPulse(ItemStack stack, IReactor reactor, ItemStack pulsingStack,
                                      int youX, int youY, int pulseX, int pulseY, boolean heatRun) {
        if (!heatRun) {
            reactor.addOutput(getPulseEnergy(stack, reactor, youX, youY));
        }
        return true;
    }

    @Override
    public float influenceExplosion(ItemStack stack, IReactor reactor) {
        return 2 * numberOfCells;
    }

    private static int checkPulseable(IReactor reactor, int x, int y, ItemStack source, int myX, int myY, boolean heatRun) {
        ItemStack other = reactor.getItemAt(x, y);
        if (other != null && !other.isEmpty() && other.getItem() instanceof IReactorComponent comp
                && comp.acceptUraniumPulse(other, reactor, source, x, y, myX, myY, heatRun)) {
            return 1;
        }
        return 0;
    }

    private static int triangularNumber(int x) {
        return (x * x + x) / 2;
    }

    private static void checkHeatAcceptor(IReactor reactor, int x, int y, Queue<ItemStackCoord> out) {
        ItemStack stack = reactor.getItemAt(x, y);
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IReactorComponent comp && comp.canStoreHeat(stack, reactor, x, y)) {
            out.add(new ItemStackCoord(stack, x, y));
        }
    }

    private static final class ItemStackCoord {
        final ItemStack stack;
        final int x;
        final int y;

        ItemStackCoord(ItemStack stack, int x, int y) {
            this.stack = stack;
            this.x = x;
            this.y = y;
        }
    }
}
