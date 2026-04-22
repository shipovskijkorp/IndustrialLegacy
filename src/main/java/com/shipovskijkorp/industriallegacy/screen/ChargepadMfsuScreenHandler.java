package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.ChargepadMfsuBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;

public class ChargepadMfsuScreenHandler extends AbstractChargepadScreenHandler {
    public ChargepadMfsuScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos());
    }

    private ChargepadMfsuScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, getClientInventory(playerInventory, pos), getClientProperties(playerInventory, pos), pos);
    }

    public ChargepadMfsuScreenHandler(int syncId, PlayerInventory playerInventory, ChargepadMfsuBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, blockEntity.getGuiProperties(), blockEntity.getPos());
    }

    private ChargepadMfsuScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties, BlockPos pos) {
        super(ModScreenHandlers.CHARGEPAD_MFSU, syncId, playerInventory, inventory, properties, pos);
    }

    private static Inventory getClientInventory(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof ChargepadMfsuBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProperties(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof ChargepadMfsuBlockEntity blockEntity) {
            return blockEntity.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
