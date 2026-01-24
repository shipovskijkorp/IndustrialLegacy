package com.shipovskijkorp.industriallegacy.net;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Simple networking for GUI buttons / interactions.
 */
public final class ModPackets {
    private ModPackets() {}

    public static final Identifier BATBOX_CYCLE_REDSTONE_MODE =
            new Identifier(IndustrialLegacy.MOD_ID, "batbox_cycle_redstone_mode");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(BATBOX_CYCLE_REDSTONE_MODE,
                (server, player, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    server.execute(() -> {
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
                            return;
                        }
                        if (!(player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity bat)) {
                            return;
                        }
                        bat.cycleRedstoneMode(player);
                    });
                });
    }
}
