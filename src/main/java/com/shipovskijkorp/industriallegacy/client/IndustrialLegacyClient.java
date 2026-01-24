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

        IndustrialLegacy.LOGGER.info("Industrial Legacy client initialized");
    }
}
