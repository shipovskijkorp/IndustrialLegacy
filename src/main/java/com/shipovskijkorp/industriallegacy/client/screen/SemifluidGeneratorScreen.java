package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.SemifluidGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * IC2 experimental Semifluid Generator dynamic GUI recreated from assets/ic2/guidef/semifluid_generator.xml.
 *
 * Source-truth layout:
 * - gui 176x166
 * - title y=6 centered
 * - fluidSlot frame 26/16, output frame 26/52, charge frame 114/48
 * - EnergyGauge bar at 110/30
 * - normal fluid tank at 56/16
 * - player inventory frames at 7/83, hotbar offset 58
 */
public class SemifluidGeneratorScreen extends HandledScreen<SemifluidGeneratorScreenHandler> {
    private static final int GUI_W = 176;
    private static final int GUI_H = 166;

    private static final int ENERGY_X = 110;
    private static final int ENERGY_Y = 30;
    private static final int ENERGY_W = 24;
    private static final int ENERGY_H = 9;

    private static final int FLUID_X = 56;
    private static final int FLUID_Y = 16;
    private static final int FLUID_W = 20;
    private static final int FLUID_H = 55;

    private static final int PLAYER_INV_FRAME_X = 7;
    private static final int PLAYER_INV_FRAME_Y = 83;
    private static final int PLAYER_HOTBAR_FRAME_Y = PLAYER_INV_FRAME_Y + 58;

    public SemifluidGeneratorScreen(SemifluidGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_W;
        this.backgroundHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = PLAYER_INV_FRAME_X + 1;
        this.playerInventoryTitleY = PLAYER_INV_FRAME_Y - 10;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(FLUID_X, FLUID_Y, FLUID_W, FLUID_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.fluid_tank", handler.getTankAmount(), handler.getTankCapacity(), handler.getTankFluid().fluidName()),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(ENERGY_X - 24, ENERGY_Y + 23, 56, 13, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuPerTick(handler.getProduction(), 3)),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        IlGuiDraw.drawDefaultBackground(context, x, y, this.backgroundWidth, this.backgroundHeight);

        drawMachineSlotFrames(context, x, y);
        drawPlayerInventoryFrames(context, x, y);

        float energyRatio = handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap();
        IlGuiDraw.drawEnergyBarFramed(context, x + ENERGY_X, y + ENERGY_Y, energyRatio);

        drawTank(context, x + FLUID_X, y + FLUID_Y, handler.getTankFluid(), handler.getTankAmount(), handler.getTankCapacity());
    }

    private static void drawMachineSlotFrames(DrawContext context, int x, int y) {
        IlGuiDraw.drawSlot(context, x + 26, y + 16);
        IlGuiDraw.drawSlot(context, x + 26, y + 52);
        IlGuiDraw.drawSlot(context, x + 114, y + 48);
    }

    private static void drawPlayerInventoryFrames(DrawContext context, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, x + PLAYER_INV_FRAME_X + col * 18, y + PLAYER_INV_FRAME_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, x + PLAYER_INV_FRAME_X + col * 18, y + PLAYER_HOTBAR_FRAME_Y);
        }
    }

    private static void drawTank(DrawContext context, int x, int y, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        float ratio = amount <= 0 || capacity <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY
                ? 0.0f
                : amount / (float) capacity;
        IlGuiDraw.drawFluidTankNormal(context, x, y, ratio, fluid.tintArgb());
    }
}
