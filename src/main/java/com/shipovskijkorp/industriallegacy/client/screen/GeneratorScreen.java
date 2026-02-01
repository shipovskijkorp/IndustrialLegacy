package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.GeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class GeneratorScreen extends HandledScreen<GeneratorScreenHandler> {

    // IL EnergyGaugeStyle.Bar inner rect (top-left inside the frame)
    private static final int ENERGY_X = 100;
    private static final int ENERGY_Y = 39;
    private static final int ENERGY_W = 24;
    private static final int ENERGY_H = 9;

    // Hover area should include the full gauge background (IL uses 32x32 at offset -4,-11)
    private static final int ENERGY_HOVER_X = ENERGY_X - 4;
    private static final int ENERGY_HOVER_Y = ENERGY_Y - 11;
    private static final int ENERGY_HOVER_W = 32;
    private static final int ENERGY_HOVER_H = 32;

    public GeneratorScreen(GeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 8;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Energy tooltip (hover over the full energy gauge frame, like IL)
        if (isMouseOver(ENERGY_HOVER_X, ENERGY_HOVER_Y, ENERGY_HOVER_W, ENERGY_HOVER_H, mouseX, mouseY)) {
            int stored = handler.getEuStored();
            int cap = handler.getEuCap();
            ctx.drawTooltip(this.textRenderer, Text.literal(stored + "/" + cap + " EU"), mouseX, mouseY);
        }
        // Ensure vanilla item tooltips (hovered slots) are drawn on top.
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // IL default framed background from common.png
        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);

        // --- slot frames (IL draws them over the panel) ---
        // machine slots
        IlGuiDraw.drawSlot(ctx, x + 56, y + 16); // charge
        IlGuiDraw.drawSlot(ctx, x + 56, y + 52); // fuel

        // player inventory slots (generator.xml: 7,83)
        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(ctx, invX + col * 18, invY + row * 18);
            }
        }
        // hotbar (startY + 58)
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(ctx, invX + col * 18, hotbarY);
        }

        float eRatio = handler.getEuCap() <= 0 ? 0f : (handler.getEuStored() / (float) handler.getEuCap());
        float fRatio = handler.getFuelMax() <= 0 ? 0f : (handler.getFuel() / (float) handler.getFuelMax());

        // IL coords (generator.xml)
        IlGuiDraw.drawFuelGauge(ctx, x + 57, y + 36, fRatio);

        // energy gauge: draw frame + fill (matches IL EnergyGaugeStyle.Bar)
        IlGuiDraw.drawEnergyBarFramed(ctx, x + ENERGY_X, y + ENERGY_Y, eRatio);
    }

    private boolean isMouseOver(int relX, int relY, int w, int h, int mouseX, int mouseY) {
        int ax = this.x + relX;
        int ay = this.y + relY;
        return mouseX >= ax && mouseX < ax + w && mouseY >= ay && mouseY < ay + h;
    }
}
