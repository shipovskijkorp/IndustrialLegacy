package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.GeoGeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GeoGeneratorScreen extends HandledScreen<GeoGeneratorScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guifluidgenerator.png");

    public GeoGeneratorScreen(GeoGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (this.isPointWithinBounds(110, 30, 32, 32, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        } else if (this.isPointWithinBounds(56, 16, 20, 55, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(handler.getFluidAmount() + " / " + handler.getFluidCap() + " mB Lava"),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(BACKGROUND, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);

        float energyRatio = handler.getEnergyCap() <= 0 ? 0.0f : handler.getEnergy() / (float) handler.getEnergyCap();
        int energyH = Math.round(50 * energyRatio);
        if (energyH > 0) {
            int drawY = this.y + 80 - energyH;
            context.fill(this.x + 135, drawY, this.x + 146, this.y + 80, 0xFF33CC33);
        }

        float fluidRatio = handler.getFluidCap() <= 0 ? 0.0f : handler.getFluidAmount() / (float) handler.getFluidCap();
        int fluidH = Math.round(47 * fluidRatio);
        if (fluidH > 0) {
            int drawY = this.y + 64 - fluidH;
            context.fill(this.x + 58, drawY, this.x + 73, this.y + 64, 0xFFFF6A00);
        }
    }
}
