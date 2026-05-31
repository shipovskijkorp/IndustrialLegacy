package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Shared IL-ish GUI drawing helpers backed by {@code textures/gui/common.png}.
 *
 * IMPORTANT: Keep this class API stable because multiple screens call it.
 */
public final class IlGuiDraw {
    public static final Identifier COMMON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");
    public static final Identifier INFO_BUTTON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/infobutton.png");
    public static final Identifier BUTTON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/button.png");
    public static final Identifier BUTTON_ACTIVE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/button_enabled.png");
    private static final Identifier ORE_WASHER_GUI = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guiorewashingplant.png");

    // common.png atlas size
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private IlGuiDraw() {}

    /**
     * Default framed GUI background (IL/IL style).
     * Draws a panel frame extending 16px outside the GUI rect.
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
        ctx.drawTexture(COMMON, x, y, u, v, w, h, TEX_W, TEX_H);
    }

    /**
     * Internal helper (kept for older code paths): draw a region from {@link #COMMON}.
     */
    private static void drawTex(DrawContext ctx, int x, int y, int u, int v, int w, int h) {
        ctx.drawTexture(COMMON, x, y, u, v, w, h, TEX_W, TEX_H);
    }


    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    // --- Slot frames ---

    /** Standard slot frame (18x18). */
    public static void drawSlot(DrawContext ctx, int x, int y) {
        // common.png: slot frame at (103,7) size 18x18
        ctx.drawTexture(COMMON, x, y, 103, 7, 18, 18, TEX_W, TEX_H);
    }

    /** Large slot frame (26x26) used for output slots in IL-style GUIs. */
    public static void drawSlotLarge(DrawContext ctx, int x, int y) {
        // common.png: large slot frame at (99,35) size 26x26
        ctx.drawTexture(COMMON, x, y, 99, 35, 26, 26, TEX_W, TEX_H);
    }

    /** Small info button (10x10). */
    public static void drawInfoButton(DrawContext ctx, int x, int y) {
        // Use dedicated texture if present; if missing, it'll just show missing texture (small).
        ctx.drawTexture(INFO_BUTTON, x, y, 0, 0, 10, 10, 10, 10);
    }

    /** Standard 20x20 GUI button. Uses the active texture while hovered. */
    public static void drawButton(DrawContext ctx, int x, int y, boolean hovered) {
        Identifier texture = hovered ? BUTTON_ACTIVE : BUTTON;
        ctx.drawTexture(texture, x, y, 0, 0, 20, 20, 20, 20);
    }

    // --- Energy gauges ---

