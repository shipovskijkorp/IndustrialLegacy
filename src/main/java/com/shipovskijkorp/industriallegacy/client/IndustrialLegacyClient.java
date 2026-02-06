package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.render.CableBlockEntityRenderer;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.color.world.FoliageColors;
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import com.shipovskijkorp.industriallegacy.net.ModPackets;


/**
 * Client-only init.
 */
public class IndustrialLegacyClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Variant selector for the single cable item.
        ModelPredicateProviderRegistry.register(
                ModItems.CABLE,
                new Identifier(IndustrialLegacy.MOD_ID, "variant"),
                (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) ->
                        (float) CableVariants.variantId(CableItem.getKind(stack), CableItem.getInsulation(stack))
        );

        // Charge indicator for RE-Battery (0..1) used by item model overrides.
        ModelPredicateProviderRegistry.register(
                ModItems.RE_BATTERY,
                new Identifier(IndustrialLegacy.MOD_ID, "charge"),
                (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) ->
                        ElectricItemManager.getChargeRatio(stack)
        );

        // Charge indicator for Advanced RE-Battery (0..1) used by item model overrides.
        ModelPredicateProviderRegistry.register(
                ModItems.ADVANCED_RE_BATTERY,
                new Identifier(IndustrialLegacy.MOD_ID, "charge"),
                (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) ->
                        ElectricItemManager.getChargeRatio(stack)
        );


        // Charge indicator for Energy Crystal (0..1) used by item model overrides.
        ModelPredicateProviderRegistry.register(
                ModItems.ENERGY_CRYSTAL,
                new Identifier(IndustrialLegacy.MOD_ID, "charge"),
                (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) ->
                        ElectricItemManager.getChargeRatio(stack)
        );


        // Phase3: thin-cable BlockEntity renderer.
        BlockEntityRendererFactories.register(ModBlockEntities.CABLE, CableBlockEntityRenderer::new);

        
        // Render layers for plants/leaves (cutout).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_LEAVES, RenderLayer.getCutoutMipped());

        // Reinforced glass (IC2-like): translucent.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REINFORCED_GLASS, RenderLayer.getTranslucent());

        // Luminator: translucent.
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.LUMINATOR, RenderLayer.getTranslucent());

        // Biome foliage tint for rubber leaves (otherwise they render gray).
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (world == null || pos == null) return FoliageColors.getDefaultColor();
            return BiomeColors.getFoliageColor(world, pos);
        }, ModBlocks.RUBBER_LEAVES);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> FoliageColors.getDefaultColor(),
                ModBlocks.RUBBER_LEAVES.asItem());

        
        // Night Vision toggle (global module key). Default: N
        KeyBinding nvToggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.industrial_legacy.nightvision_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "key.categories.industrial_legacy"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (nvToggle.wasPressed()) {
                if (client.player == null) return;
                ClientPlayNetworking.send(ModPackets.TOGGLE_NIGHTVISION, net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());
            }
        });

        IndustrialLegacy.LOGGER.info("Industrial Legacy client initialized");
    }
}