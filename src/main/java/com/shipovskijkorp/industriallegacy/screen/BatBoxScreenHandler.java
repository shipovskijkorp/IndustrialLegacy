package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;

public class BatBoxScreenHandler extends AbstractElectricStorageScreenHandler {
    public static final int SLOT_COUNT = AbstractElectricStorageScreenHandler.SLOT_COUNT;
    public static final int PROP_COUNT = AbstractElectricStorageScreenHandler.PROP_COUNT;

    public BatBoxScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos());
    }

    private BatBoxScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, getClientInventory(playerInventory, pos), getClientProperties(playerInventory, pos), pos);
    }

    public BatBoxScreenHandler(int syncId, PlayerInventory playerInventory, BatBoxBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, blockEntity.getGuiProperties(), blockEntity.getPos());
    }

    private BatBoxScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties, BlockPos pos) {
        super(ModScreenHandlers.BATBOX, syncId, playerInventory, inventory, properties, pos);
    }

    private static Inventory getClientInventory(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProperties(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof BatBoxBlockEntity blockEntity) {
            return blockEntity.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