    /**
     * Energy bar frame background (32x32 at offset -4,-11), matches IL EnergyGaugeStyle.Bar background.
     */
    public static void drawEnergyBarFrame(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 4, y - 11, 128, 0, 32, 32, TEX_W, TEX_H);
    }

    /**
     * Energy bar fill (24x9) inside the frame. Fills left->right.
     */
    public static void drawEnergyBar(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);
        int w = Math.round(24 * ratio);
        if (w <= 0) return;
        ctx.drawTexture(COMMON, x, y, 132, 43, w, 9, TEX_W, TEX_H);
    }

    /**
     * Convenience used by existing screens: draw frame + fill.
     */
    public static void drawEnergyBarFramed(DrawContext ctx, int x, int y, float ratio) {
        drawEnergyBarFrame(ctx, x, y);
        drawEnergyBar(ctx, x, y, ratio);
    }

    /**
     * Fuel gauge used by GeneratorScreen. Fills bottom->top inside a 13x13 window.
     * common.png base at (112,80) size 13x13.
     */
    public static void drawFuelGauge(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);
        // background
        ctx.drawTexture(COMMON, x, y, 112, 80, 13, 13, TEX_W, TEX_H);

        int h = Math.round(13 * ratio);
        if (h <= 0) return;
        // fill: use the same area; crop from bottom
        int srcY = 80 + (13 - h);
        int dstY = y + (13 - h);
        ctx.drawTexture(COMMON, x, dstY, 112, srcY, 13, h, TEX_W, TEX_H);
    }

    // --- Macerator-specific primitives (IL classic layout) ---

    /**
     * Energy bolt gauge for Macerator (16x16 background + 7x13 fill).
     * Draw at the top-left position from guidef (x=59,y=37).
     */
    public static void drawEnergyBolt(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        // background: common.png (96,64) 16x16, with offset (-4,-1) in IL xml
        ctx.drawTexture(COMMON, x - 4, y - 1, 96, 64, 16, 16, TEX_W, TEX_H);

        // fill: common.png (116,65) 7x13, fills bottom->top
        int fillH = Math.round(13 * ratio);
        if (fillH <= 0) return;

        int srcY = 65 + (13 - fillH);
        int dstY = y + (13 - fillH);
        ctx.drawTexture(COMMON, x, dstY, 116, srcY, 7, fillH, TEX_W, TEX_H);
    }
    /**
     * Compatibility helper: some screens call a "framed" bolt.
     * In IL, {@link #drawEnergyBolt} already draws the backing plate, so this is an alias.
     */
    public static void drawEnergyBoltFramed(DrawContext ctx, int x, int y, float ratio) {
        drawEnergyBolt(ctx, x, y, ratio);
    }


    /**
     * Macerator progress (arrow+dust). Uses:
     * background common.png (160,32) 32x16 offset(-5,-3),
     * fill common.png (165,52) 21x11.
     */
    public static void drawProgressCrush(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        // background
        ctx.drawTexture(COMMON, x - 5, y - 3, 160, 32, 32, 16, TEX_W, TEX_H);

        // fill, left->right
        int w = Math.round(21 * ratio);
        if (w <= 0) return;

        ctx.drawTexture(COMMON, x, y, 165, 52, w, 11, TEX_W, TEX_H);
    }

    /**
     * Recycler progress (IL progressrecycler). Uses:
     * background common.png (128,64) 32x16 offset(-5,0),
     * fill common.png (133,80) 18x15.
     */
    public static void drawProgressRecycler(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        drawTex(ctx, x - 5, y, 128, 64, 32, 16);

        int w = (int) Math.floor(18.0f * ratio);
        if (w <= 0) return;

        drawTex(ctx, x, y, 133, 80, w, 15);
    }

    /**
     * IL progressarrow gauge used by electric, iron and induction furnaces.
     * Matches GaugeStyle.ProgressArrow from the IL source:
     * background common.png (160,0) 32x16 at (-5,0), fill common.png (165,16) 22x15.
     */
    public static void drawProgressArrow(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        drawTex(ctx, x - 5, y, 160, 0, 32, 16);

        int w = Math.round(22.0f * ratio);
        if (w <= 0) return;

        drawTex(ctx, x, y, 165, 16, w, 15);
    }


    /**
     * IL progressdrop gauge used by the extractor and pump.
     * Matches GaugeStyle.ProgressDrop:
     * background common.png (160,96) 32x16 at offset (-5,0), fill common.png (165,112) 22x15.
     */
    public static void drawProgressDrop(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        drawTex(ctx, x - 5, y, 160, 96, 32, 16);

        int w = Math.round(22.0f * ratio);
        if (w <= 0) return;

        drawTex(ctx, x, y, 165, 112, w, 15);
    }

    public static void drawProgressTriangle(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        // IL classic "progresstriangle": background (160,64 32x16) at (-5,0), fill (165,80 22x15) at (0,1)
        drawTex(ctx, x - 5, y, 160, 64, 32, 16);
        int w = (int) Math.floor(22.0f * ratio);
        if (w <= 0) return;

        drawTex(ctx, x, y + 1, 165, 80, w, 15);
    }



    /**
     * IL normal fluid tank gauge.
     *
     * Matches TankGauge.createNormal():
     * - full/filled background: common.png (6,100) 20x55
     * - empty background:       common.png (70,100) 20x55
     * - inner fluid area:       x+4/y+4, 12x47, bottom-to-top
     * - foreground scale:       common.png (38,100) 20x55
     */
    public static void drawFluidTankNormal(DrawContext ctx, int x, int y, float ratio, int fluidColor) {
        ratio = clamp01(ratio);

        if (ratio <= 0.0f) {
            drawTex(ctx, x, y, 70, 100, 20, 55);
            return;
        }

        drawTex(ctx, x, y, 6, 100, 20, 55);

        int fillH = Math.round(47.0f * ratio);
        if (fillH > 0) {
            int fluidX = x + 4;
            int fluidY = y + 4 + (47 - fillH);
            ctx.fill(fluidX, fluidY, fluidX + 12, y + 4 + 47, fluidColor);
        }

        drawTex(ctx, x, y, 38, 100, 20, 55);
    }

    /**
     * IL progressorewasher gauge. Unlike most machine gauges this one lives in
     * GUIOreWashingPlant.png, not common.png.
     *
     * IL GaugeStyle.ProgressOreWasher:
     * background: texture (102,38) 20x19 at offset (-1,-1)
     * fill:       texture (177,118) 18x18, left-to-right
     */
    public static void drawProgressOreWasher(DrawContext ctx, int x, int y, float ratio) {
        ratio = clamp01(ratio);

        ctx.drawTexture(ORE_WASHER_GUI, x - 1, y - 1, 102, 38, 20, 19, TEX_W, TEX_H);

        int w = Math.round(18.0f * ratio);
        if (w <= 0) return;

        ctx.drawTexture(ORE_WASHER_GUI, x, y, 177, 118, w, 18, TEX_W, TEX_H);
    }
}
