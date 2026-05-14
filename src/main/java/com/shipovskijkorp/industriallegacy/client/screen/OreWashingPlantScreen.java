package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.screen.OreWashingPlantScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class OreWashingPlantScreen extends HandledScreen<OreWashingPlantScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guiorewashingplant.png");
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    public static final int RECIPE_CLICK_X = 103;
    public static final int RECIPE_CLICK_Y = 39;
    public static final int RECIPE_CLICK_W = 18;
    public static final int RECIPE_CLICK_H = 18;

    private static final int ENERGY_X = 12;
    private static final int ENERGY_Y = 44;
    private static final int ENERGY_W = 16;
    private static final int ENERGY_H = 16;

    private static final int WATER_TANK_X = 60;
    private static final int WATER_TANK_Y = 20;
    private static final int WATER_TANK_W = 12;
    private static final int WATER_TANK_H = 55;

    public OreWashingPlantScreen(OreWashingPlantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
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

        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, TEX_W, TEX_H);

        float energyRatio = this.handler.getEnergyCap() <= 0
                ? 0.0f
                : this.handler.getEnergy() / (float) this.handler.getEnergyCap();
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_X, y + ENERGY_Y, energyRatio);

        drawWaterTank(ctx, x + WATER_TANK_X, y + WATER_TANK_Y, this.handler.getWaterAmount(), this.handler.getWaterCapacity());

        float progressRatio = this.handler.getMaxProgress() <= 0
                ? 0.0f
                : this.handler.getProgress() / (float) this.handler.getMaxProgress();
        IlGuiDraw.drawProgressOreWasher(ctx, x + RECIPE_CLICK_X, y + RECIPE_CLICK_Y, progressRatio);
    }

    private void drawWaterTank(DrawContext ctx, int x, int y, int amount, int capacity) {
        int fill = Math.round(WATER_TANK_H * Math.min(1.0f, amount / (float) Math.max(1, capacity)));
        if (fill <= 0) {
            return;
        }

        int dstY = y + WATER_TANK_H - fill;
        ctx.fill(x + 1, dstY, x + WATER_TANK_W - 1, y + WATER_TANK_H, 0xAA3F76E4);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (this.isPointWithinBounds(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, Text.literal(EnergyDisplayUtil.formatEuStorage(this.handler.getEnergy(), this.handler.getEnergyCap(), 4)), mouseX, mouseY);
        }

        if (this.isPointWithinBounds(WATER_TANK_X, WATER_TANK_Y, WATER_TANK_W, WATER_TANK_H, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer, Text.translatable("tooltip.industrial_legacy.ore_washing_plant.water", this.handler.getWaterAmount(), this.handler.getWaterCapacity()), mouseX, mouseY);
        }
    }
}
