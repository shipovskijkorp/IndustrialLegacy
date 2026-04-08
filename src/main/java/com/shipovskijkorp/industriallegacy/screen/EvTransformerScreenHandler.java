package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.EvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EvTransformerScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 3;

    public final BlockPos pos;
    private final PropertyDelegate properties;

    public EvTransformerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readBlockPos());
    }

    private EvTransformerScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, pos, getClientProperties(playerInventory.player.getWorld(), pos));
    }

    public EvTransformerScreenHandler(int syncId, PlayerInventory playerInventory, EvTransformerBlockEntity blockEntity) {
        this(syncId, playerInventory, blockEntity.getPos(), blockEntity.getGuiProperties());
    }

    private EvTransformerScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos, PropertyDelegate properties) {
        super(ModScreenHandlers.EV_TRANSFORMER, syncId);
        this.pos = pos;
        this.properties = properties;
        addProperties(properties);
        addPlayerInventorySlots(playerInventory);
    }

    private static PropertyDelegate getClientProperties(World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof EvTransformerBlockEntity transformer) {
            return transformer.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        final int height = 219;
        final int xStart = (178 - 162) / 2;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, xStart + col * 18, height - 82 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, xStart + col * 18, height - 24));
        }
    }

    public int getModeOrdinal() {
        return properties.get(0);
    }

    public int getInputFlow() {
        return properties.get(1);
    }

    public int getOutputFlow() {
        return properties.get(2);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (player.getWorld().getBlockEntity(pos) instanceof EvTransformerBlockEntity) {
            return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
        return false;
    }

    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int slot) {
        return net.minecraft.item.ItemStack.EMPTY;
    }
}
