package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.block.entity.SolarPanelBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** IC2 Solar Helmet: charges the chest armor item from skylight. */
public final class SolarHelmetItem extends ArmorItem {
    private static final String NBT_SOLAR_BUFFER = "solarBuffer";

    public SolarHelmetItem(Settings settings) {
        super(ModArmorMaterials.SOLAR, Type.HELMET, settings.maxCount(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.HEAD) != stack) return;

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !ElectricItemManager.isElectric(chest)) return;

        float light = SolarPanelBlockEntity.getSkyLight(world, player.getBlockPos());
        if (light <= 0.0f) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        double buffer = MathHelper.clamp(nbt.getDouble(NBT_SOLAR_BUFFER) + light, 0.0D, 16.0D);
        long whole = (long) Math.floor(buffer);
        if (whole <= 0L) {
            nbt.putDouble(NBT_SOLAR_BUFFER, buffer);
            return;
        }

        long accepted = ElectricItemManager.charge(chest, whole, false);
        buffer -= accepted;
        if (buffer <= 0.000001D) {
            nbt.remove(NBT_SOLAR_BUFFER);
            if (nbt.getKeys().isEmpty()) stack.setNbt(null);
        } else {
            nbt.putDouble(NBT_SOLAR_BUFFER, buffer);
        }

        if (accepted > 0L) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }
}
