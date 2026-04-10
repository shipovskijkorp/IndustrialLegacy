package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.render.CableBlockEntityRenderer;
import com.shipovskijkorp.industriallegacy.client.screen.BatBoxScreen;
import com.shipovskijkorp.industriallegacy.client.screen.CesuScreen;
import com.shipovskijkorp.industriallegacy.client.screen.CompressorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.GeneratorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.LvTransformerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MvTransformerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.HvTransformerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.EvTransformerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MaceratorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MetalFormerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MfeScreen;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import com.shipovskijkorp.industriallegacy.item.armor.ElectricJetpackItem;
import com.shipovskijkorp.industriallegacy.item.tool.NanoSaberItem;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ClampedModelPredicateProvider;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only init.
 */
public class IndustrialLegacyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerItemModelPredicates();
        registerScreens();
        registerRenderers();
        registerBlockRenderLayers();
        registerBlockColors();
        registerKeyBindings();

        IndustrialLegacy.LOGGER.info("Industrial Legacy client initialized");
    }

    private static void registerItemModelPredicates() {
        registerModelPredicate(ModItems.CABLE, "variant",
                (stack, world, entity, seed) -> (float) CableVariants.variantId(CableItem.getKind(stack), CableItem.getInsulation(stack), CableItem.getOxidation(stack)));

        registerModelPredicate(ModItems.CABLE, "ox", (stack, world, entity, seed) -> {
            CableKind kind = CableItem.getKind(stack);
            int insulation = CableItem.getInsulation(stack);
            if (kind != CableKind.COPPER || insulation != 0) {
                return 0.0f;
            }
            return CableItem.getOxidation(stack) / 3.0f;
        });

        registerChargePredicate(ModItems.RE_BATTERY);
        registerChargePredicate(ModItems.ADVANCED_RE_BATTERY);
        registerChargePredicate(ModItems.ENERGY_CRYSTAL);
        registerChargePredicate(ModItems.LAPOTRON_CRYSTAL);
        registerChargePredicate(ModItems.JETPACK_ELECTRIC);
        registerChargePredicate(ModItems.NANO_SABER);

        registerModelPredicate(ModItems.NANO_SABER, "active",
                (stack, world, entity, seed) -> NanoSaberItem.isActive(stack) ? 1.0f : 0.0f);
    }

    private static void registerChargePredicate(Item item) {
        registerModelPredicate(item, "charge",
                (stack, world, entity, seed) -> ElectricItemManager.getChargeRatio(stack));
    }

    private static void registerModelPredicate(Item item, String path, ClampedModelPredicateProvider provider) {
        ModelPredicateProviderRegistry.register(item, new Identifier(IndustrialLegacy.MOD_ID, path), provider);
    }

    private static void registerScreens() {
        HandledScreens.register(ModScreenHandlers.GENERATOR, GeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.BATBOX, BatBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.CESU, CesuScreen::new);
        HandledScreens.register(ModScreenHandlers.MFE, MfeScreen::new);
        HandledScreens.register(ModScreenHandlers.MACERATOR, MaceratorScreen::new);
        HandledScreens.register(ModScreenHandlers.COMPRESSOR, CompressorScreen::new);
        HandledScreens.register(ModScreenHandlers.METAL_FORMER, MetalFormerScreen::new);
        HandledScreens.register(ModScreenHandlers.LV_TRANSFORMER, LvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.MV_TRANSFORMER, MvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.HV_TRANSFORMER, HvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.EV_TRANSFORMER, EvTransformerScreen::new);
    }

    private static void registerRenderers() {
        BlockEntityRendererFactories.register(ModBlockEntities.CABLE, CableBlockEntityRenderer::new);
    }

    private static void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_GLASS, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LUMINATOR, RenderLayer.getTranslucent());
    }

    private static void registerBlockColors() {
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (world == null || pos == null) {
                return FoliageColors.getDefaultColor();
            }
            return BiomeColors.getFoliageColor(world, pos);
        }, ModBlocks.RUBBER_LEAVES);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> FoliageColors.getDefaultColor(),
                ModBlocks.RUBBER_LEAVES.asItem());
    }

    private static void registerKeyBindings() {
        KeyBinding nightVisionToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.nightvision_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.industrial_legacy"
        ));

        KeyBinding jetpackHoverToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.jetpack_hover_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.industrial_legacy"
        ));

        final boolean[] hoverWasDown = {false};

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (nightVisionToggle.wasPressed()) {
                if (client.player == null) {
                    return;
                }
                ClientPlayNetworking.send(ModPackets.TOGGLE_NIGHTVISION,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }

            boolean hoverDown = jetpackHoverToggle.isPressed();
            if (hoverDown && !hoverWasDown[0]) {
                if (client.player == null) {
                    return;
                }
                ClientPlayNetworking.send(ModPackets.TOGGLE_JETPACK_HOVER,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
            hoverWasDown[0] = hoverDown;

            if (client.player != null && client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).getItem() instanceof ElectricJetpackItem) {
                boolean jump = client.options.jumpKey.isPressed();
                boolean sneak = client.options.sneakKey.isPressed();
                boolean forward = client.options.forwardKey.isPressed();

                var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeBoolean(jump);
                buf.writeBoolean(sneak);
                buf.writeBoolean(forward);
                ClientPlayNetworking.send(ModPackets.JETPACK_INPUT, buf);

                ElectricJetpackItem.tickClientPlayer(client.player, jump, sneak, forward);
            }
        });
    }
}
