package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.RTGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class RTGeneratorScreen extends HandledScreen<RTGeneratorScreenHandler> {
    private static final int ENERGY_X = 115;
    private static final int ENERGY_Y = 39;
    private static final int ENERGY_HOVER_X = ENERGY_X - 4;
    private static final int ENERGY_HOVER_Y = ENERGY_Y - 11;
    private static final int ENERGY_HOVER_W = 32;
    private static final int ENERGY_HOVER_H = 32;

    public RTGeneratorScreen(RTGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleY = 5;
        this.playerInventoryTitleX = 7;
        this.playerInventoryTitleY = 72;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        String title = this.title.getString();
        String line1 = rtTitleLine1(title);
        String line2 = rtTitleLine2(title);

        context.drawText(this.textRenderer, line1, (this.backgroundWidth - this.textRenderer.getWidth(line1)) / 2, this.titleY, 4210752, false);
        context.drawText(this.textRenderer, line2, (this.backgroundWidth - this.textRenderer.getWidth(line2)) / 2, this.titleY + 10, 4210752, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 4210752, false);
    }

    private String rtTitleLine1(String title) {
        int split = title.lastIndexOf(' ');
        return split > 0 ? title.substring(0, split) : title;
    }

    private String rtTitleLine2(String title) {
        int split = title.lastIndexOf(' ');
        return split > 0 ? title.substring(split + 1) : "";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (isPointWithinBounds(ENERGY_HOVER_X, ENERGY_HOVER_Y, ENERGY_HOVER_W, ENERGY_HOVER_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        IlGuiDraw.drawDefaultBackground(context, x, y, backgroundWidth, backgroundHeight);

        int gridX = x + 30;
        int gridY = y + 25;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                IlGuiDraw.drawSlot(context, gridX + col * 18, gridY + row * 18);
            }
        }

        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, invX + col * 18, invY + row * 18);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, invX + col * 18, hotbarY);
        }

        float ratio = handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap();
        IlGuiDraw.drawEnergyBarFramed(context, x + ENERGY_X, y + ENERGY_Y, ratio);
    }
}
