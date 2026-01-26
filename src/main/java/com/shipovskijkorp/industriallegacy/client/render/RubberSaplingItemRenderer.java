package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Rubber sapling item renderer.
 *
 * <p>Goal:</p>
 * <ul>
 *   <li><b>GUI/inventory</b> -> flat 2D sprite</li>
 *   <li><b>dropped on ground / in-world item entity</b> -> 3D block model (cross), like the placed sapling block</li>
 * </ul>
 *
 * <p>This requires the item model to be <code>builtin/entity</code> and using a dynamic renderer.</p>
 */
@Environment(EnvType.CLIENT)
public final class RubberSaplingItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final Identifier GUI_MODEL_ID = new Identifier(IndustrialLegacy.MOD_ID, "rubber_sapling_gui");
    private static final ModelIdentifier GUI_MODEL = new ModelIdentifier(GUI_MODEL_ID, "inventory");

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        MinecraftClient mc = MinecraftClient.getInstance();

        // GUI: render a separate baked 2D model (minecraft:item/generated).
        if (mode == ModelTransformationMode.GUI) {
            ItemRenderer ir = mc.getItemRenderer();
            BakedModel guiModel = mc.getBakedModelManager().getModel(GUI_MODEL);
            ir.renderItem(stack, mode, false, matrices, vertexConsumers, light, overlay, guiModel);
            return;
        }

        // Everything else (GROUND/FIXED/first-person/third-person): render the block model as entity.
        BlockState state = ModBlocks.RUBBER_SAPLING.getDefaultState();

        matrices.push();
        // renderBlockAsEntity expects block-space coords.
        matrices.translate(-0.5, -0.5, -0.5);
        mc.getBlockRenderManager().renderBlockAsEntity(state, matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }
}
