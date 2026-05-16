package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.SolarDistillerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SolarDistillerScreen extends HandledScreen<SolarDistillerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guisolardestiller.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int GUI_W = 176;
    private static final int GUI_H = 184;

    private static final int INPUT_TANK_X = 37;
    private static final int INPUT_TANK_Y = 43;
    private static final int INPUT_TANK_W = 53;
    private static final int INPUT_TANK_H = 18;
    private static final int OUTPUT_TANK_X = 115;
    private static final int OUTPUT_TANK_Y = 55;
    private static final int OUTPUT_TANK_W = 17;
    private static final int OUTPUT_TANK_H = 43;

    public SolarDistillerScreen(SolarDistillerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_W;
        this.backgroundHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 92;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (this.isPointWithinBounds(INPUT_TANK_X, INPUT_TANK_Y, INPUT_TANK_W, INPUT_TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.fluid_tank", handler.getInputWater(), handler.getInputCapacity(), UniversalFluidCellItem.CellFluid.WATER.fluidName()),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(OUTPUT_TANK_X, OUTPUT_TANK_Y, OUTPUT_TANK_W, OUTPUT_TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.fluid_tank", handler.getDistilledWater(), handler.getOutputCapacity(), UniversalFluidCellItem.CellFluid.DISTILLED_WATER.fluidName()),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);
        if (handler.canWork()) {
            ctx.drawTexture(BACKGROUND, x + 36, y + 26, 0, 184, 97, 29, TEX_W, TEX_H);
        }
        drawPlainTank(ctx, x + INPUT_TANK_X, y + INPUT_TANK_Y, INPUT_TANK_W, INPUT_TANK_H,
                UniversalFluidCellItem.CellFluid.WATER, handler.getInputWater(), handler.getInputCapacity());
        drawPlainTank(ctx, x + OUTPUT_TANK_X, y + OUTPUT_TANK_Y, OUTPUT_TANK_W, OUTPUT_TANK_H,
                UniversalFluidCellItem.CellFluid.DISTILLED_WATER, handler.getDistilledWater(), handler.getOutputCapacity());
    }

    private static void drawPlainTank(DrawContext ctx, int x, int y, int w, int h, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0) return;
        float ratio = Math.max(0.0f, Math.min(1.0f, amount / (float) capacity));
        int fillH = Math.round(h * ratio);
        if (fillH <= 0) return;
        ctx.fill(x, y + h - fillH, x + w, y + h, fluid.tintArgb());
    }
}
