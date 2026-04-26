package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.SolarPanelScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SolarPanelScreen extends HandledScreen<SolarPanelScreenHandler> {
    private static final Identifier SOLAR_SUN = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/overlay/solar_sun.png");
    private static final int SOLAR_X = 81;
    private static final int SOLAR_Y = 45;
    private static final int SOLAR_W = 14;
    private static final int SOLAR_H = 14;

    public SolarPanelScreen(SolarPanelScreenHandler handler, PlayerInventory inventory, Text title) {
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

        if (isPointWithinBounds(79, 25, 18, 18, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
        if (isPointWithinBounds(SOLAR_X, SOLAR_Y, SOLAR_W, SOLAR_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable(handler.hasSunlight() ? "gui.industrial_legacy.solar_panel.sunlight" : "gui.industrial_legacy.solar_panel.no_sunlight"),
                    mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        IlGuiDraw.drawDefaultBackground(context, x, y, backgroundWidth, backgroundHeight);
        IlGuiDraw.drawSlot(context, x + 79, y + 25);

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

        int u = handler.hasSunlight() ? 14 : 0;
        context.drawTexture(SOLAR_SUN, x + SOLAR_X, y + SOLAR_Y, u, 0, SOLAR_W, SOLAR_H, 28, 14);
    }
}
