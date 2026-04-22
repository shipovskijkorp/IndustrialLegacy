package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.screen.ElectricFurnaceScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public class ElectricFurnaceScreen extends HandledScreen<ElectricFurnaceScreenHandler> {
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
    private static final int PROGRESS_Y = 35;
    private static final int XP_BUTTON_X = 6;
    private static final int XP_BUTTON_Y = 50;
    private static final int XP_BUTTON_W = 20;
    private static final int XP_BUTTON_H = 20;

    public ElectricFurnaceScreen(ElectricFurnaceScreenHandler handler, PlayerInventory inv, Text title) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relX = (int) mouseX - this.x;
            int relY = (int) mouseY - this.y;
            if (relX >= XP_BUTTON_X && relX < XP_BUTTON_X + XP_BUTTON_W && relY >= XP_BUTTON_Y && relY < XP_BUTTON_Y + XP_BUTTON_H) {
                var buf = PacketByteBufs.create();
                buf.writeBlockPos(handler.pos);
                ClientPlayNetworking.send(ModPackets.ELECTRIC_FURNACE_TAKE_XP, buf);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x;
        final int y = this.y;

        IlGuiDraw.drawDefaultBackground(ctx, x, y, backgroundWidth, backgroundHeight);
        IlGuiDraw.drawInfoButton(ctx, x + 4, y + 4);
        IlGuiDraw.drawSlot(ctx, x + SLOT_IN_X, y + SLOT_IN_Y);
        IlGuiDraw.drawSlot(ctx, x + SLOT_BAT_X, y + SLOT_BAT_Y);
        IlGuiDraw.drawSlotLarge(ctx, x + SLOT_OUT_LARGE_X, y + SLOT_OUT_LARGE_Y);
        for (int i = 0; i < 4; i++) {
            IlGuiDraw.drawSlot(ctx, x + UPGRADE_X, y + UPGRADE_Y + i * 18);
        }

        float eRatio = handler.getEnergyCap() <= 0 ? 0f : (handler.getEnergy() / (float) handler.getEnergyCap());
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, eRatio);
        float pRatio = handler.getMaxProgress() <= 0 ? 0f : (handler.getProgress() / (float) handler.getMaxProgress());
        IlGuiDraw.drawProgressTriangle(ctx, x + PROGRESS_X, y + PROGRESS_Y, pRatio);

        boolean xpHovered = this.isPointWithinBounds(XP_BUTTON_X, XP_BUTTON_Y, XP_BUTTON_W, XP_BUTTON_H, mouseX, mouseY);
        IlGuiDraw.drawButton(ctx, x + XP_BUTTON_X, y + XP_BUTTON_Y, xpHovered);

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
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        super.drawForeground(ctx, mouseX, mouseY);
        ctx.drawItem(Items.EXPERIENCE_BOTTLE.getDefaultStack(), XP_BUTTON_X + 2, XP_BUTTON_Y + 2);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
        if (this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        } else if (this.isPointWithinBounds(XP_BUTTON_X, XP_BUTTON_Y, XP_BUTTON_W, XP_BUTTON_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.translatable("gui.industrial_legacy.electric_furnace.take_xp", handler.getStoredXp()),
                    mouseX, mouseY);
        }
    }
}
