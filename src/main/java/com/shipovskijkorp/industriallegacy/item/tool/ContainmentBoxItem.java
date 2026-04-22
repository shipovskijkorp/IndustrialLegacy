package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.screen.ContainmentBoxScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.world.World;

public final class ContainmentBoxItem extends Item {
    public ContainmentBoxItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            user.openHandledScreen(new Factory(hand));
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    private record Factory(Hand hand) implements ExtendedScreenHandlerFactory {
        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            buf.writeEnumConstant(hand);
        }

        @Override
        public Text getDisplayName() {
            return Text.translatable("item.industrial_legacy.containment_box");
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new ContainmentBoxScreenHandler(syncId, playerInventory, hand);
        }
    }
}
