package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.MagnetizerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MagnetizerScreen extends HandledScreen<MagnetizerScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guimagnetizer.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int TITLE_COLOR = 0x404040;

    public MagnetizerScreen(MagnetizerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleY = 166;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        if (isPointWithinBounds(11, 28, 14, 14, mouseX, mouseY)) {
            context.drawTooltip(textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), handler.getTier())),
                    mouseX, mouseY);
        }
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        IlGuiDraw.drawEnergyBolt(context, x + 11, y + 28,
                handler.getEnergyCap() <= 0 ? 0.0F : (float) handler.getEnergy() / (float) handler.getEnergyCap());
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, TITLE_COLOR, false);

        boolean metal = handler.hasMetalShoes(client.player);
        context.drawText(textRenderer,
                Text.translatable(metal
                        ? "gui.industrial_legacy.magnetizer.has_metal_shoes"
                        : "gui.industrial_legacy.magnetizer.no_metal_shoes"),
                18, 66, metal ? 0x40FF40 : 0xFF4040, false);
    }
}
