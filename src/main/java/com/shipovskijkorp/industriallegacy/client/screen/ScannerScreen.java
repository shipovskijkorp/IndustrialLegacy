package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.ScannerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ScannerScreen extends HandledScreen<ScannerScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guitoolscanner.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;
    private static final int GUI_COLOR_TITLE = 0x404040;
    private static final int GUI_COLOR_HEADER = 0x20ECFE;
    private static final int GUI_COLOR_RESULT = 0x57CADA;

    public ScannerScreen(ScannerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleY = backgroundHeight + 100;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        int count = 0;
        for (ScannerScreenHandler.ScanEntry result : handler.getResults()) {
            if (count >= ScannerScreenHandler.MAX_RESULTS) break;
            int iconX = x + 135 + (count & 1) * 15;
            int iconY = y + 28 + count * 11;
            ItemStack stack = result.stack();
            if (!stack.isEmpty()) {
                context.drawItem(stack, iconX, iconY);
            }
            count++;
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, GUI_COLOR_TITLE, false);
        context.drawText(textRenderer, Text.translatable("gui.industrial_legacy.scanner.found"), 10, 20, GUI_COLOR_HEADER, false);

        int count = 0;
        for (ScannerScreenHandler.ScanEntry result : handler.getResults()) {
            if (count >= ScannerScreenHandler.MAX_RESULTS) break;
            ItemStack stack = result.stack();
            Text name = stack.isEmpty() ? Text.empty() : stack.getName();
            context.drawText(textRenderer,
                    Text.literal(result.count() + "x ").append(name),
                    10, 34 + count * 11, GUI_COLOR_RESULT, false);
            count++;
        }
    }
}
