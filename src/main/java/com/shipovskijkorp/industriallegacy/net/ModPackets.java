package com.shipovskijkorp.industriallegacy.net;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.ElectricFurnaceBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.EvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.HvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.LvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.MvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.RedstoneModeCycleTarget;
import com.shipovskijkorp.industriallegacy.item.flight.ChestFlightManager;
import com.shipovskijkorp.industriallegacy.util.PlayerInputStateManager;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
import com.shipovskijkorp.industriallegacy.item.tool.IModeSwitchableItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Simple networking for GUI buttons / interactions.
 */
public final class ModPackets {
    private ModPackets() {
    }

    public static final Identifier BATBOX_CYCLE_REDSTONE_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "batbox_cycle_redstone_mode");

    public static final Identifier TRANSFORMER_EVENT =
            new Identifier(IndustrialLegacy.MOD_ID, "transformer_event");

    public static final Identifier TOGGLE_NIGHTVISION =
            new Identifier(IndustrialLegacy.MOD_ID, "toggle_nightvision");

    public static final Identifier TOGGLE_CHEST_FLIGHT_HOVER =
            new Identifier(IndustrialLegacy.MOD_ID, "toggle_chest_flight_hover");

    public static final Identifier PLAYER_CONTROL_INPUT =
            new Identifier(IndustrialLegacy.MOD_ID, "player_control_input");

    public static final Identifier METAL_FORMER_CYCLE_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_cycle_mode");

    public static final Identifier ELECTRIC_FURNACE_TAKE_XP =
            new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace_take_xp");

    public static final Identifier CYCLE_HELD_ITEM_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "cycle_held_item_mode");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(BATBOX_CYCLE_REDSTONE_MODE,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    server.execute(() -> handleRedstoneModeCycle(player, pos));
                });

        ServerPlayNetworking.registerGlobalReceiver(TRANSFORMER_EVENT,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    int eventId = buf.readVarInt();
                    server.execute(() -> handleTransformerEvent(player, pos, eventId));
                });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NIGHTVISION,
                (server, player, handler, buf, responseSender) -> server.execute(() -> toggleNightVision(player)));

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_CHEST_FLIGHT_HOVER,
                (server, player, handler, buf, responseSender) -> server.execute(() -> ChestFlightManager.toggleHoverMode(player)));

        ServerPlayNetworking.registerGlobalReceiver(PLAYER_CONTROL_INPUT,
                (server, player, handler, buf, responseSender) -> {
                    boolean jump = buf.readBoolean();
                    boolean sneak = buf.readBoolean();
                    boolean forward = buf.readBoolean();
                    boolean boost = buf.readBoolean();
                    server.execute(() -> PlayerInputStateManager.update(player, jump, sneak, forward, boost));
                });

        ServerPlayNetworking.registerGlobalReceiver(METAL_FORMER_CYCLE_MODE,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    server.execute(() -> handleMetalFormerCycle(player, pos));
                });

        ServerPlayNetworking.registerGlobalReceiver(ELECTRIC_FURNACE_TAKE_XP,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    server.execute(() -> handleElectricFurnaceTakeXp(player, pos));
                });

        ServerPlayNetworking.registerGlobalReceiver(CYCLE_HELD_ITEM_MODE,
                (server, player, handler, buf, responseSender) -> server.execute(() -> handleCycleHeldItemMode(player)));
    }

    private static void handleRedstoneModeCycle(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }

        if (!(player.getWorld().getBlockEntity(pos) instanceof RedstoneModeCycleTarget target)) {
            return;
        }

        target.cycleRedstoneMode(player);
    }

    private static void handleTransformerEvent(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos, int eventId) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }

        var blockEntity = player.getWorld().getBlockEntity(pos);
        if (blockEntity instanceof LvTransformerBlockEntity transformer) {
            transformer.handleClientEvent(eventId);
        } else if (blockEntity instanceof MvTransformerBlockEntity transformer) {
            transformer.handleClientEvent(eventId);
        } else if (blockEntity instanceof HvTransformerBlockEntity transformer) {
            transformer.handleClientEvent(eventId);
        } else if (blockEntity instanceof EvTransformerBlockEntity transformer) {
            transformer.handleClientEvent(eventId);
        }
    }

    private static void handleMetalFormerCycle(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.getWorld().getBlockEntity(pos) instanceof MetalFormerBlockEntity be) {
            be.cycleMode();
        }
    }


    private static void handleElectricFurnaceTakeXp(net.minecraft.server.network.ServerPlayerEntity player, BlockPos pos) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return;
        }
        if (player.getWorld().getBlockEntity(pos) instanceof ElectricFurnaceBlockEntity be) {
            be.collectXp(player);
        }
    }

    private static void handleCycleHeldItemMode(net.minecraft.server.network.ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (mainHand.getItem() instanceof IModeSwitchableItem switchable) {
            switchable.cycleMode(mainHand, player);
            player.sendMessage(Text.translatable("message.industrial_legacy.mode", switchable.getModeName(mainHand)), true);
            return;
        }

        ItemStack offHand = player.getOffHandStack();
        if (offHand.getItem() instanceof IModeSwitchableItem switchable) {
            switchable.cycleMode(offHand, player);
            player.sendMessage(Text.translatable("message.industrial_legacy.mode", switchable.getModeName(offHand)), true);
        }
    }

    private static void toggleNightVision(net.minecraft.server.network.ServerPlayerEntity player) {
        ItemStack head = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!(head.getItem() instanceof INightVisionModule module)) {
            player.sendMessage(Text.translatable("message.industrial_legacy.nightvision.no_module"), true);
            return;
        }

        boolean active = module.isNightVisionActive(head);
        module.setNightVisionActive(head, !active);
        player.sendMessage(Text.translatable(!active
                ? "message.industrial_legacy.nightvision.enabled"
                : "message.industrial_legacy.nightvision.disabled"), true);
    }
}
