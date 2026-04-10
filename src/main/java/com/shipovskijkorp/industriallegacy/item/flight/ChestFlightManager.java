package com.shipovskijkorp.industriallegacy.item.flight;

import com.shipovskijkorp.industriallegacy.util.PlayerInputStateManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Universal routing for chest flight items.
 */
public final class ChestFlightManager {
    private ChestFlightManager() {
    }

    public static void toggleHoverMode(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            player.sendMessage(Text.translatable("message.industrial_legacy.flight.no_module").formatted(Formatting.GRAY), true);
            return;
        }
        flightItem.toggleHoverMode(player, chest);
    }

    public static void tickServerPlayer(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            return;
        }

        flightItem.decrementHoverToggleTimer(chest);

        boolean jump = PlayerInputStateManager.isJump(player);
        boolean sneak = PlayerInputStateManager.isSneak(player);
        boolean forward = PlayerInputStateManager.isForward(player);

        flightItem.tickFlightServer(player, chest, jump, sneak, forward);
    }

    public static void tickClientPlayer(PlayerEntity player, boolean jump, boolean sneak, boolean forward) {
        if (player == null) return;

        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            return;
        }

        flightItem.tickFlightClient(player, chest, jump, sneak, forward);
    }
}
