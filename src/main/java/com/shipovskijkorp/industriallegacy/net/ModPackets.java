package com.shipovskijkorp.industriallegacy.net;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.EvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.HvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.LvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.MvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.RedstoneModeCycleTarget;
import com.shipovskijkorp.industriallegacy.item.armor.ElectricJetpackItem;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
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

    public static final Identifier TOGGLE_JETPACK_HOVER =
            new Identifier(IndustrialLegacy.MOD_ID, "toggle_jetpack_hover");

    public static final Identifier JETPACK_INPUT =
            new Identifier(IndustrialLegacy.MOD_ID, "jetpack_input");

    public static final Identifier METAL_FORMER_CYCLE_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_cycle_mode");

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

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_JETPACK_HOVER,
                (server, player, handler, buf, responseSender) -> server.execute(() -> ElectricJetpackItem.toggleHoverMode(player)));

        ServerPlayNetworking.registerGlobalReceiver(JETPACK_INPUT,
                (server, player, handler, buf, responseSender) -> {
                    boolean jump = buf.readBoolean();
                    boolean sneak = buf.readBoolean();
                    boolean forward = buf.readBoolean();
                    server.execute(() -> ElectricJetpackItem.handleInput(player, jump, sneak, forward));
                });

        ServerPlayNetworking.registerGlobalReceiver(METAL_FORMER_CYCLE_MODE,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    server.execute(() -> handleMetalFormerCycle(player, pos));
                });
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
