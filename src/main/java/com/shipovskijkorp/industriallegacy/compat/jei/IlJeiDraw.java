package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.CannerBlockEntity;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

final class IlJeiDraw {
    static final int DYNAMIC_WIDTH = 160;
    static final int DYNAMIC_HEIGHT = 60;

    private static final Identifier COMMON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");
    private static final Identifier CANNER = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guicanner.png");
    private static final Identifier SOLID_CANNER = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guisolidcanner.png");
    private static final Identifier ORE_WASHER = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guiorewashingplant.png");
    private static final Identifier THERMAL_CENTRIFUGE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guitermalcentrifuge.png");
    private static final Identifier SCRAP_BOX = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/scrapboxrecipes.png");
    private static final Identifier CANNER_ARROW = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/canner_arrow.png");

    private static final int TEX = 256;

    private IlJeiDraw() {}

    static void slot(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x, y, 103, 7, 18, 18, TEX, TEX);
    }

    static void largeSlot(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x, y, 99, 35, 26, 26, TEX, TEX);
    }

    static void energyBolt(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 4, y - 1, 96, 64, 16, 16, TEX, TEX);
        int fillH = animatedSize(13, 300);
        if (fillH > 0) {
            int srcY = 65 + (13 - fillH);
            int dstY = y + (13 - fillH);
            ctx.drawTexture(COMMON, x, dstY, 116, srcY, 7, fillH, TEX, TEX);
        }
    }

    static void progressCrush(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 5, y - 3, 160, 32, 32, 16, TEX, TEX);
        int w = animatedSize(21, 100);
        if (w > 0) ctx.drawTexture(COMMON, x, y, 165, 52, w, 11, TEX, TEX);
    }

    static void progressTriangle(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 5, y, 160, 64, 32, 16, TEX, TEX);
        int w = animatedSize(22, 66);
        if (w > 0) ctx.drawTexture(COMMON, x, y + 1, 165, 80, w, 15, TEX, TEX);
    }


    static void progressDrop(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 5, y, 160, 96, 32, 16, TEX, TEX);
        int w = animatedSize(22, 66);
        if (w > 0) ctx.drawTexture(COMMON, x, y, 165, 112, w, 15, TEX, TEX);
    }

    static void progressRecycler(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x - 5, y, 128, 64, 32, 16, TEX, TEX);
        int w = animatedSize(18, 15);
        if (w > 0) ctx.drawTexture(COMMON, x, y, 133, 80, w, 15, TEX, TEX);
    }

    static void progressMetalFormer(DrawContext ctx, int x, int y) {
        // IC2 GaugeStyle.ProgressMetalFormer:
        // background common.png (192,0) 64x16 at offset (-8,-3), fill common.png (200,19) 46x9.
        ctx.drawTexture(COMMON, x - 8, y - 3, 192, 0, 64, 16, TEX, TEX);
        int w = animatedSize(46, 66);
        if (w > 0) ctx.drawTexture(COMMON, x, y, 200, 19, w, 9, TEX, TEX);
    }

    static void progressArrow(DrawContext ctx, int x, int y) {
        int w = animatedSize(22, 66);
        if (w > 0) ctx.drawTexture(COMMON, x, y, 165, 16, w, 15, TEX, TEX);
    }

    static void progressCentrifuge(DrawContext ctx, int x, int y) {
        int h = animatedSize(28, 166);
        if (h <= 0) return;
        int srcY = 33 + (28 - h);
        int dstY = y + (28 - h);
        ctx.drawTexture(COMMON, x, dstY, 252, srcY, 3, h, TEX, TEX);
    }

    static void heatCentrifuge(DrawContext ctx, int x, int y) {
        ctx.drawTexture(COMMON, x, y, 225, 54, 20, 4, TEX, TEX);
    }

    static void drawSimpleMachineFrame(DrawContext ctx, SimpleMachineJeiCategory.Progress progress) {
        slot(ctx, 55, 0);
        largeSlot(ctx, 111, 14);
        slot(ctx, 55, 36);
        energyBolt(ctx, 59, 21);
        switch (progress) {
            case CRUSH -> progressCrush(ctx, 80, 22);
            case TRIANGLE -> progressTriangle(ctx, 80, 19);
            case RECYCLER -> progressRecycler(ctx, 80, 19);
            case DROP -> progressDrop(ctx, 80, 19);
            case ARROW -> progressTriangle(ctx, 80, 19);
        }
    }

    static void drawMetalFormerFrame(DrawContext ctx) {
        slot(ctx, 16, 0);
        largeSlot(ctx, 111, 14);
        slot(ctx, 16, 36);
        energyBolt(ctx, 20, 21);
        progressMetalFormer(ctx, 52, 24);
    }

    static void drawSolidCannerFrame(DrawContext ctx) {
        slot(ctx, 36, 19);
        slot(ctx, 66, 19);
        slot(ctx, 115, 19);
        ctx.drawTexture(CANNER_ARROW, 54, 19, 0, 0, 12, 18, 16, 32);
        progressArrow(ctx, 89, 20);
    }

    static void drawCannerFrame(DrawContext ctx) {
        drawCannerFrame(ctx, CannerBlockEntity.Mode.BOTTLE_SOLID);
    }

    static void drawCannerFrame(DrawContext ctx, CannerBlockEntity.Mode mode) {
        ctx.drawTexture(CANNER, 0, 0, 40, 16, 96, 81, TEX, TEX);
        ctx.drawTexture(CANNER, 23, 65, 176, 18 + mode.ordinal() * 14, 50, 14, TEX, TEX);

        switch (mode) {
            case BOTTLE_SOLID -> {
                ctx.drawTexture(CANNER, 19, 37, 3, 4, 9, 18, TEX, TEX);
                ctx.drawTexture(CANNER, 59, 37, 3, 4, 18, 23, TEX, TEX);
            }
            case EMPTY_LIQUID -> {
                ctx.drawTexture(CANNER, 31, 27, 196, 0, 26, 18, TEX, TEX);
                ctx.drawTexture(CANNER, 19, 37, 3, 4, 9, 18, TEX, TEX);
            }
            case BOTTLE_LIQUID -> {
                ctx.drawTexture(CANNER, 59, 37, 3, 4, 18, 23, TEX, TEX);
                ctx.drawTexture(CANNER, 31, 27, 196, 0, 26, 18, TEX, TEX);
            }
            case ENRICH_LIQUID -> {
                drawCannerTank(ctx, -1, 26, UniversalFluidCellItem.CellFluid.EMPTY, 0, 8000);
                drawCannerTank(ctx, 77, 26, UniversalFluidCellItem.CellFluid.EMPTY, 0, 8000);
            }
        }

        int w = animatedSize(23, 66);
        if (w > 0) ctx.drawTexture(CANNER, 34, 6, 233, 0, w, 14, TEX, TEX);
    }


    static boolean isCannerTankHovered(int x, int y, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 55;
    }

    static List<Text> cannerTankTooltip(UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY || amount <= 0 || capacity <= 0) {
            return List.of();
        }

        List<Text> tooltip = new ArrayList<>(2);
        tooltip.add(fluid.fluidName());
        tooltip.add(Text.literal(amount + " / " + capacity + " mB").formatted(Formatting.GRAY));
        return tooltip;
    }

    static void drawCannerTank(DrawContext ctx, int x, int y, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            ctx.drawTexture(COMMON, x, y, 70, 100, 20, 55, TEX, TEX);
            return;
        }

        ctx.drawTexture(COMMON, x, y, 6, 100, 20, 55, TEX, TEX);

        float ratio = Math.max(0.0f, Math.min(1.0f, amount / (float) capacity));
        int fillH = Math.round(47 * ratio);
        if (fillH > 0) {
            int fillX1 = x + 4;
            int fillX2 = fillX1 + 12;
            int fillY2 = y + 4 + 47;
            int fillY1 = fillY2 - fillH;
            ctx.fill(fillX1, fillY1, fillX2, fillY2, cannerFluidColor(fluid));
        }

        ctx.drawTexture(COMMON, x, y, 38, 100, 20, 55, TEX, TEX);
    }

    private static int cannerFluidColor(UniversalFluidCellItem.CellFluid fluid) {
        return fluid.tintArgb();
    }

    static void progressOreWasher(DrawContext ctx, int x, int y) {
        ctx.drawTexture(ORE_WASHER, x - 1, y - 1, 102, 38, 20, 19, TEX, TEX);
        int w = animatedSize(18, 166);
        if (w > 0) ctx.drawTexture(ORE_WASHER, x, y, 177, 118, w, 18, TEX, TEX);
    }

    static void drawOreWashingPlantFrame(DrawContext ctx) {
        // IC2 DynamicCategory renders guidef nodes with xOffset=0 and yOffset=-16.
        ctx.drawTexture(ORE_WASHER, 37, 0, 37, 16, 87, 63, TEX, TEX);

        slot(ctx, 37, 0);
        slot(ctx, 37, 45);
        slot(ctx, 103, 0);
        slot(ctx, 85, 45);
        slot(ctx, 103, 45);
        slot(ctx, 121, 45);

        energyBolt(ctx, 15, 22);
        progressOreWasher(ctx, 103, 23);
    }

    static void drawThermalCentrifugeFrame(DrawContext ctx) {
        ctx.drawTexture(THERMAL_CENTRIFUGE, 40, 2, 40, 18, 80, 60, TEX, TEX);
        slot(ctx, 10, 1);
        slot(ctx, 10, 37);
        slot(ctx, 123, 1);
        slot(ctx, 123, 19);
        slot(ctx, 123, 37);
        energyBolt(ctx, 15, 22);
        progressCentrifuge(ctx, 84, 9);
        heatCentrifuge(ctx, 68, 51);
    }

    static void drawScrapBoxFrame(DrawContext ctx) {
        ctx.drawTexture(SCRAP_BOX, 0, 0, 55, 30, 82, 26, TEX, TEX);
    }

    private static int animatedSize(int max, int ticks) {
        int period = Math.max(1, ticks);
        long gameTick = System.currentTimeMillis() / 50L;
        int step = (int) (gameTick % (period + 1));
        return Math.max(1, Math.min(max, Math.round(max * (step / (float) period))));
    }
}
