package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.WaterKineticGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class WaterKineticGeneratorScreen extends HandledScreen<WaterKineticGeneratorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guiwaterkineticgenerator.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int TEXT_COLOR = 0x20EB3E;
    private static final int TITLE_COLOR = 0x404040;

    public WaterKineticGeneratorScreen(WaterKineticGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleY = 166;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, title, titleX, titleY, TITLE_COLOR, false);

        if (handler.isInvalidBiome()) {
            context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.water_kinetic_generator.wrongbiome1"), 38, 52, TEXT_COLOR, false);
            context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.water_kinetic_generator.wrongbiome2"), 45, 69, TEXT_COLOR, false);
            return;
        }
        if (!handler.hasRotor()) {
            context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.water_kinetic_generator.rotormiss"), 27, 52, TEXT_COLOR, false);
            return;
        }
        if (!handler.rotorHasSpace()) {
            context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.water_kinetic_generator.rotorspace"), 20, 52, TEXT_COLOR, false);
            return;
        }
        context.drawText(this.textRenderer, Text.translatable("gui.industrial_legacy.water_kinetic_generator.output", handler.getKuOutput()), 55, 52, TEXT_COLOR, false);
        MutableText rotorHealth = Text.translatable("gui.industrial_legacy.water_kinetic_generator.rotorhealth", handler.getRotorHealth());
        context.drawText(this.textRenderer, rotorHealth.append(" %"), 46, 70, TEXT_COLOR, false);
    }
}
