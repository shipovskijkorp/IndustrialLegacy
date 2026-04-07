package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.CesuBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.math.BlockPos;

public class CesuScreenHandler extends AbstractElectricStorageScreenHandler {
    public static final int SLOT_COUNT = AbstractElectricStorageScreenHandler.SLOT_COUNT;
    public static final int PROP_COUNT = AbstractElectricStorageScreenHandler.PROP_COUNT;

    public CesuScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos());
    }

    private CesuScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, getClientInventory(playerInventory, pos), getClientProperties(playerInventory, pos), pos);
    }

    public CesuScreenHandler(int syncId, PlayerInventory playerInventory, CesuBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity, blockEntity.getGuiProperties(), blockEntity.getPos());
    }

    private CesuScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate properties, BlockPos pos) {
        super(ModScreenHandlers.CESU, syncId, playerInventory, inventory, properties, pos);
    }

    private static Inventory getClientInventory(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof CesuBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleInventory(SLOT_COUNT);
    }

    private static PropertyDelegate getClientProperties(PlayerInventory playerInventory, BlockPos pos) {
        if (playerInventory.player.getWorld().getBlockEntity(pos) instanceof CesuBlockEntity blockEntity) {
            return blockEntity.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
