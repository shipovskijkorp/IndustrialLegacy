package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.screen.AbstractChargepadScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Shared GUI for IC2-style charge pads. */
public abstract class AbstractChargepadScreen<T extends AbstractChargepadScreenHandler> extends HandledScreen<T> {
    private static final Identifier BACKGROUND =
            new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guichargepadblock.png");
    private static final Identifier REDSTONE_SLOT_ICON =
            new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/redstone_slot.png");

    private static final int REDSTONE_BTN_X = 152;
    private static final int REDSTONE_BTN_Y = 4;
    private static final int REDSTONE_BTN_W = 20;
    private static final int REDSTONE_BTN_H = 20;

    private static final int ENERGY_X = 79;
    private static final int ENERGY_Y = 38;
    private static final int ENERGY_HOVER_X = ENERGY_X - 4;
    private static final int ENERGY_HOVER_Y = ENERGY_Y - 11;
    private static final int ENERGY_HOVER_W = 32;
    private static final int ENERGY_HOVER_H = 32;
    private static final int TEXT_COLOR = 4210752;

    protected AbstractChargepadScreen(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 161;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        this.playerInventoryTitleX = 8;
        this.playerInventoryTitleY = 67;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);

        int capacity = handler.getEuCap();
        int stored = handler.getEuStored();
        int clamped = Math.min(stored, capacity);

        context.drawText(this.textRenderer, Text.translatable("il.EUStorage.gui.info.level"),
                79, 25, TEXT_COLOR, false);
        context.drawText(this.textRenderer, Text.literal(" " + EnergyDisplayUtil.toSiString(clamped, 4)), 110, 35, TEXT_COLOR, false);
        context.drawText(this.textRenderer, Text.literal("/" + EnergyDisplayUtil.toSiString(capacity, 4)), 110, 45, TEXT_COLOR, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (isMouseOverRedstoneButton(mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.translatable("il.chargepad.gui.mod.redstone" + handler.getRedstoneMode()),
                    mouseX, mouseY);
        } else if (isMouseOver(ENERGY_HOVER_X, ENERGY_HOVER_Y, ENERGY_HOVER_W, ENERGY_HOVER_H, mouseX, mouseY)) {
            context.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEuStored(), handler.getEuCap(), 4)),
                    mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverRedstoneButton(mouseX, mouseY)) {
            if (this.client != null) {
                this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }

            var buffer = PacketByteBufs.create();
            buffer.writeBlockPos(handler.pos);
            ClientPlayNetworking.send(ModPackets.BATBOX_CYCLE_REDSTONE_MODE, buffer);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(BACKGROUND, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
        context.drawTexture(REDSTONE_SLOT_ICON, x + REDSTONE_BTN_X + 2, y + REDSTONE_BTN_Y + 2,
                0, 0, 16, 16, 16, 16);

        float ratio = handler.getEuCap() <= 0 ? 0.0f : handler.getEuStored() / (float) handler.getEuCap();
        IlGuiDraw.drawEnergyBarFramed(context, x + ENERGY_X, y + ENERGY_Y, ratio);
    }

    private boolean isMouseOverRedstoneButton(double mouseX, double mouseY) {
        int buttonX = this.x + REDSTONE_BTN_X;
        int buttonY = this.y + REDSTONE_BTN_Y;
        return mouseX >= buttonX && mouseX < (buttonX + REDSTONE_BTN_W)
                && mouseY >= buttonY && mouseY < (buttonY + REDSTONE_BTN_H);
    }

    private boolean isMouseOver(int relX, int relY, int width, int height, int mouseX, int mouseY) {
        int absoluteX = this.x + relX;
        int absoluteY = this.y + relY;
        return mouseX >= absoluteX && mouseX < absoluteX + width
                && mouseY >= absoluteY && mouseY < absoluteY + height;
    }
}
