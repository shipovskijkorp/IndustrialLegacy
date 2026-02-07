package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.screen.MfeScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public class MfeScreen extends HandledScreen<MfeScreenHandler> {
    private static final Identifier BG =
            new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guielectricblock.png");

    // GUI-only icon for the redstone mode indicator (no button background, no item render)
    private static final Identifier REDSTONE_SLOT_ICON = new Identifier("industrial_legacy", "textures/gui/redstone_slot.png");

    // IL VanillaButton: (152,4) size 20x20, texture lives inside guielectricblock.png at (176,0)
    private static final int REDSTONE_BTN_X = 152;
    private static final int REDSTONE_BTN_Y = 4;
    private static final int REDSTONE_BTN_W = 20;
    private static final int REDSTONE_BTN_H = 20;
    private static final int REDSTONE_BTN_U = 176;
    private static final int REDSTONE_BTN_V = 0;

    // IL EnergyGaugeStyle.Bar inner rect (top-left inside the frame)
    private static final int ENERGY_X = 79;
    private static final int ENERGY_Y = 38;
    private static final int ENERGY_W = 24;
    private static final int ENERGY_H = 9;

    // Hover area should include the full gauge background (IL uses 32x32 at offset -4,-11)
    private static final int ENERGY_HOVER_X = ENERGY_X - 4;
    private static final int ENERGY_HOVER_Y = ENERGY_Y - 11;
    private static final int ENERGY_HOVER_W = 32;
    private static final int ENERGY_HOVER_H = 32;

    // IL text color
    private static final int TEXT_COLOR = 4210752;

    public MfeScreen(MfeScreenHandler handler, net.minecraft.entity.player.PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 196; // IL
    }

    @Override
    protected void init() {
        super.init();

        // IL centers the title and doesn't show "Inventory" label.
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;

        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 10_000; // hide
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // Titles (HandledScreen draws them in super)
        super.drawForeground(ctx, mouseX, mouseY);

        // Mirrors IL GuiElectricBlock#drawForegroundLayer
        ctx.drawText(this.textRenderer, Text.translatable("il.EUStorage.gui.info.armor"),
                8, this.backgroundHeight - 126 + 3, TEXT_COLOR, false);

        ctx.drawText(this.textRenderer, Text.translatable("il.EUStorage.gui.info.level"),
                79, 25, TEXT_COLOR, false);

        int cap = handler.getEuCap();
        int stored = handler.getEuStored();
        int e = Math.min(stored, cap);

        ctx.drawText(this.textRenderer, Text.literal(" " + e),
                110, 35, TEXT_COLOR, false);

        ctx.drawText(this.textRenderer, Text.literal("/" + cap),
                110, 45, TEXT_COLOR, false);

        String outStr = String.format(Locale.ROOT, "%.1f", (double) handler.getOutputEUt());
        ctx.drawText(this.textRenderer, Text.translatable("il.EUStorage.gui.info.output", outStr),
                85, 60, TEXT_COLOR, false);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        // Vanilla 1.20+ slot highlight uses a 24x24 animated sprite that protrudes outside
        // IL's tight slot frames. We can't override HandledScreen's private drawSlot() here,
        // so instead we *clip* the protruding pixels by redrawing the underlying GUI texture
        // on the outer strips around the hovered slot.
        if (this.focusedSlot != null) {
            maskVanillaSlotHighlight(ctx, this.focusedSlot);
        }
        if (isMouseOverRedstoneButton(mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.translatable("il.EUStorage.gui.mod.redstone" + handler.getRedstoneMode()),
                    mouseX, mouseY);
        } else if (isMouseOver(ENERGY_HOVER_X, ENERGY_HOVER_Y, ENERGY_HOVER_W, ENERGY_HOVER_H, mouseX, mouseY)) {
            int stored = handler.getEuStored();
            int cap = handler.getEuCap();
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(stored + "/" + cap + " EU"),
                    mouseX, mouseY);
        }
        // Ensure vanilla item tooltips (hovered slots) are drawn on top.
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverRedstoneButton(mouseX, mouseY)) {
            if (this.client != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            var buf = PacketByteBufs.create();
            buf.writeBlockPos(handler.pos);
            ClientPlayNetworking.send(ModPackets.BATBOX_CYCLE_REDSTONE_MODE, buf);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.drawTexture(BG, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);

        // Redstone mode icon (static texture)
        ctx.drawTexture(REDSTONE_SLOT_ICON, x + REDSTONE_BTN_X + 2, y + REDSTONE_BTN_Y + 2, 0, 0, 16, 16, 16, 16);
// energy gauge: draw frame + fill (matches IL EnergyGaugeStyle.Bar)
        float eRatio = handler.getEuCap() <= 0 ? 0f : (handler.getEuStored() / (float) handler.getEuCap());
        IlGuiDraw.drawEnergyBarFramed(ctx, x + ENERGY_X, y + ENERGY_Y, eRatio);
    }

    private boolean isMouseOverRedstoneButton(double mouseX, double mouseY) {
        int bx = this.x + REDSTONE_BTN_X;
        int by = this.y + REDSTONE_BTN_Y;
        return mouseX >= bx && mouseX < (bx + REDSTONE_BTN_W)
                && mouseY >= by && mouseY < (by + REDSTONE_BTN_H);
    }

    private boolean isMouseOver(int relX, int relY, int w, int h, int mouseX, int mouseY) {
        int ax = this.x + relX;
        int ay = this.y + relY;
        return mouseX >= ax && mouseX < ax + w && mouseY >= ay && mouseY < ay + h;
    }

    /**
     * Redraws the underlying GUI texture on the parts of the vanilla 24x24 slot highlight
     * that stick out beyond an 18x18 IL-style slot frame.
     */
    private void maskVanillaSlotHighlight(DrawContext ctx, Slot slot) {
        // Slot coords are relative to the GUI top-left and point at the 16x16 item area.
        final int itemX = this.x + slot.x;
        final int itemY = this.y + slot.y;

        // Vanilla highlight is 24x24 at (itemX-4, itemY-4). IL frame is 18x18 at (itemX-1, itemY-1).
        final int hlX = itemX - 4;
        final int hlY = itemY - 4;

        // Left protruding strip: [itemX-4 .. itemX-2] (3px), full 24px height
        redrawGuiStrip(ctx, hlX, hlY, 3, 24);

        // Right protruding strip: [itemX+17 .. itemX+19] (3px)
        redrawGuiStrip(ctx, itemX + 17, hlY, 3, 24);

        // Top protruding strip above the frame: y = itemY-4 .. itemY-2 (3px), width = 18px aligned to frame
        redrawGuiStrip(ctx, itemX - 1, hlY, 18, 3);

        // Bottom protruding strip: y = itemY+17 .. itemY+19 (3px)
        redrawGuiStrip(ctx, itemX - 1, itemY + 17, 18, 3);
    }

    /**
     * Redraws a rectangle from BG at the same relative GUI coordinates.
     * screenX/screenY are absolute screen coords. Texture UV is derived from GUI-relative coords.
     */
    private void redrawGuiStrip(DrawContext ctx, int screenX, int screenY, int w, int h) {
        int u = screenX - this.x;
        int v = screenY - this.y;
        ctx.drawTexture(BG, screenX, screenY, u, v, w, h, 256, 256);
    }
}
