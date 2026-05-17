package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2-style electric backpack base.
 *
 * The original IC2 batpack family is chest armor with zero armor protection that can
 * provide EU to carried or worn electric items:
 * - BatPack: 60,000 EU, 100 EU/t, tier 1
 * - Advanced BatPack: 600,000 EU, 1,000 EU/t, tier 2
 * - Energypack: 2,000,000 EU, 1,000 EU/t, tier 3
 */
public class BatteryPackItem extends ArmorItem implements IElectricItem {
    private static final String NBT_ENERGY = "energy";

    private final long capacityEu;
    private final long transferLimitEuT;
    private final int tier;

    public BatteryPackItem(ArmorMaterial material, Settings settings, long capacityEu, long transferLimitEuT, int tier) {
        super(material, Type.CHESTPLATE, settings.maxCount(1));
        this.capacityEu = capacityEu;
        this.transferLimitEuT = transferLimitEuT;
        this.tier = tier;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.CHEST) != stack) return;
        if (getEnergy(stack) <= 0L) return;

        long remaining = Math.min(getTransferLimit(stack), getEnergy(stack));

        for (int i = 0; i < player.getInventory().main.size() && remaining > 0L && getEnergy(stack) > 0L; i++) {
            remaining -= transferTo(stack, player.getInventory().main.get(i), remaining);
        }

        for (EquipmentSlot armorSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (remaining <= 0L || getEnergy(stack) <= 0L) break;
            remaining -= transferTo(stack, player.getEquippedStack(armorSlot), remaining);
        }

        if (remaining < getTransferLimit(stack)) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    private long transferTo(ItemStack source, ItemStack target, long maxAmount) {
        if (target.isEmpty() || target == source || !ElectricItemManager.isElectric(target)) return 0L;
        if (ElectricItemManager.getTier(target) > tier) return 0L;

        long amount = Math.min(maxAmount, Math.min(getTransferLimit(source), getEnergy(source)));
        long accepted = ElectricItemManager.charge(target, amount, true);
        if (accepted <= 0L) return 0L;

        long extracted = Math.min(accepted, getEnergy(source));
        setEnergy(source, getEnergy(source) - extracted);
        long charged = ElectricItemManager.charge(target, extracted, false);
        if (charged < extracted) {
            setEnergy(source, getEnergy(source) + (extracted - charged));
        }
        return charged;
    }

    @Override
    public long getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long energy = nbt.getLong(NBT_ENERGY);
        if (energy < 0L) energy = 0L;
        if (energy > capacityEu) energy = capacityEu;
        return energy;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        if (stack == null || stack.isEmpty()) return;
        long clamped = Math.max(0L, Math.min(capacityEu, energy));
        if (clamped == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, clamped);
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
        return Math.round((float) getEnergy(stack) * 13.0f / (float) capacityEu);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), capacityEu, 3)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", tier).formatted(Formatting.DARK_GRAY));
    }
}
