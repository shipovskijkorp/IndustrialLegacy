package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.IlGuiDraw;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.screen.MetalFormerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MetalFormerScreen extends HandledScreen<MetalFormerScreenHandler> {
    private static final Identifier BACKGROUND = new Identifier(IndustrialLegacy.MOD_ID, "textures/gui/guimetalformer.png");

    private static final int ENERGY_BOLT_X = 20;
    private static final int ENERGY_BOLT_Y = 37;

    private static final int MODE_BUTTON_X = 65;
    private static final int MODE_BUTTON_Y = 53;
    private static final int MODE_BUTTON_W = 20;
    private static final int MODE_BUTTON_H = 20;

    public MetalFormerScreen(MetalFormerScreenHandler handler, PlayerInventory inv, Text title) {
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

    private ItemStack getModeIconStack() {
        return switch (handler.getModeOrdinal()) {
            case 1 -> ModItems.FORGE_HAMMER.getDefaultStack();
            case 2 -> ModItems.CUTTER.getDefaultStack();
            default -> CableItem.createStack(ModItems.CABLE, CableKind.COPPER, 0);
        };
    }

    private void sendCycleModePacket() {
        var buf = PacketByteBufs.create();
        buf.writeBlockPos(handler.pos);
        ClientPlayNetworking.send(ModPackets.METAL_FORMER_CYCLE_MODE, buf);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int relX = (int) mouseX - this.x;
            int relY = (int) mouseY - this.y;
            if (relX >= MODE_BUTTON_X && relX < MODE_BUTTON_X + MODE_BUTTON_W
                    && relY >= MODE_BUTTON_Y && relY < MODE_BUTTON_Y + MODE_BUTTON_H) {
                sendCycleModePacket();
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

        ctx.drawTexture(BACKGROUND, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
        IlGuiDraw.drawInfoButton(ctx, x + 4, y + 4);

        float eRatio = handler.getEnergyCap() <= 0 ? 0f : (handler.getEnergy() / (float) handler.getEnergyCap());
        IlGuiDraw.drawEnergyBoltFramed(ctx, x + ENERGY_BOLT_X, y + ENERGY_BOLT_Y, eRatio);

        // No triangle progress overlay here: the Metal Former GUI should keep the center clean.
        // Draw a framed mode switch button like the previous UI instead.
        IlGuiDraw.drawSlot(ctx, x + MODE_BUTTON_X + 1, y + MODE_BUTTON_Y + 1);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        super.drawForeground(ctx, mouseX, mouseY);
        ctx.drawItem(getModeIconStack(), MODE_BUTTON_X + 2, MODE_BUTTON_Y + 2);
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (this.isPointWithinBounds(ENERGY_BOLT_X, ENERGY_BOLT_Y, 16, 16, mouseX, mouseY)) {
            ctx.drawTooltip(this.textRenderer,
                    Text.literal(EnergyDisplayUtil.formatEuStorage(handler.getEnergy(), handler.getEnergyCap(), 4)),
                    mouseX, mouseY);
        } else if (this.isPointWithinBounds(MODE_BUTTON_X, MODE_BUTTON_Y, MODE_BUTTON_W, MODE_BUTTON_H, mouseX, mouseY)) {
            Text tip = switch (handler.getModeOrdinal()) {
                case 1 -> Text.translatable("gui.industrial_legacy.metal_former.switch.rolling");
                case 2 -> Text.translatable("gui.industrial_legacy.metal_former.switch.cutting");
                default -> Text.translatable("gui.industrial_legacy.metal_former.switch.extruding");
            };
            ctx.drawTooltip(this.textRenderer, tip, mouseX, mouseY);
        }
    }
}
