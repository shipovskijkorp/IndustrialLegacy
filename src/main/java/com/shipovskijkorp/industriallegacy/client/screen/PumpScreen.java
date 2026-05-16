package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.PumpScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** IC2 Experimental Pump GUI recreated from assets/ic2/guidef/pump.xml. */
public class PumpScreen extends HandledScreen<PumpScreenHandler> {
    private static final int GUI_W = 176;
    private static final int GUI_H = 166;
    private static final Identifier PUMP_ARROW = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/pump_arrow.png");

    private static final int ENERGY_X = 12;
    private static final int ENERGY_Y = 28;
    private static final int PROGRESS_X = 36;
    private static final int PROGRESS_Y = 34;
    private static final int TANK_X = 70;
    private static final int TANK_Y = 16;
    private static final int TANK_W = 20;
    private static final int TANK_H = 55;

    public PumpScreen(PumpScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = GUI_W;
        this.backgroundHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = 7;
        this.playerInventoryTitleY = 73;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(ENERGY_X - 4, ENERGY_Y - 1, 16, 16, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("tooltip.industrial_legacy.fluid_tank", handler.getTankAmount(), handler.getTankCapacity(), handler.getTankFluid().fluidName()),
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
        drawSlotFrames(context, x, y);
        drawPlayerInventoryFrames(context, x, y);

        context.drawTexture(PUMP_ARROW, x + 93, y + 36, 0, 0, 36, 13, 36, 13);

        float energyRatio = handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap();
        IlGuiDraw.drawEnergyBoltFramed(context, x + ENERGY_X, y + ENERGY_Y, energyRatio);

        float progressRatio = handler.getProgress() / (float) handler.getMaxProgress();
        IlGuiDraw.drawProgressDrop(context, x + PROGRESS_X, y + PROGRESS_Y, progressRatio);

        drawTank(context, x + TANK_X, y + TANK_Y, handler.getTankFluid(), handler.getTankAmount(), handler.getTankCapacity());
    }

    private static void drawSlotFrames(DrawContext context, int x, int y) {
        IlGuiDraw.drawSlot(context, x + 98, y + 16);
        IlGuiDraw.drawSlot(context, x + 131, y + 33);
        IlGuiDraw.drawSlot(context, x + 7, y + 43);
        for (int i = 0; i < 4; i++) {
            IlGuiDraw.drawSlot(context, x + 151, y + 7 + i * 18);
        }
    }

    private static void drawPlayerInventoryFrames(DrawContext context, int x, int y) {
        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, invX + col * 18, invY + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, invX + col * 18, invY + 58);
        }
    }

    private static void drawTank(DrawContext context, int x, int y, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        float ratio = amount <= 0 || capacity <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY
                ? 0.0f
                : amount / (float) capacity;
        IlGuiDraw.drawFluidTankNormal(context, x, y, ratio, fluid.tintArgb());
    }
}
