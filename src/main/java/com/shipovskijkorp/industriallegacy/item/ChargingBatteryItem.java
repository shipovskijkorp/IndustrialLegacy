package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.tool.IModeSwitchableItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * IC2 ItemBatteryChargeHotbar equivalent.
 *
 * <p>Stores EU like the other IL electric items and, while enabled, charges the
 * player's hotbar every 10 ticks. Right click or the common mode-switch key cycles
 * Enabled -> Disabled -> Not in hand.</p>
 */
public final class ChargingBatteryItem extends Item implements IElectricItem, IModeSwitchableItem {
    private static final String NBT_ENERGY = "energy";
    private static final String NBT_MODE = "mode";

    private final long capacityEu;
    private final long transferLimitEuT;
    private final int tier;

    public ChargingBatteryItem(Settings settings, long capacityEu, long transferLimitEuT, int tier) {
        super(settings);
        this.capacityEu = capacityEu;
        this.transferLimitEuT = transferLimitEuT;
        this.tier = tier;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        Mode mode = getMode(stack);
        if (!mode.enabled || world.getTime() % 10L >= tier || getEnergy(stack) <= 0L) {
            return;
        }

        long remaining = getTransferLimit(stack);
        for (int i = 0; i < 9 && remaining > 0L && getEnergy(stack) > 0L; i++) {
            if (mode == Mode.NOT_IN_HAND && i == player.getInventory().selectedSlot) {
                continue;
            }

            ItemStack target = player.getInventory().main.get(i);
            if (target.isEmpty() || target == stack || target.getItem() instanceof ChargingBatteryItem) {
                continue;
            }
            if (!ElectricItemManager.isElectric(target)) {
                continue;
            }

            long accepted = ElectricItemManager.charge(target, remaining, true);
            if (accepted <= 0L) {
                continue;
            }

            long extracted = ElectricItemManager.discharge(stack, accepted, false);
            if (extracted <= 0L) {
                continue;
            }

            long charged = ElectricItemManager.charge(target, extracted, false);
            if (charged < extracted) {
                ElectricItemManager.charge(stack, extracted - charged, false);
            }

            remaining -= charged;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            cycleMode(stack, serverPlayer);
            user.sendMessage(Text.translatable("message.industrial_legacy.mode", getModeName(stack)), true);
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        Mode next = getMode(stack).next();
        setMode(stack, next);
        return next.ordinal();
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable("tooltip.industrial_legacy.mode." + getMode(stack).name().toLowerCase(Locale.ROOT));
    }

    public Mode getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_MODE)) {
            return Mode.ENABLED;
        }
        return Mode.byId(nbt.getByte(NBT_MODE));
    }

    private void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateNbt().putByte(NBT_MODE, (byte) mode.ordinal());
    }

    @Override
    public long getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > capacityEu) e = capacityEu;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        if (stack == null || stack.isEmpty()) return;
        long e = Math.max(0L, Math.min(capacityEu, energy));

        NbtCompound nbt = stack.getNbt();
        if (e == 0L) {
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }

        stack.getOrCreateNbt().putLong(NBT_ENERGY, e);
    }

    @Override
    public long getCapacity(ItemStack stack) {
        return capacityEu;
    }

    @Override
    public long getTransferLimit(ItemStack stack) {
        return transferLimitEuT;
    }

    @Override
    public int getTier(ItemStack stack) {
        return tier;
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < capacityEu;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        double r = (double) getEnergy(stack) / (double) capacityEu;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), capacityEu, tier)).formatted(Formatting.GRAY));
        tooltip.add(getModeName(stack).copy().formatted(Formatting.GRAY));
    }

    public enum Mode {
        ENABLED(true),
        DISABLED(false),
        NOT_IN_HAND(true);

        private final boolean enabled;

        Mode(boolean enabled) {
            this.enabled = enabled;
        }

        Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        static Mode byId(int id) {
            Mode[] values = values();
            if (id < 0 || id >= values.length) {
                return ENABLED;
            }
            return values[id];
        }
    }
}
