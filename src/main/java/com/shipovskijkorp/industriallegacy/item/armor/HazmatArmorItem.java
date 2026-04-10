package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2 Experimental hazmat armor port.
 *
 * Pieces:
 * - hazmat helmet (scuba helmet)
 * - hazmat chestplate
 * - hazmat leggings
 * - rubber boots
 *
 * Behaviour mirrored from ItemArmorHazmat as closely as practical for 1.20.1:
 * - full suit extinguishes fire and protects from heat/electric hazards
 * - helmet auto-consumes air cells from the main inventory when air <= 100
 * - boots provide fall protection handled in a mixin
 * - all pieces have 64 durability and tiny general protection handled in a mixin
 */
public class HazmatArmorItem extends ArmorItem {
    public HazmatArmorItem(Type type, Settings settings) {
        super(type == Type.BOOTS ? ModArmorMaterials.RUBBER_BOOTS : ModArmorMaterials.HAZMAT, type, settings.maxCount(1));
    }

    public static boolean isHazmatPiece(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof HazmatArmorItem;
    }

    public static boolean hasCompleteHazmat(LivingEntity living) {
        return isHazmatPiece(living.getEquippedStack(EquipmentSlot.HEAD))
                && isHazmatPiece(living.getEquippedStack(EquipmentSlot.CHEST))
                && isHazmatPiece(living.getEquippedStack(EquipmentSlot.LEGS))
                && isHazmatPiece(living.getEquippedStack(EquipmentSlot.FEET));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient) return;
        if (!(entity instanceof PlayerEntity player)) return;
        if (this.getType() != Type.HELMET) return;
        if (player.getEquippedStack(EquipmentSlot.HEAD) != stack) return;

        if (player.isOnFire() && hasCompleteHazmat(player)) {
            if (player.isInLava()) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20, 0, true, true));
            }
            player.extinguish();
        }

        int air = player.getAir();
        if (air <= 100) {
            int neededMb = (300 - air) * 1000 / 150;
            int suppliedMb = UniversalFluidCellItem.consumeFluidFromPlayerInventory(player, UniversalFluidCellItem.CellFluid.AIR, neededMb);
            if (suppliedMb > 0) {
                player.setAir(air + suppliedMb * 150 / 1000);
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (getType() == Type.HELMET) {
            tooltip.add(Text.translatable("tooltip.industrial_legacy.hazmat.air_refill").formatted(Formatting.DARK_GRAY));
        }
        if (getType() == Type.BOOTS) {
            tooltip.add(Text.translatable("tooltip.industrial_legacy.hazmat.fall_protection").formatted(Formatting.DARK_GRAY));
        }
    }
}
