package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class IlGuiDraw {
    public static final Identifier COMMON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private IlGuiDraw() {}

    /**
     * IL default GUI background.
     *
     * Matches {@code il.core.gui.GuiDefaultBackground#drawBackgroundAndTitle} from IL 1.12.2:
     * draws a framed panel from {@code common.png} that extends 16px outside the GUI area.
     */
    public static void drawDefaultBackground(DrawContext ctx, int x, int y, int w, int h) {
        // corners (32x32)
        drawCommon(ctx, x - 16,     y - 16,     32, 32, 0,  0);
        drawCommon(ctx, x + w - 16, y - 16,     32, 32, 64, 0);
        drawCommon(ctx, x - 16,     y + h - 16, 32, 32, 0,  64);
        drawCommon(ctx, x + w - 16, y + h - 16, 32, 32, 64, 64);

        // top & bottom edges
        for (int side = 0; side < 2; side++) {
            int dy = (side == 0) ? (y - 16) : (y + h - 16);
            int v = 64 * side;
            for (int dx = 16; dx < w - 16; dx += 32) {
                int rw = Math.min(32, (w - 16) - dx);
                drawCommon(ctx, x + dx, dy, rw, 32, 32, v);
            }
        }

        // left & right edges
        for (int side = 0; side < 2; side++) {
            int dx = (side == 0) ? (x - 16) : (x + w - 16);
            int u = 64 * side;
            for (int dy = 16; dy < h - 16; dy += 32) {
                int rh = Math.min(32, (h - 16) - dy);
                drawCommon(ctx, dx, y + dy, 32, rh, u, 32);
            }
        }

        // center fill
        for (int dy = 16; dy < h - 16; dy += 32) {
            int rh = Math.min(32, (h - 16) - dy);
            for (int dx = 16; dx < w - 16; dx += 32) {
                int rw = Math.min(32, (w - 16) - dx);
                drawCommon(ctx, x + dx, y + dy, rw, rh, 32, 32);
            }
        }
    }

    private static void drawCommon(DrawContext ctx, int x, int y, int w, int h, int u, int v) {
        ctx.drawTexture(COMMON, x, y, u, v, w, h, 256, 256);
    }

    // slot frame at (103,7) 18x18 in common.png
    public static void drawSlot(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x, y, 103, 7, 18, 18, 256, 256);
    }

    
// energy bar frame background from IL EnergyGaugeStyle.Bar:
// withBackground(-4, -11, 32, 32, 128, 0) in common.png
public static void drawEnergyBarFrame(DrawContext ctx, int x, int y) {
    ctx.drawTexture(COMMON, x - 4, y - 11, 128, 0, 32, 32, 256, 256);
}

public static void drawEnergyBarFramed(DrawContext ctx, int x, int y, float ratio) {
    drawEnergyBarFrame(ctx, x, y);
    drawEnergyBar(ctx, x, y, ratio);
}

// energy bar at (132,43) 24x9, fills left->right
    public static void drawEnergyBar(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);
        int w = Math.round(24 * ratio);
        if (w <= 0) return;
        ctx.drawTexture(COMMON, x, y, 132, 43, w, 9, 256, 256);
    }

    // fuel gauge at (112,80) 13x13, fills bottom->top
    public static void drawFuelGauge(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);
        int h = Math.round(13 * ratio);
        if (h <= 0) return;
        int srcY = 80 + (13 - h);
        ctx.drawTexture(COMMON, x, y + (13 - h), 112, srcY, 13, h, 256, 256);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    // --- IC2-like widgets used by classic machines ---

    /** Large output slot frame (used by macerator/etc). */
    public static void drawSlotLarge(DrawContext ctx, int x, int y) {
        // 64x64 frame in common.png
        drawCommon(ctx, x, y, 16, 16, 64, 64);
    }

    /** Small energy bolt icon (dark/light variants). */
    public static void drawEnergyBolt(DrawContext ctx, int x, int y, boolean lit) {
        drawCommon(ctx, x, y, lit ? 116 : 100, 65, 7, 13);
    }

    /** Info button icon (purely cosmetic for now). */
    public static void drawInfoButton(DrawContext ctx, int x, int y) {
        drawCommon(ctx, x, y, 111, 113, 12, 13);
    }

    /**
     * Macerator-style crushing progress (arrow + dust), filled left-to-right.
     * The sprites are in common.png at (165,96) and (165,112).
     */
    public static void drawProgressCrush(DrawContext ctx, int x, int y, float ratio) {
        ratio = MathHelper.clamp(ratio, 0.0f, 1.0f);

        // background
        drawCommon(ctx, x, y, 165, 96, 22, 15);

        // fill (crop width)
        int w = MathHelper.floor(22.0f * ratio);
        if (w > 0) {
            ctx.drawTexture(COMMON, x, y, 165, 112, w, 15, TEX_W, TEX_H);
        }
    }

}
