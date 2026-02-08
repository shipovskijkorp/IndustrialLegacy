package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.LvTransformerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * Минималистичный GUI: показывает буферы и сторону DOT.
 * (IC2 у трансформатора GUI не имел, но ты просил интерфейс — делаем лёгкий.)
 */
public class LvTransformerScreen extends HandledScreen<LvTransformerScreenHandler> {

    public LvTransformerScreen(LvTransformerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 88;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // Без текстуры: просто тёмная плашка (не трогаем твои GUI-ассеты)
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xAA000000);
        ctx.drawBorder(x, y, backgroundWidth, backgroundHeight, 0xFF777777);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(textRenderer, title, 8, 6, 0xFFFFFF, false);

        ctx.drawText(textRenderer, Text.literal("LV buffer: " + handler.getLowBuffer() + "/128"), 8, 24, 0xCCCCCC, false);
        ctx.drawText(textRenderer, Text.literal("MV buffer: " + handler.getHighBuffer() + "/128"), 8, 36, 0xCCCCCC, false);
        ctx.drawText(textRenderer, Text.literal("DOT side id: " + handler.getDotDirId()), 8, 48, 0x888888, false);

        ctx.drawText(textRenderer, Text.literal("LV<->MV: dot=MV side"), 8, 64, 0xAAAAAA, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
