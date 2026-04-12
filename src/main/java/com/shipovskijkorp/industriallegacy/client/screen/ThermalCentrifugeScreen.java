package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.ThermalCentrifugeScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ThermalCentrifugeScreen extends HandledScreen<ThermalCentrifugeScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guitermalcentrifuge.png");

    public ThermalCentrifugeScreen(ThermalCentrifugeScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 7200;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + 15, y + 42, handler.getEnergyCap() <= 0 ? 0f : handler.getEnergy() / (float) handler.getEnergyCap());

        float progressRatio = handler.getMaxProgress() <= 0 ? 0f : handler.getProgress() / (float) handler.getMaxProgress();
        int progressHeight = Math.round(28 * progressRatio);
        if (progressHeight > 0) {
            int srcY = 33 + (28 - progressHeight);
            int dstY = y + 25 + (28 - progressHeight);
            ctx.drawTexture(IlGuiDraw.COMMON, x + 84, dstY, 252, srcY, 3, progressHeight, 256, 256);
        }

        float heatRatio = handler.getWorkHeat() <= 0 ? 0f : Math.min(1f, handler.getHeat() / (float) handler.getWorkHeat());
        int heatWidth = Math.round(20 * heatRatio);
        if (heatWidth > 0) {
            ctx.drawTexture(IlGuiDraw.COMMON, x + 68, y + 67, 225, 54, heatWidth, 4, 256, 256);
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
        if (this.isPointWithinBounds(15, 38, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
    }
}
