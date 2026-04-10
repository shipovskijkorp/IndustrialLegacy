package com.shipovskijkorp.industriallegacy.item.flight;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Universal routing for chest flight items.
 */
public final class ChestFlightManager {
    private ChestFlightManager() {
    }

    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private static final class InputState {
        boolean jump;
        boolean sneak;
        boolean forward;
        int ttl;
    }

    public static void toggleHoverMode(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            player.sendMessage(Text.translatable("message.industrial_legacy.flight.no_module").formatted(Formatting.GRAY), true);
            return;
        }
        flightItem.toggleHoverMode(player, chest);
    }

    public static void handleInput(ServerPlayerEntity player, boolean jump, boolean sneak, boolean forward) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            INPUTS.remove(player.getUuid());
            return;
        }

        InputState state = INPUTS.computeIfAbsent(player.getUuid(), id -> new InputState());
        state.jump = jump;
        state.sneak = sneak;
        state.forward = forward;
        state.ttl = 3;
    }

    public static void tickServerPlayer(ServerPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof IFlightChestItem flightItem) || !flightItem.isFlightActive(chest)) {
            INPUTS.remove(player.getUuid());
            return;
        }

        flightItem.decrementHoverToggleTimer(chest);

        InputState state = INPUTS.get(player.getUuid());
        boolean jump = state != null && state.jump;
        boolean sneak = state != null && state.sneak;
        boolean forward = state != null && state.forward;
        decayInputState(player.getUuid(), state);

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

    private static void decayInputState(UUID playerId, InputState state) {
        if (state == null) return;
        if (state.ttl > 0) state.ttl--;
        if (state.ttl <= 0) {
            INPUTS.remove(playerId);
        }
    }
}
