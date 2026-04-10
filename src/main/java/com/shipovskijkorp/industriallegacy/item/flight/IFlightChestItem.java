package com.shipovskijkorp.industriallegacy.item.flight;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Generic chest-slot flight capability.
 *
 * Any chest armor item that can provide powered flight should implement this
 * interface so the client keybind + server packet flow can treat hover mode and
 * flight input in a universal way, similar to how night vision modules are
 * toggled via the currently equipped armor piece.
 */
public interface IFlightChestItem {
    String NBT_HOVER = "hoverMode";
    String NBT_TOGGLE_TIMER = "toggleTimer";

    default boolean isFlightActive(ItemStack stack) {
        return true;
    }

    default boolean supportsHoverMode(ItemStack stack) {
        return true;
    }

    default boolean isHoverModeActive(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_HOVER);
    }

    default void setHoverModeActive(ItemStack stack, boolean active) {
        if (stack == null || stack.isEmpty()) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(NBT_HOVER, active);
    }

    default int getHoverToggleTimer(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) return 0;
        return stack.getNbt().getByte(NBT_TOGGLE_TIMER) & 0xFF;
    }

    default void setHoverToggleTimer(ItemStack stack, int ticks) {
        if (stack == null || stack.isEmpty()) return;
        stack.getOrCreateNbt().putByte(NBT_TOGGLE_TIMER, (byte) Math.max(0, Math.min(255, ticks)));
    }

    default int getHoverToggleCooldown(ItemStack stack) {
        return 10;
    }

    default void decrementHoverToggleTimer(ItemStack stack) {
        int timer = getHoverToggleTimer(stack);
        if (timer > 0) {
            setHoverToggleTimer(stack, timer - 1);
        }
    }

    default void toggleHoverMode(ServerPlayerEntity player, ItemStack stack) {
        if (!supportsHoverMode(stack)) {
            return;
        }
        if (getHoverToggleTimer(stack) != 0) {
            return;
        }

        boolean active = !isHoverModeActive(stack);
        setHoverModeActive(stack, active);
        setHoverToggleTimer(stack, getHoverToggleCooldown(stack));
        player.sendMessage(Text.translatable(active
                ? "message.industrial_legacy.flight.hover_enabled"
                : "message.industrial_legacy.flight.hover_disabled").formatted(Formatting.GRAY), true);
    }

    default void onGroundHoverDisabled(ServerPlayerEntity player, ItemStack stack) {
        player.sendMessage(Text.translatable("message.industrial_legacy.flight.hover_disabled").formatted(Formatting.GRAY), true);
    }

    void tickFlightServer(ServerPlayerEntity player, ItemStack stack, boolean jump, boolean sneak, boolean forward);

    void tickFlightClient(PlayerEntity player, ItemStack stack, boolean jump, boolean sneak, boolean forward);
}
