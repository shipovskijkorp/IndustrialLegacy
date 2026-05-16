package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.particle.ChargepadParticle;
import com.shipovskijkorp.industriallegacy.client.render.CableBlockEntityRenderer;
import com.shipovskijkorp.industriallegacy.client.render.MiningLaserEntityRenderer;
import com.shipovskijkorp.industriallegacy.client.screen.*;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import com.shipovskijkorp.industriallegacy.item.armor.BiogasJetpackItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumLeggingsItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumBootsItem;
import com.shipovskijkorp.industriallegacy.item.flight.ChestFlightManager;
import com.shipovskijkorp.industriallegacy.item.flight.IFlightChestItem;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.item.tool.NanoSaberItem;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModFluids;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModParticles;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
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
        registerParticles();
        registerFluidRenderers();
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
        registerChargePredicate(ModItems.CHARGING_RE_BATTERY);
        registerChargePredicate(ModItems.ADVANCED_CHARGING_RE_BATTERY);
        registerChargePredicate(ModItems.ENERGY_CRYSTAL);
        registerChargePredicate(ModItems.LAPOTRON_CRYSTAL);
        registerChargePredicate(ModItems.CHARGING_ENERGY_CRYSTAL);
        registerChargePredicate(ModItems.CHARGING_LAPOTRON_CRYSTAL);
        registerChargePredicate(ModItems.BATPACK);
        registerChargePredicate(ModItems.ADVANCED_BATPACK);
        registerModelPredicate(ModItems.JETPACK, "fuel",
                (stack, world, entity, seed) -> BiogasJetpackItem.getFuelRatio(stack));
        registerChargePredicate(ModItems.JETPACK_ELECTRIC);
        registerChargePredicate(ModItems.NANO_HELMET);
        registerChargePredicate(ModItems.NANO_CHESTPLATE);
        registerChargePredicate(ModItems.NANO_LEGGINGS);
        registerChargePredicate(ModItems.NANO_BOOTS);
        registerChargePredicate(ModItems.QUANTUM_HELMET);
        registerChargePredicate(ModItems.QUANTUM_CHESTPLATE);
        registerChargePredicate(ModItems.QUANTUM_LEGGINGS);
        registerChargePredicate(ModItems.QUANTUM_BOOTS);
        registerChargePredicate(ModItems.NANO_SABER);
        registerChargePredicate(ModItems.MINING_LASER);
        registerChargePredicate(ModItems.ENERGY_PACK);

        registerModelPredicate(ModItems.FLUID_CELL, "cell",
                (stack, world, entity, seed) -> UniversalFluidCellItem.getModelPredicate(stack));

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
        HandledScreens.register(ModScreenHandlers.GEO_GENERATOR, GeoGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.SOLAR_PANEL, SolarPanelScreen::new);
        HandledScreens.register(ModScreenHandlers.RT_GENERATOR, RTGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.SEMIFLUID_GENERATOR, SemifluidGeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
        HandledScreens.register(ModScreenHandlers.INDUCTION_FURNACE, InductionFurnaceScreen::new);
        HandledScreens.register(ModScreenHandlers.BATBOX, BatBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.CESU, CesuScreen::new);
        HandledScreens.register(ModScreenHandlers.MFE, MfeScreen::new);
        HandledScreens.register(ModScreenHandlers.MFSU, MfsuScreen::new);
        HandledScreens.register(ModScreenHandlers.CHARGEPAD_BATBOX, ChargepadBatBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.CHARGEPAD_CESU, ChargepadCesuScreen::new);
        HandledScreens.register(ModScreenHandlers.CHARGEPAD_MFE, ChargepadMfeScreen::new);
        HandledScreens.register(ModScreenHandlers.CHARGEPAD_MFSU, ChargepadMfsuScreen::new);
        HandledScreens.register(ModScreenHandlers.MACERATOR, MaceratorScreen::new);
        HandledScreens.register(ModScreenHandlers.COMPRESSOR, CompressorScreen::new);
        HandledScreens.register(ModScreenHandlers.EXTRACTOR, ExtractorScreen::new);
        HandledScreens.register(ModScreenHandlers.RECYCLER, RecyclerScreen::new);
        HandledScreens.register(ModScreenHandlers.METAL_FORMER, MetalFormerScreen::new);
        HandledScreens.register(ModScreenHandlers.SOLID_CANNER, SolidCannerScreen::new);
        HandledScreens.register(ModScreenHandlers.CANNER, CannerScreen::new);
        HandledScreens.register(ModScreenHandlers.FLUID_BOTTLER, FluidBottlerScreen::new);
        HandledScreens.register(ModScreenHandlers.THERMAL_CENTRIFUGE, ThermalCentrifugeScreen::new);
        HandledScreens.register(ModScreenHandlers.ORE_WASHING_PLANT, OreWashingPlantScreen::new);
        HandledScreens.register(ModScreenHandlers.NUCLEAR_REACTOR, NuclearReactorScreen::new);
        HandledScreens.register(ModScreenHandlers.LV_TRANSFORMER, LvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.MV_TRANSFORMER, MvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.HV_TRANSFORMER, HvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.EV_TRANSFORMER, EvTransformerScreen::new);
        HandledScreens.register(ModScreenHandlers.CONTAINMENT_BOX, ContainmentBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.TOOL_BOX, ToolboxScreen::new);
    }

    private static void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(ModParticles.CHARGEPAD, ChargepadParticle.Factory::new);
    }

    private static void registerRenderers() {
        BlockEntityRendererFactories.register(ModBlockEntities.CABLE, CableBlockEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MINING_LASER, MiningLaserEntityRenderer::new);
    }

    private static void registerFluidRenderers() {
        for (ModFluids.Ic2FluidEntry entry : ModFluids.entries()) {
            FluidRenderHandlerRegistry.INSTANCE.register(entry.still(), entry.flowing(),
                    new SimpleFluidRenderHandler(entry.stillTexture(), entry.flowingTexture(), entry.tintRgb()));
            BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), entry.still(), entry.flowing());
        }
    }

    private static void registerBlockRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_LEAVES, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_GLASS, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.SCAFFOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_SCAFFOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.IRON_SCAFFOLD, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_IRON_SCAFFOLD, RenderLayer.getCutout());
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

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            if (tintIndex != 1 || !UniversalFluidCellItem.isFilled(stack)) {
                return 0xFFFFFF;
            }
            return UniversalFluidCellItem.getFluidTintRgb(stack);
        }, ModItems.FLUID_CELL);

        // minecraft:block/water_still is a grayscale/tintable texture. Without an
        // item color provider the IC2-style flat water sheet renders gray in GUIs.
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? 0x3F76E4 : 0xFFFFFF,
                ModItems.WATER_SHEET);
    }

    private static void registerKeyBindings() {
        KeyBinding nightVisionToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.nightvision_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.industrial_legacy"
        ));

        KeyBinding jetpackHoverToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.flight_hover_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "key.categories.industrial_legacy"
        ));

        KeyBinding boostKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.boost",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_CONTROL,
                "key.categories.industrial_legacy"
        ));

        KeyBinding modeSwitchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.mode_switch",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
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

            while (modeSwitchKey.wasPressed()) {
                if (client.player == null || client.currentScreen != null) {
                    continue;
                }
                ClientPlayNetworking.send(ModPackets.CYCLE_HELD_ITEM_MODE,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }

            boolean hoverDown = jetpackHoverToggle.isPressed();
            if (hoverDown && !hoverWasDown[0]) {
                if (client.player == null) {
                    return;
                }
                ClientPlayNetworking.send(ModPackets.TOGGLE_CHEST_FLIGHT_HOVER,
                        net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
            hoverWasDown[0] = hoverDown;

            if (client.player != null) {
                boolean jump = client.options.jumpKey.isPressed();
                boolean sneak = client.options.sneakKey.isPressed();
                boolean forward = client.options.forwardKey.isPressed();
                boolean boost = boostKey.isPressed();

                var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                buf.writeBoolean(jump);
                buf.writeBoolean(sneak);
                buf.writeBoolean(forward);
                buf.writeBoolean(boost);
                ClientPlayNetworking.send(ModPackets.PLAYER_CONTROL_INPUT, buf);

                if (client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).getItem() instanceof IFlightChestItem) {
                    ChestFlightManager.tickClientPlayer(client.player, jump, sneak, forward);
                }
                if (client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS).getItem() instanceof QuantumLeggingsItem) {
                    QuantumLeggingsItem.tickClientPlayer(client.player, jump, forward, boost);
                }
                if (client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET).getItem() instanceof QuantumBootsItem) {
                    QuantumBootsItem.tickClientPlayer(client.player, jump, boost);
                }
            }
        });
    }
}
