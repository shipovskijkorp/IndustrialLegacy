package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.StirlingGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class StirlingGeneratorScreen extends HandledScreen<StirlingGeneratorScreenHandler> {
    private static final int TEXT_COLOR = 0x404040;
    private static final int INV_X = 7;
    private static final int INV_Y = 83;

    public StirlingGeneratorScreen(StirlingGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = INV_X;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        IlGuiDraw.drawDefaultBackground(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, this.x + INV_X + col * 18, this.y + INV_Y + row * 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, this.x + INV_X + col * 18, this.y + INV_Y + 58);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        context.drawText(this.textRenderer,
                Text.translatable("gui.industrial_legacy.stirling_generator.bandwidth", this.handler.getMaxOutput()),
                41, 33, TEXT_COLOR, false);
        context.drawText(this.textRenderer,
                Text.translatable("gui.industrial_legacy.stirling_generator.output", this.handler.getEuOutput()),
                41, 45, TEXT_COLOR, false);
    }
}
