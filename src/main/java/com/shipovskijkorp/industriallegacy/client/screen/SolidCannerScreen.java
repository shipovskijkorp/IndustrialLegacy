package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.SolidCannerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SolidCannerScreen extends HandledScreen<SolidCannerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guisolidcanner.png");
    private static final Identifier OVERLAY = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/canner_arrow.png");

    public SolidCannerScreen(SolidCannerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2 - 2;
        this.titleY = 6;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + 11, y + 46, handler.getEnergyCap() <= 0 ? 0f : handler.getEnergy() / (float) handler.getEnergyCap());

        float ratio = handler.getMaxProgress() <= 0 ? 0f : handler.getProgress() / (float) handler.getMaxProgress();
        int width = Math.round(22 * ratio);
        if (width > 0) {
            ctx.drawTexture(IlGuiDraw.COMMON, x + 89, y + 36, 165, 16, width, 15, 256, 256);
        }
        ctx.drawTexture(OVERLAY, x + 54, y + 35, 0, 0, 12, 18, 16, 32);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
        if (this.isPointWithinBounds(11, 46, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
    }
}
