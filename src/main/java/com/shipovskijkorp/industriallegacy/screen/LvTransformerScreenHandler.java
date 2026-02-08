package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.block.entity.LvTransformerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LvTransformerScreenHandler extends ScreenHandler {
    public static final int PROP_COUNT = 3;

    private final LvTransformerBlockEntity be;
    private final PropertyDelegate props;

    // Client constructor
    public LvTransformerScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        this(syncId, inv, getBe(inv.player.getWorld(), buf.readBlockPos()), new net.minecraft.screen.ArrayPropertyDelegate(PROP_COUNT));
    }

    // Server constructor
    public LvTransformerScreenHandler(int syncId, PlayerInventory inv, LvTransformerBlockEntity be, PropertyDelegate props) {
        super(ModScreenHandlers.LV_TRANSFORMER, syncId);
        this.be = be;
        this.props = props;
        addProperties(props);
    }

    private static LvTransformerBlockEntity getBe(World world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        if (!(be instanceof LvTransformerBlockEntity tr)) {
            throw new IllegalStateException("LV Transformer BE not found at " + pos);
        }
        return tr;
    }

    public int getLowBuffer() { return props.get(0); }
    public int getHighBuffer() { return props.get(1); }
    public int getDotDirId() { return props.get(2); }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Transformer has no inventory slots (GUI is informational only).
        return ItemStack.EMPTY;
    }
}
