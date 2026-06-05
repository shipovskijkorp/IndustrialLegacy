package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.FluidHeatGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FluidHeatGeneratorScreen extends HandledScreen<FluidHeatGeneratorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guifluidheatgenerator.png");
    private static final Identifier COMMON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int TITLE_COLOR = 0x404040;
    private static final int INFO_COLOR = 0x57C4DA;

    private static final int TANK_X = 70;
    private static final int TANK_Y = 20;
    private static final int TANK_W = 20;
    private static final int TANK_H = 55;
    private static final int TANK_FLUID_X = 4;
    private static final int TANK_FLUID_Y = 4;
    private static final int TANK_FLUID_W = 12;
    private static final int TANK_FLUID_H = 47;
    private static final int TANK_FILLED_BG_U = 6;
    private static final int TANK_OVERLAY_U = 38;
    private static final int TANK_EMPTY_U = 70;
    private static final int TANK_V = 100;

    public FluidHeatGeneratorScreen(FluidHeatGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (isPointWithinBounds(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer, getTankTooltip(), mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);
        drawTank(context);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TITLE_COLOR, false);
        context.drawText(this.textRenderer,
                Text.translatable("gui.industrial_legacy.fluid_heat_generator.emit", this.handler.getTransmitHeat()),
                96, 33, INFO_COLOR, false);
        context.drawText(this.textRenderer,
                Text.translatable("gui.industrial_legacy.fluid_heat_generator.max_emit", this.handler.getMaxHeat()),
                96, 52, INFO_COLOR, false);
    }

    private void drawTank(DrawContext context) {
        int amount = this.handler.getTankAmount();
        int capacity = this.handler.getTankCapacity();
        UniversalFluidCellItem.CellFluid fluid = getFluid();
        int drawX = this.x + TANK_X;
        int drawY = this.y + TANK_Y;

        if (amount <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            context.drawTexture(COMMON, drawX, drawY, TANK_EMPTY_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);
            return;
        }

        context.drawTexture(COMMON, drawX, drawY, TANK_FILLED_BG_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);

        int fill = capacity <= 0 ? 0 : Math.max(0, Math.min(TANK_FLUID_H, amount * TANK_FLUID_H / capacity));
        if (fill > 0) {
            int fluidX = drawX + TANK_FLUID_X;
            int fluidY = drawY + TANK_FLUID_Y + (TANK_FLUID_H - fill);
            context.fill(fluidX, fluidY, fluidX + TANK_FLUID_W, fluidY + fill, fluid.tintArgb());
        }

        context.drawTexture(COMMON, drawX, drawY, TANK_OVERLAY_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }

    private Text getTankTooltip() {
        int amount = this.handler.getTankAmount();
        if (amount <= 0) {
            return Text.translatable("fluid.industrial_legacy.empty");
        }

        UniversalFluidCellItem.CellFluid fluid = getFluid();
        if (fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            return Text.translatable("fluid.industrial_legacy.empty");
        }

        return Text.translatable("tooltip.industrial_legacy.fluid_tank",
                amount,
                this.handler.getTankCapacity(),
                fluid.fluidName());
    }

    private UniversalFluidCellItem.CellFluid getFluid() {
        UniversalFluidCellItem.CellFluid[] values = UniversalFluidCellItem.CellFluid.values();
        int ordinal = this.handler.getTankFluidOrdinal();
        if (ordinal < 0 || ordinal >= values.length) {
            return UniversalFluidCellItem.CellFluid.EMPTY;
        }
        return values[ordinal];
    }
}
