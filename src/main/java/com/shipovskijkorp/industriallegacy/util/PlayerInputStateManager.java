package com.shipovskijkorp.industriallegacy.util;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side cache of recent player control inputs sent by the client.
 */
public final class PlayerInputStateManager {
    private PlayerInputStateManager() {}

    private static final Map<UUID, InputState> INPUTS = new HashMap<>();

    private static final class InputState {
        boolean jump;
        boolean sneak;
        boolean forward;
        boolean boost;
        int ttl;
    }

    public static void update(ServerPlayerEntity player, boolean jump, boolean sneak, boolean forward, boolean boost) {
        InputState state = INPUTS.computeIfAbsent(player.getUuid(), id -> new InputState());
        state.jump = jump;
        state.sneak = sneak;
        state.forward = forward;
        state.boost = boost;
        state.ttl = 3;
    }

    public static void tick(ServerPlayerEntity player) {
        InputState state = INPUTS.get(player.getUuid());
        if (state == null) return;
        if (state.ttl > 0) state.ttl--;
        if (state.ttl <= 0) INPUTS.remove(player.getUuid());
    }

    public static void clear(ServerPlayerEntity player) {
        INPUTS.remove(player.getUuid());
    }

    private static InputState get(ServerPlayerEntity player) {
        return INPUTS.get(player.getUuid());
    }

    public static boolean isJump(ServerPlayerEntity player) {
        InputState state = get(player);
        return state != null && state.jump;
    }

    public static boolean isSneak(ServerPlayerEntity player) {
        InputState state = get(player);
        return state != null && state.sneak;
    }

    public static boolean isForward(ServerPlayerEntity player) {
        InputState state = get(player);
        return state != null && state.forward;
    }

    public static boolean isBoost(ServerPlayerEntity player) {
        InputState state = get(player);
        return state != null && state.boost;
    }
}
