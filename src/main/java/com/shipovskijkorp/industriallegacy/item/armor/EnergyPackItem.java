package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
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
 * IC2 Experimental Energypack.
 *
 * Source values: ItemArmorEnergypack(maxCharge=2,000,000 EU, transfer=1,000 EU/t, tier=3).
 * Forge IC2 exposes this as an external armor energy provider; IL mirrors that by feeding
 * carried/worn electric items from the equipped chest slot.
 */
public final class EnergyPackItem extends ArmorItem implements IElectricItem {
    public static final long CAPACITY_EU = 2_000_000L;
    public static final long TRANSFER_LIMIT_EU_T = 1_000L;
    public static final int TIER = 3;

    private static final String NBT_ENERGY = "energy";

    public EnergyPackItem(Settings settings) {
        super(ModArmorMaterials.ENERGYPACK, Type.CHESTPLATE, settings.maxCount(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.CHEST) != stack) return;
        if (getEnergy(stack) <= 0L) return;

        long remaining = Math.min(getTransferLimit(stack), getEnergy(stack));

        // Hotbar first, then main inventory, then the other armor pieces. This makes tools feel like IC2,
        // while still allowing the solar helmet/static boots to feed chest storage and this pack to feed back.
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
        if (ElectricItemManager.getTier(target) > TIER) return 0L;

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
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > CAPACITY_EU) e = CAPACITY_EU;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long e = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (e == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, e);
    }

    @Override public long getCapacity(ItemStack stack) { return CAPACITY_EU; }
    @Override public long getTransferLimit(ItemStack stack) { return TRANSFER_LIMIT_EU_T; }
    @Override public int getTier(ItemStack stack) { return TIER; }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return true;
    }

    @Override public boolean isItemBarVisible(ItemStack stack) { return getEnergy(stack) < CAPACITY_EU; }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getEnergy(stack) * 13.0f / (float) CAPACITY_EU);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, TIER)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.power_tier", TIER).formatted(Formatting.DARK_GRAY));
    }
}
