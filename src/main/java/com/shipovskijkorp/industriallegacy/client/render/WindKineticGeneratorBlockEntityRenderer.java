package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.block.WindKineticGeneratorBlock;
import com.shipovskijkorp.industriallegacy.block.entity.WindKineticGeneratorBlockEntity;
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

/** Port of IC2's KineticGeneratorRenderer + KineticGeneratorRotor model. */
@Environment(EnvType.CLIENT)
public final class WindKineticGeneratorBlockEntityRenderer implements BlockEntityRenderer<WindKineticGeneratorBlockEntity> {
    private static final float SCALE = 0.0625F;
    private static final float ROTOR_TRANSLATE_X = -0.2F;
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 256;

    public WindKineticGeneratorBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(WindKineticGeneratorBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        int diameter = entity.getRotorDiameter();
        if (diameter == 0) {
            return;
        }

        BlockState state = entity.getCachedState();
        Direction facing = state.contains(WindKineticGeneratorBlock.FACING)
                ? state.get(WindKineticGeneratorBlock.FACING)
                : Direction.NORTH;
        Identifier texture = entity.getRotorRenderTexture();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));

        if (entity.getWorld() != null) {
            light = WorldRenderer.getLightmapCoordinates(entity.getWorld(), entity.getPos().offset(facing));
        }

        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        rotateLikeIc2Facing(matrices, facing);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getAngle()));
        matrices.translate(ROTOR_TRANSLATE_X, 0.0F, 0.0F);
        renderIc2RotorModel(matrices, vertices, light, overlay, diameter);
        matrices.pop();
    }

    private static void rotateLikeIc2Facing(MatrixStack matrices, Direction facing) {
        switch (facing) {
            case NORTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
            case EAST -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F));
            case SOUTH -> matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-270.0F));
            case UP -> matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
            case WEST, DOWN -> {
                // IC2 has no transform for these cases in KineticGeneratorRenderer.
            }
        }
    }

    private static void renderIc2RotorModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
        renderIc2Blade(matrices, vertices, light, overlay, radius, 0.0F, -0.5F, 0.0F);
        renderIc2Blade(matrices, vertices, light, overlay, radius, 3.1F, 0.5F, 0.0F);
        renderIc2Blade(matrices, vertices, light, overlay, radius, 4.7F, 0.0F, 0.5F);
        renderIc2Blade(matrices, vertices, light, overlay, radius, 1.5F, 0.0F, -0.5F);
    }

    private static void renderIc2Blade(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                       int radius, float rotateAngleX, float rotateAngleY, float rotateAngleZ) {
        matrices.push();
        matrices.translate(-8.0F * SCALE, 0.0F, 0.0F);
        if (rotateAngleZ != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotateAngleZ));
        if (rotateAngleY != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotateAngleY));
        if (rotateAngleX != 0.0F) matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotateAngleX));
        renderIc2RotorBox(matrices, vertices, light, overlay, radius);
        matrices.pop();
    }

    /**
     * 1:1 UV port of Minecraft 1.12's ModelBox constructor for the IC2 call:
     * ModelRenderer(model, 0, 0).addBox(0, 0, -4, 1, radius * 8, 8).
     *
     * Important: IC2 sets ModelRenderer.mirror = true after addBox(), so vanilla 1.12 has
     * already built the ModelBox with mirror=false. The previous Fabric renderer mirrored the
     * already-created box, which made the rotor texture wrap slightly differently from IC2.
     */
    private static void renderIc2RotorBox(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
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

        Ic2Vertex vertex0 = ic2Vertex(minX, minY, minZ);
        Ic2Vertex vertex1 = ic2Vertex(maxX, minY, minZ);
        Ic2Vertex vertex2 = ic2Vertex(maxX, maxY, minZ);
        Ic2Vertex vertex3 = ic2Vertex(minX, maxY, minZ);
        Ic2Vertex vertex4 = ic2Vertex(minX, minY, maxZ);
        Ic2Vertex vertex5 = ic2Vertex(maxX, minY, maxZ);
        Ic2Vertex vertex6 = ic2Vertex(maxX, maxY, maxZ);
        Ic2Vertex vertex7 = ic2Vertex(minX, maxY, maxZ);

        // net.minecraft.client.model.ModelBox quadList[0..5], with no mirror flip.
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex5, vertex1, vertex2, vertex6 },
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeZ, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex0, vertex4, vertex7, vertex3 },
                textureOffsetX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex5, vertex4, vertex0, vertex1 },
                textureOffsetX + sizeZ, textureOffsetY,
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex2, vertex3, vertex7, vertex6 },
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeX, textureOffsetY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex1, vertex0, vertex3, vertex2 },
                textureOffsetX + sizeZ, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX, textureOffsetY + sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay,
                new Ic2Vertex[] { vertex4, vertex5, vertex6, vertex7 },
                textureOffsetX + sizeZ + sizeX + sizeZ, textureOffsetY + sizeZ,
                textureOffsetX + sizeZ + sizeX + sizeZ + sizeX, textureOffsetY + sizeZ + sizeY);
    }

    private static Ic2Vertex ic2Vertex(float x, float y, float z) {
        return new Ic2Vertex(x * SCALE, y * SCALE, z * SCALE);
    }

    /** Equivalent to Minecraft 1.12 TexturedQuad(PositionTextureVertex[], u1, v1, u2, v2, 32, 256). */
    private static void renderTexturedQuad(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                                           Ic2Vertex[] quad, int u1, int v1, int u2, int v2) {
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

    private static final class Ic2Vertex {
        final float x;
        final float y;
        final float z;

        Ic2Vertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final class RenderVertex {
        final Ic2Vertex position;
        final float u;
        final float v;

        RenderVertex(Ic2Vertex position, float u, float v) {
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
    public boolean rendersOutsideBoundingBox(WindKineticGeneratorBlockEntity blockEntity) {
        return true;
    }
}
