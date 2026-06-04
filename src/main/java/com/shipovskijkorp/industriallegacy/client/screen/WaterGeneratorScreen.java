package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.WaterGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class WaterGeneratorScreen extends HandledScreen<WaterGeneratorScreenHandler> {
    private static final int GUI_TEXT_COLOR = 0x404040;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public WaterGeneratorScreen(WaterGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleY = 166;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        IlGuiDraw.drawDefaultBackground(context, x, y, backgroundWidth, backgroundHeight);

        IlGuiDraw.drawSlot(context, x + 80, y + 17);
        IlGuiDraw.drawSlot(context, x + 80, y + 53);
        drawBucketGauge(context, x + 82, y + 36, handler.getWaterRatio());

        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, invX + col * 18, invY + row * 18);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, invX + col * 18, hotbarY);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, GUI_TEXT_COLOR, false);
    }

    private static void drawBucketGauge(DrawContext context, int x, int y, float ratio) {
        ratio = Math.max(0.0F, Math.min(1.0F, ratio));
        context.drawTexture(IlGuiDraw.COMMON, x, y, 96, 111, 14, 16, TEX_W, TEX_H);
        int h = Math.round(16.0F * ratio);
        if (h <= 0) return;
        context.drawTexture(IlGuiDraw.COMMON, x, y + 16 - h, 110, 111 + 16 - h, 14, h, TEX_W, TEX_H);
    }
}
