package com.shipovskijkorp.industriallegacy.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenCoordinateDebugMixin extends Screen {
    @Shadow protected int x;
    @Shadow protected int y;

    protected HandledScreenCoordinateDebugMixin(Text title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void industriallegacy$debugGuiCoords(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        // ПКМ + Ctrl + Shift
        if (button == 1 && Screen.hasControlDown() && Screen.hasShiftDown()) {
            int relX = (int) mouseX - this.x;
            int relY = (int) mouseY - this.y;
            int absX = (int) mouseX;
            int absY = (int) mouseY;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                String screenName = this.getClass().getSimpleName();
                client.player.sendMessage(
                        Text.literal(screenName + " | gui=" + relX + "," + relY + " | abs=" + absX + "," + absY),
                        false
                );
            }

            cir.setReturnValue(true);
        }
    }
}