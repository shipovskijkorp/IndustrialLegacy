package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.WindKineticGeneratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


/** IL-experimental 2.8.222 wind kinetic generator GUI port. */
public class WindKineticGeneratorScreen extends HandledScreen<WindKineticGeneratorScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guiwindkineticgenerator.png");

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int TEXT_COLOR = 0x20EB3E;
    private static final int TITLE_COLOR = 0x404040;

    private static final int WARNING_U = 176;
    private static final int WARNING_V = 0;
    private static final int WARNING_W = 30;
    private static final int WARNING_H = 26;
    private static final int LEFT_WARNING_X = 44;
    private static final int RIGHT_WARNING_X = 102;
    private static final int WARNING_Y = 20;

    public WindKineticGeneratorScreen(WindKineticGeneratorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(getLegacyTitle())) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 166;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        if (handler.isRotorOverloaded()
                && (isPointWithinBounds(LEFT_WARNING_X, WARNING_Y, WARNING_W, WARNING_H, mouseX, mouseY)
                || isPointWithinBounds(RIGHT_WARNING_X, WARNING_Y, WARNING_W, WARNING_H, mouseX, mouseY))) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("gui.industrial_legacy.wind_kinetic_generator.error.overload"),
                    mouseX, mouseY);
        }
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        if (handler.isRotorOverloaded()) {
            drawWarningIcon(context, LEFT_WARNING_X, WARNING_Y);
            drawWarningIcon(context, RIGHT_WARNING_X, WARNING_Y);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        Text title = getLegacyTitle();
        context.drawText(this.textRenderer, title, (this.backgroundWidth - this.textRenderer.getWidth(title)) / 2, 6, TITLE_COLOR, false);

        Text firstLine = getFirstStatusLine();
        context.drawText(this.textRenderer, firstLine, 21, 51, TEXT_COLOR, false);

        Text secondLine = getSecondStatusLine();
        if (secondLine != null) {
            context.drawText(this.textRenderer, secondLine, 21, 69, TEXT_COLOR, false);
        }
    }

    private void drawWarningIcon(DrawContext context, int guiX, int guiY) {
        context.drawTexture(TEXTURE, x + guiX, y + guiY,
                WARNING_U, WARNING_V, WARNING_W, WARNING_H,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private Text getLegacyTitle() {
        return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.name");
    }

    private Text getFirstStatusLine() {
        if (!handler.hasRotor()) {
            return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.rotormiss");
        }
        if (!handler.rotorHasSpace()) {
            return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.rotorspace");
        }
        if (!handler.isWindStrongEnough()) {
            return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.windweak1");
        }
        return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.output", handler.getKuOutput());
    }

    private Text getSecondStatusLine() {
        if (!handler.hasRotor() || !handler.rotorHasSpace()) {
            return null;
        }
        if (!handler.isWindStrongEnough()) {
            return Text.translatable("gui.industrial_legacy.wind_kinetic_generator.windweak2");
        }
        MutableText rotorHealth = Text.translatable("gui.industrial_legacy.wind_kinetic_generator.rotorhealth", handler.getRotorHealth());
        return rotorHealth.append(" %");
    }
}
