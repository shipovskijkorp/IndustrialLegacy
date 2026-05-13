package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/** IC2 Static Boots: walking charges the chest armor item. */
public final class StaticBootsItem extends ArmorItem {
    private static final String NBT_X = "static_x";
    private static final String NBT_Z = "static_z";
    private static final String NBT_STATIC_BUFFER = "staticBuffer";

    public StaticBootsItem(Settings settings) {
        super(ModArmorMaterials.STATIC_BOOTS, Type.BOOTS, settings.maxCount(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient || !(entity instanceof ServerPlayerEntity player)) return;
        if (player.getEquippedStack(EquipmentSlot.FEET) != stack) return;

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !ElectricItemManager.isElectric(chest)) return;

        NbtCompound nbt = stack.getOrCreateNbt();
        boolean reset = player.hasVehicle() || player.isTouchingWater();
        int x = MathHelper.floor(player.getX());
        int z = MathHelper.floor(player.getZ());

        if (!nbt.contains(NBT_X) || reset) nbt.putInt(NBT_X, x);
        if (!nbt.contains(NBT_Z) || reset) nbt.putInt(NBT_Z, z);
        if (reset) return;

        int lastX = nbt.getInt(NBT_X);
        int lastZ = nbt.getInt(NBT_Z);
        double dx = lastX - x;
        double dz = lastZ - z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 5.0D) return;

        nbt.putInt(NBT_X, x);
        nbt.putInt(NBT_Z, z);

        double generated = Math.min(3.0D, distance / 5.0D);
        double buffer = MathHelper.clamp(nbt.getDouble(NBT_STATIC_BUFFER) + generated, 0.0D, 16.0D);
        long whole = (long) Math.floor(buffer);
        if (whole <= 0L) {
            nbt.putDouble(NBT_STATIC_BUFFER, buffer);
            return;
        }

        long accepted = ElectricItemManager.charge(chest, whole, false);
        buffer -= accepted;
        if (buffer <= 0.000001D) {
            nbt.remove(NBT_STATIC_BUFFER);
        } else {
            nbt.putDouble(NBT_STATIC_BUFFER, buffer);
        }

        if (accepted > 0L) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }
}
