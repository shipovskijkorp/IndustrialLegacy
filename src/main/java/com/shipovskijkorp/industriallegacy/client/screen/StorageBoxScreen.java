package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.block.StorageBoxBlock;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.StorageBoxScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class StorageBoxScreen extends HandledScreen<StorageBoxScreenHandler> {
    private final StorageBoxBlock.Type type;

    public StorageBoxScreen(StorageBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.type = handler.getStorageBoxType();
        this.backgroundWidth = type.guiWidth();
        this.backgroundHeight = type.guiHeight();
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = type.playerInventoryX() + 1;
        this.playerInventoryTitleY = type.playerInventoryY() - 10;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;

        IlGuiDraw.drawDefaultBackground(context, x, y, this.backgroundWidth, this.backgroundHeight);

        for (int row = 0; row < type.rows(); row++) {
            for (int col = 0; col < type.columns(); col++) {
                IlGuiDraw.drawSlot(context, x + type.inventoryX() + col * 18, y + type.inventoryY() + row * 18);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(context, x + type.playerInventoryX() + col * 18, y + type.playerInventoryY() + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(context, x + type.playerInventoryX() + col * 18, y + type.playerInventoryY() + 58);
        }
    }
}
