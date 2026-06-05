package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.StirlingGeneratorBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class StirlingGeneratorScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 3;
    private static final int INV_X = 8;
    private static final int INV_Y = 84;

    public final BlockPos pos;
    private final PropertyDelegate props;

    public StirlingGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PacketByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos());
    }

    private StirlingGeneratorScreenHandler(int syncId, PlayerInventory playerInv, BlockPos pos) {
        this(syncId, playerInv, getClientProps(playerInv, pos), pos);
    }

    public StirlingGeneratorScreenHandler(int syncId, PlayerInventory playerInv, StirlingGeneratorBlockEntity be) {
        this(syncId, playerInv, be.getGuiProperties(), be.getPos());
    }

    public StirlingGeneratorScreenHandler(int syncId, PlayerInventory playerInv, PropertyDelegate props, BlockPos pos) {
        super(ModScreenHandlers.STIRLING_GENERATOR, syncId);
        checkDataCount(props, PROP_COUNT);
        this.pos = pos;
        this.props = props;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, INV_Y + 58));
        }

        addProperties(props);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getWorld().getBlockEntity(pos) instanceof StirlingGeneratorBlockEntity
                && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    public int getHeatInput() {
        return props.get(0);
    }

    public int getEuOutput() {
        return props.get(1);
    }

    public int getMaxOutput() {
        return props.get(2);
    }

    private static PropertyDelegate getClientProps(PlayerInventory playerInv, BlockPos pos) {
        if (playerInv.player.getWorld().getBlockEntity(pos) instanceof StirlingGeneratorBlockEntity be) {
            return be.getGuiProperties();
        }
        return new ArrayPropertyDelegate(PROP_COUNT);
    }
}
