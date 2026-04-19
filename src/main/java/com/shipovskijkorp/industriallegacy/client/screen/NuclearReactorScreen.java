package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.NuclearReactorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NuclearReactorScreen extends HandledScreen<NuclearReactorScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guinuclearreactor.png");

    public NuclearReactorScreen(NuclearReactorScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 230;
        this.backgroundHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 7200;
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
        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);

        int heatBarHeight = Math.round(100.0f * handler.getHeat() / (float) handler.getMaxHeat());
        if (heatBarHeight > 0) {
            ctx.fill(x + 9, y + 131 - heatBarHeight, x + 15, y + 131, 0x88ff5533);
        }

        for (int col = handler.getReactorSize(); col < 9; col++) {
            int dx = x + 26 + col * 18;
            ctx.fill(dx, y + 25, dx + 16, y + 25 + 16 * 6 + 2 * 5, 0x88000000);
        }
    }
}
