package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.GeoGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Geothermal Generator GUI.
 *
 * Rebuilt from the IC2 gui definition:
 * - gui size 176x166
 * - title centered at y=6
 * - fluid slot x=26 y=16
 * - output x=26 y=52
 * - charge slot x=114 y=48
 * - energygauge x=110 y=30 style=bar
 * - fluidtank x=56 y=16
 * - player inventory x=7 y=83
 *
 * The static layout comes from the top-left 176x166 area of guifluidgenerator.png.
 * Dynamic parts are then drawn on top at the exact guidef coordinates.
 */
public class GeoGeneratorScreen extends HandledScreen<GeoGeneratorScreenHandler> {
    private static final Identifier BACKGROUND =
            new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guifluidgenerator.png");
    private static final Identifier COMMON =
            new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    private static final int ENERGY_X = 112;
    private static final int ENERGY_Y = 29;

    private static final int FLUID_X = 56;
    private static final int FLUID_Y = 16;
    private static final int FLUID_W = 20;
    private static final int FLUID_H = 55;

    // TankGauge.Normal from common.png
    private static final int TANK_FILLED_BG_U = 6;
    private static final int TANK_GAUGE_U = 38;
    private static final int TANK_EMPTY_U = 70;
    private static final int TANK_V = 100;
    private static final int TANK_FLUID_OFFSET_X = 4;
    private static final int TANK_FLUID_OFFSET_Y = 4;
    private static final int TANK_FLUID_W = 12;
    private static final int TANK_FLUID_H = 47;

    public GeoGeneratorScreen(GeoGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = 7;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(ENERGY_X, ENERGY_Y, 24, 9, mouseX, mouseY)) {
            context.drawTooltip(
                    this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX,
                    mouseY
            );
        }
        if (this.isPointWithinBounds(56, 16, 20, 55, mouseX, mouseY)) {
            context.drawTooltip(
                    this.textRenderer,
                    Text.literal(handler.getFluidAmount() + " / " + handler.getFluidCap() + " mB Lava"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        final int x = this.x;
        final int y = this.y;

        // Standard IC2/IL outer frame.
        IlGuiDraw.drawDefaultBackground(context, x, y, this.backgroundWidth, this.backgroundHeight);

        // Exact static geo generator interior from IC2 texture: top-left 176x166 region only.
        context.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);

        // Exact guidef widgets.
        drawEnergyBarFill(context, x + ENERGY_X, y + ENERGY_Y,
                handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap());

        drawFluidTank(context, x + FLUID_X, y + FLUID_Y, handler.getFluidAmount(), handler.getFluidCap());
        context.fill(183, 91, 208, 105, 0xFFC6C6C6);
        context.fill(228, 72, 242, 128, 0xFFC6C6C6);
        context.fill(221, 123, 242, 128, 0xFFC6C6C6);
    }

    /**
     * Draw only the fill of the IC2 energy bar. The frame is already in the static geo texture.
     */
    private static void drawEnergyBarFill(DrawContext context, int x, int y, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int w = Math.round(24.0f * ratio);
        if (w <= 0) return;
        context.drawTexture(IlGuiDraw.COMMON, x, y, 132, 43, w, 9, TEX_W, TEX_H);
    }

    /**
     * Draw IC2 TankGauge.Normal exactly at guidef coordinates.
     */
    private static void drawFluidTank(DrawContext context, int x, int y, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) {
            context.drawTexture(COMMON, x, y, TANK_EMPTY_U, TANK_V, FLUID_W, FLUID_H, TEX_W, TEX_H);
            return;
        }

        context.drawTexture(COMMON, x, y, TANK_FILLED_BG_U, TANK_V, FLUID_W, FLUID_H, TEX_W, TEX_H);

        float ratio = Math.max(0.0f, Math.min(1.0f, amount / (float) capacity));
        int fillH = Math.round(TANK_FLUID_H * ratio);
        if (fillH > 0) {
            int fillX1 = x + TANK_FLUID_OFFSET_X;
            int fillX2 = fillX1 + TANK_FLUID_W;
            int fillY2 = y + TANK_FLUID_OFFSET_Y + TANK_FLUID_H;
            int fillY1 = fillY2 - fillH;
            context.fill(fillX1, fillY1, fillX2, fillY2, 0xFFFF6A00);
        }

        context.drawTexture(COMMON, x, y, TANK_GAUGE_U, TANK_V, FLUID_W, FLUID_H, TEX_W, TEX_H);
        context.fill(227, 122, 227, 122, 0xFFC6C6C6);
    }
}
