package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.block.WaterKineticGeneratorBlock;
import com.shipovskijkorp.industriallegacy.block.entity.WaterKineticGeneratorBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Port of IL's KineticGeneratorRenderer + KineticGeneratorRotor model. */
@Environment(EnvType.CLIENT)
public final class WaterKineticGeneratorBlockEntityRenderer implements BlockEntityRenderer<WaterKineticGeneratorBlockEntity> {
    private static final float SCALE = 0.0625F;
    private static final float ROTOR_TRANSLATE_X = -0.2F;
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 256;

    public WaterKineticGeneratorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(WaterKineticGeneratorBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        int diameter = entity.getRotorDiameter();
        if (diameter == 0) {
            return;
        }

        BlockState state = entity.getCachedState();
        Direction facing = state.contains(WaterKineticGeneratorBlock.FACING)
                ? state.get(WaterKineticGeneratorBlock.FACING)
                : Direction.NORTH;
        Identifier texture = entity.getRotorRenderTexture();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));

        if (entity.getWorld() != null) {
            light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), entity.getPos().offset(facing));
        }

        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        rotateLikeLegacyFacing(matrices, facing);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getAngle()));
        matrices.translate(ROTOR_TRANSLATE_X, 0.0F, 0.0F);
        renderLegacyRotorModel(matrices, vertices, light, overlay, diameter);
        matrices.pop();
    }

    private static void rotateLikeLegacyFacing(MatrixStack matrices, Direction facing) {
        switch (facing) {
            case NORTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F));
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-270.0F));
            case UP -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
            case WEST, DOWN -> {
                // IL has no transform for these cases in KineticGeneratorRenderer.
            }
        }
    }

    private static void renderLegacyRotorModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 0.0F, -0.5F, 0.0F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 3.1F, 0.5F, 0.0F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 4.7F, 0.0F, 0.5F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 1.5F, 0.0F, -0.5F);
    }

    private static void renderLegacyBlade(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                       int radius, float rotateAngleX, float rotateAngleY, float rotateAngleZ) {
        matrices.push();
        matrices.translate(-8.0F * SCALE, 0.0F, 0.0F);
        if (rotateAngleZ != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotateAngleZ));
        if (rotateAngleY != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotateAngleY));
        if (rotateAngleX != 0.0F) matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotateAngleX));
        renderLegacyRotorBox(matrices, vertices, light, overlay, radius);
        matrices.pop();
    }

    /**
     * 1:1 UV port of Minecraft 1.12's ModelBox constructor for the IL call:
     * ModelRenderer(model, 0, 0).addBox(0, 0, -4, 1, radius * 8, 8).
     *
     * Important: IL sets ModelRenderer.mirror = true after addBox(), so vanilla 1.12 has
     * already built the ModelBox with mirror=false. The previous Fabric renderer mirrored the
     * already-created box, which made the rotor texture wrap slightly differently from IL.
     */
    private static void renderLegacyRotorBox(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
        final int textureOffsetX = 0;
        final int textureOffsetY = 0;
        final int sizeX = 1;
        final int sizeY = radius * 8;
        final int sizeZ = 8;

        float minX = 0.0F;
        float minY = 0.0F;
        float minZ = -4.0F;
        float maxX = minX + sizeX;
        float maxY = minY + sizeY;
        float maxZ = minZ + sizeZ;

        LegacyVertex vertex0 = legacyVertex(minX, minY, minZ);
        LegacyVertex vertex1 = legacyVertex(maxX, minY, minZ);
        LegacyVertex vertex2 = legacyVertex(maxX, maxY, minZ);
        LegacyVertex vertex3 = legacyVertex(minX, maxY, minZ);
        LegacyVertex vertex4 = legacyVertex(minX, minY, maxZ);
        LegacyVertex vertex5 = legacyVertex(maxX, minY, maxZ);
        LegacyVertex vertex6 = legacyVertex(maxX, maxY, maxZ);
        LegacyVertex vertex7 = legacyVertex(minX, maxY, maxZ);

        // net.minecraft.client.model.ModelBox quadList[0..5], with no mirror flip.
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex5, vertex1, vertex2, vertex6 },
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeZ, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex0, vertex4, vertex7, vertex3 },
                textureOffsetX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex5, vertex4, vertex0, vertex1 },
                textureOffsetX + sizeZ, textureOffsetY,
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex2, vertex3, vertex7, vertex6 },
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeX, textureOffsetY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex1, vertex0, vertex3, vertex2 },
                textureOffsetX + sizeZ, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new LegacyVertex[] { vertex4, vertex5, vertex6, vertex7 },
                textureOffsetX + sizeZ + sizeX + sizeZ, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeZ + sizeX, textureOffsetY + sizeZ + sizeY);
    }

    private static LegacyVertex legacyVertex(float x, float y, float z) {
        return new LegacyVertex(x * SCALE, y * SCALE, z * SCALE);
    }

    /** Equivalent to Minecraft 1.12 TexturedQuad(PositionTextureVertex[], u1, v1, u2, v2, 32, 256). */
    private static void renderTexturedQuad(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                           LegacyVertex[] quad, int u1, int v1, int u2, int v2) {
        RenderVertex[] renderVertices = new RenderVertex[] {
                new RenderVertex(quad[0], u2 / (float) TEXTURE_WIDTH, v1 / (float) TEXTURE_HEIGHT),
                new RenderVertex(quad[1], u1 / (float) TEXTURE_WIDTH, v1 / (float) TEXTURE_HEIGHT),
                new RenderVertex(quad[2], u1 / (float) TEXTURE_WIDTH, v2 / (float) TEXTURE_HEIGHT),
                new RenderVertex(quad[3], u2 / (float) TEXTURE_WIDTH, v2 / (float) TEXTURE_HEIGHT)
        };

        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();

        float ax = renderVertices[1].position.x - renderVertices[0].position.x;
        float ay = renderVertices[1].position.y - renderVertices[0].position.y;
        float az = renderVertices[1].position.z - renderVertices[0].position.z;
        float bx = renderVertices[2].position.x - renderVertices[1].position.x;
        float by = renderVertices[2].position.y - renderVertices[1].position.y;
        float bz = renderVertices[2].position.z - renderVertices[1].position.z;
        float normalX = ay * bz - az * by;
        float normalY = az * bx - ax * bz;
        float normalZ = ax * by - ay * bx;
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 1.0E-5F) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }

        for (RenderVertex renderVertex : renderVertices) {
            vertex(vertices, positionMatrix, normalMatrix,
                    renderVertex.position.x, renderVertex.position.y, renderVertex.position.z,
                    renderVertex.u, renderVertex.v,
                    normalX, normalY, normalZ, light, overlay);
        }
    }

    private static final class LegacyVertex {
        final float x;
        final float y;
        final float z;

        LegacyVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class RenderVertex {
        final LegacyVertex position;
        final float u;
        final float v;

        RenderVertex(LegacyVertex position, float u, float v) {
            this.position = position;
            this.u = u;
            this.v = v;
        }
    }

    private static void vertex(VertexConsumer vertices, Matrix4f positionMatrix, Matrix3f normalMatrix,
                               float x, float y, float z, float u, float v,
                               float normalX, float normalY, float normalZ, int light, int overlay) {
        vertices.vertex(positionMatrix, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(normalMatrix, normalX, normalY, normalZ)
                .next();
    }

    @Override
    public boolean rendersOutsideBoundingBox(WaterKineticGeneratorBlockEntity blockEntity) {
        return true;
    }
}
