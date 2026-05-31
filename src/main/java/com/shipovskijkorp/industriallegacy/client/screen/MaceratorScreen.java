package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.MaceratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;

public class MaceratorScreen extends HandledScreen<MaceratorScreenHandler> {

    // From guidef/macerator.xml (IL classic layout)
    private static final int SLOT_IN_X = 55;
    private static final int SLOT_IN_Y = 16;

    private static final int SLOT_BAT_X = 55;
    private static final int SLOT_BAT_Y = 52;

    private static final int SLOT_OUT_LARGE_X = 111;
    private static final int SLOT_OUT_LARGE_Y = 30;

    private static final int UPGRADE_X = 151;
    private static final int UPGRADE_Y = 7;

    private static final int ENERGY_BOLT_X = 59;
    private static final int ENERGY_BOLT_Y = 37;

    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 38;

    // как ты попросил
    private static final int PLAYER_INV_X = 6;
    private static final int PLAYER_INV_Y = 82;

    public MaceratorScreen(MaceratorScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
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

    /**
     * ВАЖНО: если render() переопределён неправильно — пропадают стандартные tooltips предметов.
     * Этот render() гарантированно возвращает их.
     */
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY); // <- это и рисует подсказки предметов
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x;
        final int y = this.y;

        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);
        IlGuiDraw.drawInfoButton(ctx, x + 4, y + 4);

        // slot frames
        IlGuiDraw.drawSlot(ctx, x + SLOT_IN_X, y + SLOT_IN_Y);
        IlGuiDraw.drawSlot(ctx, x + SLOT_BAT_X, y + SLOT_BAT_Y);

        // output: ONLY large frame (small inner frame NOT drawn)
        IlGuiDraw.drawSlotLarge(ctx, x + SLOT_OUT_LARGE_X, y + SLOT_OUT_LARGE_Y);

        // upgrades
        for (int i = 0; i < 4; i++) {
            IlGuiDraw.drawSlot(ctx, x + UPGRADE_X, y + UPGRADE_Y + i * 18);
        }

        // gauges
        float eRatio = handler.getEnergyCap() <= 0 ? 0f : (handler.getEnergy() / (float) handler.getEnergyCap());
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, eRatio);

        float pRatio = handler.getMaxProgress() <= 0 ? 0f : (handler.getProgress() / (float) handler.getMaxProgress());
        IlGuiDraw.drawProgressCrush(ctx, x + PROGRESS_X, y + PROGRESS_Y, pRatio);

        // player slot frames
        int invX = x + PLAYER_INV_X;
        int invY = y + PLAYER_INV_Y;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(ctx, invX + col * 18, invY + row * 18);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(ctx, invX + col * 18, hotbarY);
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY); // <- это тултипы предметов!

        // tooltip на молнии (устойчивый hitbox)
        boolean hover =
                this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 16, 16, mouseX, mouseY) ||
                        this.isPointWithinBounds(ENERGY_BOLT_X - 4, ENERGY_BOLT_Y - 1, 16, 16, mouseX, mouseY) ||
                        this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 7, 13, mouseX, mouseY);

        if (hover) {
            int e = handler.getEnergy();
            int cap = handler.getEnergyCap();
            ctx.drawTooltip(this.textRenderer, Text.literal(EnergyDisplayUtil.formatEuStorage(e, cap, 4)), mouseX, mouseY);
        }
    }
}
