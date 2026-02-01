package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.MaceratorScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MaceratorScreen extends HandledScreen<MaceratorScreenHandler> {

    // From guidef/macerator.xml (IC2 classic layout)
    private static final int SLOT_IN_X = 55;
    private static final int SLOT_IN_Y = 16;

    private static final int SLOT_BAT_X = 55;
    private static final int SLOT_BAT_Y = 52;

    private static final int SLOT_OUT_LARGE_X = 111;
    private static final int SLOT_OUT_LARGE_Y = 30;

    private static final int SLOT_OUT_X = 115;
    private static final int SLOT_OUT_Y = 34;

    private static final int UPGRADE_X = 151;
    private static final int UPGRADE_Y = 7;

    private static final int ENERGY_BOLT_X = 59;
    private static final int ENERGY_BOLT_Y = 37;

    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 38;

    public MaceratorScreen(MaceratorScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // center title like IC2
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 72;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        // energy tooltip on bolt icon
        if (this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 7, 13, mouseX, mouseY)) {
            int e = handler.getEnergy();
            int cap = handler.getEnergyCap();
            ctx.drawTooltip(this.textRenderer, Text.literal(e + " / " + cap + " EU"), mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);

        // cosmetic info button (top-left)
        IlGuiDraw.drawInfoButton(ctx, x + 5, y + 5);

        // slots (frames)
        IlGuiDraw.drawSlot(ctx, x + SLOT_IN_X, y + SLOT_IN_Y);     // input
        IlGuiDraw.drawSlot(ctx, x + SLOT_BAT_X, y + SLOT_BAT_Y);   // battery/discharge

        IlGuiDraw.drawSlotLarge(ctx, x + SLOT_OUT_LARGE_X, y + SLOT_OUT_LARGE_Y);
        IlGuiDraw.drawSlot(ctx, x + SLOT_OUT_X, y + SLOT_OUT_Y);   // output (actual slot frame)

        // 4 upgrade slots on the right
        for (int i = 0; i < 4; i++) {
            IlGuiDraw.drawSlot(ctx, x + UPGRADE_X, y + UPGRADE_Y + i * 18);
        }

        // player slots
        int invX = x + 7;
        int invY = y + 83;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                IlGuiDraw.drawSlot(ctx, invX + col * 18, invY + row * 18);
            }
        }
        int hotbarY = invY + 58;
        for (int col = 0; col < 9; col++) {
            IlGuiDraw.drawSlot(ctx, invX + col * 18, hotbarY);
        }

        // energy bolt (no vertical bar in IC2 classic machines)
        IlGuiDraw.drawEnergyBolt(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, handler.getEnergy() > 0);

        // progress (crushing arrow + dust)
        int p = handler.getProgress();
        int pm = Math.max(1, handler.getMaxProgress());
        float ratio = p / (float) pm;
        IlGuiDraw.drawProgressCrush(ctx, x + PROGRESS_X, y + PROGRESS_Y, ratio);
    }
}
