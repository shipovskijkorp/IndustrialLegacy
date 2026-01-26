package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.render.CableBlockEntityRenderer;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
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

        // Phase3: thin-cable BlockEntity renderer.
        BlockEntityRendererFactories.register(ModBlockEntities.CABLE, CableBlockEntityRenderer::new);

        
        // Render layers for plants/leaves (cutout).
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_SAPLING, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.RUBBER_LEAVES, RenderLayer.getCutoutMipped());

        // Biome foliage tint for rubber leaves (otherwise they render gray).
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) -> {
            if (world == null || pos == null) return FoliageColors.getDefaultColor();
            return BiomeColors.getFoliageColor(world, pos);
        }, ModBlocks.RUBBER_LEAVES);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> FoliageColors.getDefaultColor(),
                ModBlocks.RUBBER_LEAVES.asItem());

        IndustrialLegacy.LOGGER.info("Industrial Legacy client initialized");
    }
}