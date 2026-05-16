package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.FluidBottlerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * IC2 experimental Fluid Bottler GUI.
 *
 * Source-truth layout:
 * - GuiFluidBottler: GUIBottler.png, ySize 184
 * - ContainerFluidBottler: discharge 8/53, drain input 44/35, fill input 44/72,
 *   output 117/53, upgrades 152/26..80
 * - EnergyGauge.asBolt at 12/35
 * - TankGauge.createNormal at 78/34
 * - Progress overlays at 61/36, 61/73, 99/55 using GUIBottler.png u=198 v=0.
 */
public class FluidBottlerScreen extends HandledScreen<FluidBottlerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guibottler.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int GUI_W = 176;
    private static final int GUI_H = 184;

    private static final int ENERGY_X = 12;
    private static final int ENERGY_Y = 35;

    private static final int TANK_X = 78;
    private static final int TANK_Y = 34;
    private static final int TANK_W = 20;
    private static final int TANK_H = 55;

    private static final int PROGRESS_U = 198;
    private static final int PROGRESS_V = 0;
    private static final int PROGRESS_W = 16;
    private static final int PROGRESS_H = 13;

    public FluidBottlerScreen(FluidBottlerScreenHandler handler, PlayerInventory inventory, Text title) {
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

        if (this.isPointWithinBounds(ENERGY_X - 4, ENERGY_Y - 1, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.fluid_tank", handler.getTankAmount(), handler.getTankCapacity(), handler.getTankFluid().fluidName()),
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
        IlGuiDraw.drawInfoButton(ctx, x + 3, y + 3);

        float energyRatio = handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap();
        IlGuiDraw.drawEnergyBolt(ctx, x + ENERGY_X, y + ENERGY_Y, energyRatio);

        drawTank(ctx, x + TANK_X, y + TANK_Y, handler.getTankFluid(), handler.getTankAmount(), handler.getTankCapacity());
        drawProgress(ctx, x, y);
    }

    private void drawProgress(DrawContext ctx, int x, int y) {
        float ratio = handler.getProgress() / (float) handler.getMaxProgress();
        int progressSize = Math.round(Math.max(0.0f, Math.min(1.0f, ratio)) * PROGRESS_W);
        if (progressSize <= 0) return;

        ctx.drawTexture(BACKGROUND, x + 61, y + 36, PROGRESS_U, PROGRESS_V, progressSize, PROGRESS_H, TEX_W, TEX_H);
        ctx.drawTexture(BACKGROUND, x + 61, y + 73, PROGRESS_U, PROGRESS_V, progressSize, PROGRESS_H, TEX_W, TEX_H);
        ctx.drawTexture(BACKGROUND, x + 99, y + 55, PROGRESS_U, PROGRESS_V, progressSize, PROGRESS_H, TEX_W, TEX_H);
    }

    private static void drawTank(DrawContext ctx, int x, int y, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        float ratio = amount <= 0 || capacity <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY
                ? 0.0f
                : amount / (float) capacity;
        IlGuiDraw.drawFluidTankNormal(ctx, x, y, ratio, fluid.tintArgb());
    }
}
