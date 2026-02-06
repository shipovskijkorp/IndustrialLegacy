package com.shipovskijkorp.industriallegacy.net;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.item.nvg.INightVisionModule;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Simple networking for GUI buttons / interactions.
 */
public final class ModPackets {
    private ModPackets() {}

    public static final Identifier BATBOX_CYCLE_REDSTONE_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "batbox_cycle_redstone_mode");

    public static final Identifier TOGGLE_NIGHTVISION =
            new Identifier(IndustrialLegacy.MOD_ID, "toggle_nightvision");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(BATBOX_CYCLE_REDSTONE_MODE,
                (srv, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    srv.execute(() -> {
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                            return;
                        }
                        if (!(player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity bat)) {
                            return;
                        }
                        bat.cycleRedstoneMode(player);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_NIGHTVISION,
                (srv, player, handler, buf, responseSender) -> srv.execute(() -> toggleNightVision(player)));
    }

    private static final String NBT_ACTIVE = "active";

    private static void toggleNightVision(ServerPlayerEntity player) {
        ItemStack stack = findFirstNvModule(player);
        if (stack.isEmpty()) {
            player.sendMessage(Text.translatable("message.industrial_legacy.nightvision.no_module"), false);
            return;
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        boolean active = nbt.getBoolean(NBT_ACTIVE);
        nbt.putBoolean(NBT_ACTIVE, !active);

        player.sendMessage(Text.translatable(!active ? "message.industrial_legacy.nightvision.enabled" : "message.industrial_legacy.nightvision.disabled"), true);
    }

    /**
     * Finds the first stack that supports nightvision toggling.
     * Priority: helmet -> offhand -> mainhand -> inventory.
     */
    private static ItemStack findFirstNvModule(PlayerEntity player) {
        // Helmet first (most common)
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!helmet.isEmpty() && helmet.getItem() instanceof INightVisionModule) return helmet;

        // Hands
        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isEmpty() && offhand.getItem() instanceof INightVisionModule) return offhand;

        ItemStack mainhand = player.getMainHandStack();
        if (!mainhand.isEmpty() && mainhand.getItem() instanceof INightVisionModule) return mainhand;

        // Inventory
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof INightVisionModule) return s;
        }

        return ItemStack.EMPTY;
    }
}
