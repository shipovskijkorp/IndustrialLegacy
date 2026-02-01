package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.CompressorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * IC2-like Compressor GUI (guidef/compressor.xml).
 *
 * Slots:
 *  - input:     (55,16)
 *  - discharge: (55,52)
 *  - output:    large frame at (111,30) (do NOT draw inner small frame)
 *  - upgrades:  4 vertical slots at (151,7)
 *
 * Gauges:
 *  - energy bolt: (59,37) + tooltip on hover
 *  - progress triangle: (80,35)
 */
public class CompressorScreen extends HandledScreen<CompressorScreenHandler> {

    private static final int SLOT_IN_X = 55;
    private static final int SLOT_IN_Y = 16;

    private static final int SLOT_BAT_X = 55;
    private static final int SLOT_BAT_Y = 52;

    private static final int SLOT_OUT_LARGE_X = 111;
    private static final int SLOT_OUT_LARGE_Y = 30;

    private static final int UPGRADE_X = 151;
    private static final int UPGRADE_Y = 7;

    private static final int ENERGY_BOLT_X = 59;
    private static final int ENERGY_BOLT_Y = 37;

    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 35;

    public CompressorScreen(CompressorScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Center title like IC2
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        // Energy tooltip on bolt icon
        if (this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 7, 13, mouseX, mouseY)) {
            int e = handler.getEnergy();
            int cap = handler.getEnergyCap();
            ctx.drawTooltip(this.textRenderer, Text.literal(e + " / " + cap + " EU"), mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x;
        final int y = this.y;

        // Base panel (IC2 style)
        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);

        // Info button (top-left) cosmetic
        IlGuiDraw.drawInfoButton(ctx, x + 4, y + 4);

        // Machine slot frames
        IlGuiDraw.drawSlot(ctx, x + SLOT_IN_X, y + SLOT_IN_Y);     // input
        IlGuiDraw.drawSlot(ctx, x + SLOT_BAT_X, y + SLOT_BAT_Y);   // discharge/battery

        // Output: ONLY large frame. Do NOT draw inner small slot frame.
        IlGuiDraw.drawSlotLarge(ctx, x + SLOT_OUT_LARGE_X, y + SLOT_OUT_LARGE_Y);

        // 4 upgrade slots on the right
        for (int i = 0; i < 4; i++) {
            IlGuiDraw.drawSlot(ctx, x + UPGRADE_X, y + UPGRADE_Y + i * 18);
        }

        // Gauges
        float eRatio = handler.getEnergyCap() <= 0 ? 0f : (handler.getEnergy() / (float) handler.getEnergyCap());
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, eRatio);

        float pRatio = handler.getMaxProgress() <= 0 ? 0f : (handler.getProgress() / (float) handler.getMaxProgress());
        IlGuiDraw.drawProgressTriangle(ctx, x + PROGRESS_X, y + PROGRESS_Y, pRatio);

        // Player slot frames (IC2 style offset)
        int invX = x + 6;
        int invY = y + 82;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(ctx, invX + col * 18, invY + row * 18);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(ctx, invX + col * 18, hotbarY);
        }
    }
}
