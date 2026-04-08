package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.HvTransformerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HvTransformerScreen extends HandledScreen<HvTransformerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guitransfomer.png");

    public HvTransformerScreen(HvTransformerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 219;
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.industrial_legacy.transformer.switch.mode1"), button -> sendModeEvent(0))
                .dimensions(this.x + 7, this.y + 65, 144, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.industrial_legacy.transformer.switch.mode2"), button -> sendModeEvent(1))
                .dimensions(this.x + 7, this.y + 85, 144, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.industrial_legacy.transformer.switch.mode3"), button -> sendModeEvent(2))
                .dimensions(this.x + 7, this.y + 105, 144, 20)
                .build());
    }

    private void sendModeEvent(int eventId) {
        var buf = PacketByteBufs.create();
        buf.writeBlockPos(handler.pos);
        buf.writeVarInt(eventId);
        ClientPlayNetworking.send(ModPackets.TRANSFORMER_EVENT, buf);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(BACKGROUND, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        context.drawText(this.textRenderer, this.title, titleX, 6, 4210752, false);
        context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.transformer.output"), 6, 30, 4210752, false);
        context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.transformer.input"), 6, 43, 4210752, false);

        context.drawText(this.textRenderer, Text.literal(handler.getInputFlow() + " EU/t"), 52, 30, 2157374, false);
        context.drawText(this.textRenderer, Text.literal(handler.getOutputFlow() + " EU/t"), 52, 45, 2157374, false);

        int wrenchY = switch (handler.getModeOrdinal()) {
            case 1 -> 87;
            case 2 -> 107;
            default -> 67;
        };
        context.drawItem(ModItems.DEBUG_WRENCH.getDefaultStack(), 152, wrenchY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relX = (int) mouseX - this.x;
            int relY = (int) mouseY - this.y;
            if (relX >= 150 && relY >= 32 && relX <= 167 && relY <= 49) {
                sendModeEvent(3);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
