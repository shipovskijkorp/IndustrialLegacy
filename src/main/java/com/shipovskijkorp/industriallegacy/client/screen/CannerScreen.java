package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.CannerBlockEntity;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.screen.CannerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class CannerScreen extends HandledScreen<CannerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guicanner.png");
    private static final Identifier COMMON = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/common.png");

    private static final int TEX_W = 256;
    private static final int TEX_H = 256;
    private static final int GUI_W = 176;
    private static final int GUI_H = 184;

    private static final int ENERGY_X = 12;
    private static final int ENERGY_Y = 62;

    private static final int INPUT_TANK_X = 39;
    private static final int OUTPUT_TANK_X = 117;
    private static final int TANK_Y = 42;
    private static final int TANK_W = 20;
    private static final int TANK_H = 55;
    private static final int TANK_FILLED_BG_U = 6;
    private static final int TANK_GAUGE_U = 38;
    private static final int TANK_EMPTY_U = 70;
    private static final int TANK_V = 100;

    private static final int MODE_BUTTON_X = 63;
    private static final int MODE_BUTTON_Y = 81;
    private static final int MODE_BUTTON_W = 50;
    private static final int MODE_BUTTON_H = 14;
    private static final int MODE_BUTTON_U = 176;
    private static final int MODE_BUTTON_V = 18;

    private static final int SWAP_BUTTON_X = 77;
    private static final int SWAP_BUTTON_Y = 64;
    private static final int SWAP_BUTTON_W = 22;
    private static final int SWAP_BUTTON_H = 13;

    public CannerScreen(CannerScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = GUI_W;
        this.backgroundHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = this.x;
        int y = this.y;
        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);

        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_X, y + ENERGY_Y,
                handler.getEnergyCap() <= 0 ? 0f : handler.getEnergy() / (float) handler.getEnergyCap());

        drawTank(ctx, x + INPUT_TANK_X, y + TANK_Y, handler.getInputTankFluid(), handler.getInputTankAmount(), handler.getTankCapacity());
        drawTank(ctx, x + OUTPUT_TANK_X, y + TANK_Y, handler.getOutputTankFluid(), handler.getOutputTankAmount(), handler.getTankCapacity());

        switch (handler.getMode()) {
            case BOTTLE_SOLID -> {
                ctx.drawTexture(BACKGROUND, x + 59, y + 53, 9, 18, 3, 4, TEX_W, TEX_H);
                ctx.drawTexture(BACKGROUND, x + 99, y + 53, 18, 23, 3, 4, TEX_W, TEX_H);
            }
            case EMPTY_LIQUID -> {
                ctx.drawTexture(BACKGROUND, x + 71, y + 43, 196, 0, 26, 18, TEX_W, TEX_H);
                ctx.drawTexture(BACKGROUND, x + 59, y + 53, 9, 18, 3, 4, TEX_W, TEX_H);
            }
            case BOTTLE_LIQUID -> {
                ctx.drawTexture(BACKGROUND, x + 99, y + 53, 18, 23, 3, 4, TEX_W, TEX_H);
                ctx.drawTexture(BACKGROUND, x + 71, y + 43, 196, 0, 26, 18, TEX_W, TEX_H);
            }
            case ENRICH_LIQUID -> {
                // Base GUI already carries the center square for enrich mode.
            }
        }

        float ratio = handler.getMaxProgress() <= 0 ? 0f : handler.getProgress() / (float) handler.getMaxProgress();
        int width = Math.round(23 * ratio);
        if (width > 0) {
            ctx.drawTexture(BACKGROUND, x + 74, y + 22, 233, 0, width, 14, TEX_W, TEX_H);
        }

        drawModeButton(ctx, x, y, mouseX, mouseY);
    }

    private void drawModeButton(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        int modeIndex = handler.getMode().ordinal();
        int u = MODE_BUTTON_U;
        int v = MODE_BUTTON_V + modeIndex * MODE_BUTTON_H;
        int drawX = x + MODE_BUTTON_X;
        int drawY = y + MODE_BUTTON_Y;
        ctx.drawTexture(BACKGROUND, drawX, drawY, u, v, MODE_BUTTON_W, MODE_BUTTON_H, TEX_W, TEX_H);

        if (isPointWithinBounds(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H, mouseX, mouseY)) {
            ctx.fill(drawX, drawY, drawX + MODE_BUTTON_W, drawY + MODE_BUTTON_H, 0x22FFFFFF);
        }
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
        if (this.isPointWithinBounds(ENERGY_X, ENERGY_Y, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        }
        if (this.isPointWithinBounds(INPUT_TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, tankTooltip(true), mouseX, mouseY);
        }
        if (this.isPointWithinBounds(OUTPUT_TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, tankTooltip(false), mouseX, mouseY);
        }
        if (this.isPointWithinBounds(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, modeTooltip(handler.getMode()), mouseX, mouseY);
        }
        if (this.isPointWithinBounds(SWAP_BUTTON_X, SWAP_BUTTON_Y, SWAP_BUTTON_W, SWAP_BUTTON_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, Text.translatable("gui.industrial_legacy.canner.switch_tanks"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.interactionManager != null) {
            if (this.isPointWithinBounds(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H, mouseX, mouseY)) {
                CannerBlockEntity.Mode next = handler.getMode().next();
                this.client.interactionManager.clickButton(this.handler.syncId, CannerScreenHandler.BUTTON_MODE_BASE + next.ordinal());
                return true;
            }
            if (this.isPointWithinBounds(SWAP_BUTTON_X, SWAP_BUTTON_Y, SWAP_BUTTON_W, SWAP_BUTTON_H, mouseX, mouseY)) {
                this.client.interactionManager.clickButton(this.handler.syncId, CannerScreenHandler.BUTTON_SWAP_TANKS);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Text tankTooltip(boolean input) {
        UniversalFluidCellItem.CellFluid fluid = input ? handler.getInputTankFluid() : handler.getOutputTankFluid();
        int amount = input ? handler.getInputTankAmount() : handler.getOutputTankAmount();
        String fluidName = switch (fluid) {
            case WATER -> "Water";
            case LAVA -> "Lava";
            case AIR -> "Air";
            default -> "Empty";
        };
        return Text.literal(amount + " / " + handler.getTankCapacity() + " mB " + fluidName);
    }

    private Text modeTooltip(CannerBlockEntity.Mode mode) {
        return switch (mode) {
            case BOTTLE_SOLID -> Text.translatable("gui.industrial_legacy.canner.mode.bottle_solid");
            case EMPTY_LIQUID -> Text.translatable("gui.industrial_legacy.canner.mode.empty_liquid");
            case BOTTLE_LIQUID -> Text.translatable("gui.industrial_legacy.canner.mode.bottle_liquid");
            case ENRICH_LIQUID -> Text.translatable("gui.industrial_legacy.canner.mode.enrich_liquid");
        };
    }

    private static void drawTank(DrawContext ctx, int x, int y, UniversalFluidCellItem.CellFluid fluid, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0 || fluid == UniversalFluidCellItem.CellFluid.EMPTY) {
            ctx.drawTexture(COMMON, x, y, TANK_EMPTY_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);
            return;
        }

        ctx.drawTexture(COMMON, x, y, TANK_FILLED_BG_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);

        float ratio = Math.max(0.0f, Math.min(1.0f, amount / (float) capacity));
        int fillH = Math.round(47 * ratio);
        if (fillH > 0) {
            int color = switch (fluid) {
                case WATER -> 0xFF3F76E4;
                case LAVA -> 0xFFFF6A00;
                case AIR -> 0xFFBFC9D9;
                default -> 0xFFFFFFFF;
            };
            int fillX1 = x + 4;
            int fillX2 = fillX1 + 12;
            int fillY2 = y + 4 + 47;
            int fillY1 = fillY2 - fillH;
            ctx.fill(fillX1, fillY1, fillX2, fillY2, color);
        }

        ctx.drawTexture(COMMON, x, y, TANK_GAUGE_U, TANK_V, TANK_W, TANK_H, TEX_W, TEX_H);
    }
}
