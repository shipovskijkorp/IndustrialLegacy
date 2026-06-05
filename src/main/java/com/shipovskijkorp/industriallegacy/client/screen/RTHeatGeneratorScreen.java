package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.RTHeatGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RTHeatGeneratorScreen extends HandledScreen<RTHeatGeneratorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guirtheatgenerator.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int TITLE_COLOR = 0x404040;
    private static final int HEAT_COLOR = 0x57C4DA;
    private static final int HEAT_TEXT_X = 49;
    private static final int HEAT_TEXT_Y = 69;
    private static final int HEAT_TEXT_W = 79;
    private static final int HEAT_TEXT_H = 13;

    public RTHeatGeneratorScreen(RTHeatGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (isPointWithinBounds(HEAT_TEXT_X, HEAT_TEXT_Y, HEAT_TEXT_W, HEAT_TEXT_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.rt_heat_generator.heat"),
                    mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TITLE_COLOR, false);

        Text heatText = Text.literal(this.handler.getTransmitHeat() + " / " + this.handler.getMaxHeat());
        int textX = HEAT_TEXT_X + (HEAT_TEXT_W - this.textRenderer.getWidth(heatText)) / 2;
        context.drawText(this.textRenderer, heatText, textX, HEAT_TEXT_Y, HEAT_COLOR, false);
    }
}
