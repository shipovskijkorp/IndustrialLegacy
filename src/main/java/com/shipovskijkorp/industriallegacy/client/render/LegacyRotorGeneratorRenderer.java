package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.block.entity.LegacyRotorProvider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class LegacyRotorGeneratorRenderer<T extends BlockEntity & LegacyRotorProvider> implements BlockEntityRenderer<T> {
    private static final float SCALE = 0.0625F;
    private static final float ROTOR_TRANSLATE_X = -0.2F;
    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 256;

    public LegacyRotorGeneratorRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        int diameter = entity.getRotorDiameter();
        if (diameter == 0) return;
        Direction facing = entity.getFacing();
        VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(entity.getRotorRenderTexture()));
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
            case WEST, DOWN -> {}
        }
    }

    private static void renderLegacyRotorModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 0.0F, -0.5F, 0.0F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 3.1F, 0.5F, 0.0F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 4.7F, 0.0F, 0.5F);
        renderLegacyBlade(matrices, vertices, light, overlay, radius, 1.5F, 0.0F, -0.5F);
    }

    private static void renderLegacyBlade(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius, float rotateAngleX, float rotateAngleY, float rotateAngleZ) {
        matrices.push();
        matrices.translate(-8.0F * SCALE, 0.0F, 0.0F);
        if (rotateAngleZ != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotation(rotateAngleZ));
        if (rotateAngleY != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotateAngleY));
        if (rotateAngleX != 0.0F) matrices.multiply(RotationAxis.POSITIVE_X.rotation(rotateAngleX));
        renderLegacyRotorBox(matrices, vertices, light, overlay, radius);
        matrices.pop();
    }

    private static void renderLegacyRotorBox(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int radius) {
        final int sizeX = 1;
        final int sizeY = radius * 8;
        final int sizeZ = 8;
        float minX = 0.0F, minY = 0.0F, minZ = -4.0F;
        float maxX = minX + sizeX, maxY = minY + sizeY, maxZ = minZ + sizeZ;
        LegacyVertex v0 = v(minX, minY, minZ), v1 = v(maxX, minY, minZ), v2 = v(maxX, maxY, minZ), v3 = v(minX, maxY, minZ);
        LegacyVertex v4 = v(minX, minY, maxZ), v5 = v(maxX, minY, maxZ), v6 = v(maxX, maxY, maxZ), v7 = v(minX, maxY, maxZ);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v5, v1, v2, v6}, sizeZ + sizeX, sizeZ, sizeZ + sizeX + sizeZ, sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v0, v4, v7, v3}, 0, sizeZ, sizeZ, sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v5, v4, v0, v1}, sizeZ, 0, sizeZ + sizeX, sizeZ);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v2, v3, v7, v6}, sizeZ + sizeX, sizeZ, sizeZ + sizeX + sizeX, 0);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v1, v0, v3, v2}, sizeZ, sizeZ, sizeZ + sizeX, sizeZ + sizeY);
        renderTexturedQuad(matrices, vertices, light, overlay, new LegacyVertex[]{v4, v5, v6, v7}, sizeZ + sizeX + sizeZ, sizeZ, sizeZ + sizeX + sizeZ + sizeX, sizeZ + sizeY);
    }

    private static LegacyVertex v(float x, float y, float z) { return new LegacyVertex(x * SCALE, y * SCALE, z * SCALE); }

    private static void renderTexturedQuad(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, LegacyVertex[] q, int u1, int v1, int u2, int v2) {
        RenderVertex[] rv = new RenderVertex[]{
                new RenderVertex(q[0], u2 / (float) TEXTURE_WIDTH, v1 / (float) TEXTURE_HEIGHT),
                new RenderVertex(q[1], u1 / (float) TEXTURE_WIDTH, v1 / (float) TEXTURE_HEIGHT),
                new RenderVertex(q[2], u1 / (float) TEXTURE_WIDTH, v2 / (float) TEXTURE_HEIGHT),
                new RenderVertex(q[3], u2 / (float) TEXTURE_WIDTH, v2 / (float) TEXTURE_HEIGHT)
        };
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f positionMatrix = entry.getPositionMatrix();
        Matrix3f normalMatrix = entry.getNormalMatrix();
        float ax = rv[1].p.x - rv[0].p.x, ay = rv[1].p.y - rv[0].p.y, az = rv[1].p.z - rv[0].p.z;
        float bx = rv[2].p.x - rv[1].p.x, by = rv[2].p.y - rv[1].p.y, bz = rv[2].p.z - rv[1].p.z;
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1.0E-5F) { nx /= len; ny /= len; nz /= len; }
        for (RenderVertex r : rv) {
            vertices.vertex(positionMatrix, r.p.x, r.p.y, r.p.z).color(255,255,255,255).texture(r.u, r.v).overlay(overlay).light(light).normal(normalMatrix, nx, ny, nz).next();
        }
    }

    private record LegacyVertex(float x, float y, float z) {}
    private record RenderVertex(LegacyVertex p, float u, float v) {}

    @Override public boolean rendersOutsideBoundingBox(T blockEntity) { return true; }
}
