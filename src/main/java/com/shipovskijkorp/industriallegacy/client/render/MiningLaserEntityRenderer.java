package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.entity.projectile.MiningLaserEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Modern equivalent of IL's old RenderCrossed laser renderer.
 */
@Environment(EnvType.CLIENT)
public final class MiningLaserEntityRenderer extends EntityRenderer<MiningLaserEntity> {
    private static final Identifier TEXTURE = new Identifier(IndustrialLegacy.MOD_ID, "textures/models/laser.png");

    public MiningLaserEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MiningLaserEntity entity, float entityYaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        if (entity.prevYaw == 0.0f && entity.prevPitch == 0.0f && entity.age < 1) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0f));
        matrices.scale(0.05625f, 0.05625f, 0.05625f);
        matrices.translate(-4.0f, 0.0f, 0.0f);

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));

        // Back cap (same atlas slice as the old IL renderer).
        quad(matrices, vc,
                -7.0f, -2.0f, -2.0f,
                -7.0f, -2.0f,  2.0f,
                -7.0f,  2.0f,  2.0f,
                -7.0f,  2.0f, -2.0f,
                0.0f, 0.15625f, 0.15625f, 0.3125f,
                light, 1.0f, 0.0f, 0.0f);
        quad(matrices, vc,
                -7.0f,  2.0f, -2.0f,
                -7.0f,  2.0f,  2.0f,
                -7.0f, -2.0f,  2.0f,
                -7.0f, -2.0f, -2.0f,
                0.0f, 0.15625f, 0.15625f, 0.3125f,
                light, -1.0f, 0.0f, 0.0f);

        for (int i = 0; i < 4; i++) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
            quad(matrices, vc,
                    -8.0f, -2.0f, 0.0f,
                     8.0f, -2.0f, 0.0f,
                     8.0f,  2.0f, 0.0f,
                    -8.0f,  2.0f, 0.0f,
                    0.0f, 0.5f, 0.0f, 0.15625f,
                    light, 0.0f, 0.0f, 1.0f);
        }

        matrices.pop();
        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void quad(MatrixStack matrices, VertexConsumer vc,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float uMin, float uMax, float vMin, float vMax,
                             int light, float nx, float ny, float nz) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        vertex(vc, pos, normal, x1, y1, z1, uMin, vMin, light, nx, ny, nz);
        vertex(vc, pos, normal, x2, y2, z2, uMax, vMin, light, nx, ny, nz);
        vertex(vc, pos, normal, x3, y3, z3, uMax, vMax, light, nx, ny, nz);
        vertex(vc, pos, normal, x4, y4, z4, uMin, vMax, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pos, Matrix3f normal,
                               float x, float y, float z, float u, float v,
                               int light, float nx, float ny, float nz) {
        vc.vertex(pos, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, nx, ny, nz)
                .next();
    }

    @Override
    public Identifier getTexture(MiningLaserEntity entity) {
        return TEXTURE;
    }
}
