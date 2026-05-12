package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.InductionFurnaceScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class InductionFurnaceScreen extends HandledScreen<InductionFurnaceScreenHandler> {
    private static final Identifier INPUT_OVERLAY = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/induction_furnace_input.png");
    private static final Identifier OUTPUT_OVERLAY = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/induction_furnace_output.png");

    private static final int INPUT_OVERLAY_X = 42;
    private static final int INPUT_OVERLAY_Y = 16;
    private static final int OUTPUT_OVERLAY_X = 110;
    private static final int OUTPUT_OVERLAY_Y = 30;
    private static final int DISCHARGE_X = 49;
    private static final int DISCHARGE_Y = 51;
    private static final int UPGRADE_X = 150;
    private static final int UPGRADE_Y = 24;
    private static final int ENERGY_BOLT_X = 55;
    private static final int ENERGY_BOLT_Y = 37;
    private static final int PROGRESS_X = 81;
    private static final int PROGRESS_Y = 35;
    private static final int HEAT_TEXT_X = 10;
    private static final int HEAT_LABEL_Y = 36;
    private static final int HEAT_VALUE_Y = 46;

    public InductionFurnaceScreen(InductionFurnaceScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);
        IlGuiDraw.drawInfoButton(ctx, x + 4, y + 4);

        ctx.drawTexture(INPUT_OVERLAY, x + INPUT_OVERLAY_X, y + INPUT_OVERLAY_Y, 0, 0, 34, 18, 34, 18);
        ctx.drawTexture(OUTPUT_OVERLAY, x + OUTPUT_OVERLAY_X, y + OUTPUT_OVERLAY_Y, 0, 0, 38, 26, 38, 26);

        IlGuiDraw.drawSlot(ctx, x + DISCHARGE_X, y + DISCHARGE_Y);
        for (int i = 0; i < 2; i++) {
            IlGuiDraw.drawSlot(ctx, x + UPGRADE_X, y + UPGRADE_Y + i * 18);
        }

        float eRatio = handler.getEnergyCap() <= 0 ? 0f : handler.getEnergy() / (float) handler.getEnergyCap();
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, eRatio);

        float pRatio = handler.getMaxProgress() <= 0 ? 0f : handler.getProgress() / (float) handler.getMaxProgress();
        IlGuiDraw.drawProgressArrow(ctx, x + PROGRESS_X, y + PROGRESS_Y, pRatio);

        int invX = x + 7;
        int invY = y + 83;
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

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        super.drawForeground(ctx, mouseX, mouseY);
        ctx.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.heat"), HEAT_TEXT_X, HEAT_LABEL_Y, 0x404040, false);
        ctx.drawText(this.textRenderer, Text.literal(handler.getHeatPercent() + "%"), HEAT_TEXT_X, HEAT_VALUE_Y, 0x404040, false);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
        if (this.isPointWithinBounds(ENERGY_BOLT_X - 4, ENERGY_BOLT_Y - 1, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
    }
}
