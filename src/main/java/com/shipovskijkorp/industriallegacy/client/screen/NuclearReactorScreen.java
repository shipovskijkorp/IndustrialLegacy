package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.NuclearReactorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NuclearReactorScreen extends HandledScreen<NuclearReactorScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guinuclearreactor.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public NuclearReactorScreen(NuclearReactorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 212;
        this.backgroundHeight = 243;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 10_000;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        context.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);

        int disabledStart = handler.getReactorSize();
        for (int row = 0; row < 6; row++) {
            for (int col = disabledStart; col < 9; col++) {
                context.drawTexture(BACKGROUND, x + 26 + col * 18, y + 25 + row * 18, 213, 1, 16, 16, TEX_W, TEX_H);
            }
        }

        int heatWidth = Math.round(100.0f * handler.getHeat() / (float) handler.getMaxHeat());
        if (heatWidth > 0) {
            context.drawTexture(BACKGROUND, x + 7, y + 136, 0, 243, Math.min(100, heatWidth), 13, TEX_W, TEX_H);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        context.drawText(this.textRenderer,
                Text.translatable("gui.industrial_legacy.nuclear_reactor.output_eu", Math.round(handler.getOutput())),
                111, 139, 5752026, false);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(6, 135, 102, 15, mouseX, mouseY)) {
            double heatPercent = handler.getMaxHeat() <= 0 ? 0.0 : (handler.getHeat() * 100.0) / handler.getMaxHeat();
            context.drawTooltip(this.textRenderer,
                    Text.translatable("gui.industrial_legacy.nuclear_reactor.temp", heatPercent),
                    mouseX, mouseY);
        } else if (this.isPointWithinBounds(5, 160, 18, 18, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("gui.industrial_legacy.nuclear_reactor.mode.electric"),
                    mouseX, mouseY);
        }
    }
}
