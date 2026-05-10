package com.shipovskijkorp.industriallegacy.client.render;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Thin IL-style cable renderer (tube/rectangular prism).
 *
 * Fixes: correct face winding so outside faces are visible (no accidental backface culling),
 * and avoids internal Z-fighting by not rendering inner faces between center and arms.
 *
 * Textures: one sprite per cable variant, applied to each face like a normal cube.
 */
@Environment(EnvType.CLIENT)
public class CableBlockEntityRenderer implements BlockEntityRenderer<CableBlockEntity> {

    public CableBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(CableBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        World world = entity.getWorld();
        if (world == null) return;

        BlockState state = entity.getCachedState();
        Block block = state.getBlock();
        if (!(block instanceof CableBlock cable)) return;

        CableKind kind = cable.getKind();

        String texPath = CableItem.colorTexturePath(cable.getTexturePath(), entity.getColor());
        // Copper oxidation visual stages for COPPER + insulation=0.
        if (kind == CableKind.COPPER && cable.getInsulation() == 0) {
            int lvl = entity.getOxidationLevel();
            texPath = switch (lvl) {
                case 1 -> texPath + "_exposed";
                case 2 -> texPath + "_weathered";
                case 3 -> texPath + "_oxidized";
                default -> texPath;
            };
        }

        Identifier texId = new Identifier(IndustrialLegacy.MOD_ID, texPath);
        if ((kind == CableKind.DETECTOR || kind == CableKind.SPLITTER) && entity.isActive()) {
            texId = new Identifier(IndustrialLegacy.MOD_ID, cable.getTexturePath() + "_active");
        }

        Sprite sprite = MinecraftClient.getInstance()
                .getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
                .apply(texId);

        RenderLayer layer = (kind == CableKind.GLASS)
                ? RenderLayer.getTranslucent()
                : RenderLayer.getCutoutMipped();

        VertexConsumer vc = vertexConsumers.getBuffer(layer);

        BlockPos pos = entity.getPos();

        // Determine visual connections (IL-like: connect to cables and to EU storages that can insert/extract on that face).
        Set<Direction> conns = EnumSet.noneOf(Direction.class);
        for (Direction dir : Direction.values()) {
            if (CableBlock.connectsTo(world, pos, dir)) conns.add(dir);
        }

        float w = cable.getVisualWidth();
        float min = 0.5f - w / 2.0f;
        float max = 0.5f + w / 2.0f;

        matrices.push();

        // Center: render only faces that are NOT connected (to avoid internal faces).
        renderCuboidFaces(matrices, vc, sprite,
                min, min, min, max, max, max,
                facesNotConnected(conns),
                light);

        // Arms: for each connected direction, render the arm cuboid, but skip the face that touches the center.
        for (Direction dir : conns) {
            float x1 = min, y1 = min, z1 = min, x2 = max, y2 = max, z2 = max;
            switch (dir) {
                case NORTH -> { z1 = 0.0f; z2 = min; }
                case SOUTH -> { z1 = max; z2 = 1.0f; }
                case WEST  -> { x1 = 0.0f; x2 = min; }
                case EAST  -> { x1 = max; x2 = 1.0f; }
                case DOWN  -> { y1 = 0.0f; y2 = min; }
                case UP    -> { y1 = max; y2 = 1.0f; }
            }

            // render all faces except the inner one (opposite of direction)
            Set<Direction> armFaces = EnumSet.allOf(Direction.class);
            armFaces.remove(dir.getOpposite());
            renderCuboidFaces(matrices, vc, sprite, x1, y1, z1, x2, y2, z2, armFaces, light);
        }

        matrices.pop();
    }

    private static Set<Direction> facesNotConnected(Set<Direction> conns) {
        Set<Direction> s = EnumSet.allOf(Direction.class);
        // if connected in a direction, we do NOT render that outer face on the center cube
        s.removeAll(conns);
        return s;
    }

    /**
     * Render only selected faces of a cuboid in block local coords [0..1].
     * Face winding is CCW when viewed from the outside, so culling works correctly.
     */
    private static void renderCuboidFaces(MatrixStack matrices, VertexConsumer vc, Sprite sprite,
                                         float x1, float y1, float z1, float x2, float y2, float z2,
                                         Set<Direction> faces, int light) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f m = entry.getPositionMatrix();
        Matrix3f n = entry.getNormalMatrix();

        float uMin = sprite.getMinU();
        float uMax = sprite.getMaxU();
        float vMin = sprite.getMinV();
        float vMax = sprite.getMaxV();

        int ov = OverlayTexture.DEFAULT_UV;

        // Helper: map [0..1] -> sprite UV range
        java.util.function.DoubleUnaryOperator uMap = (t) -> (float)(uMin + (uMax - uMin) * t);
        java.util.function.DoubleUnaryOperator vMap = (t) -> (float)(vMin + (vMax - vMin) * t);

        if (faces.contains(Direction.NORTH)) {
            // -Z, UV: U=X, V=Y
            face(vc, m, n,
                    x1, y1, z1, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - y1),
                    x1, y2, z1, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - y2),
                    x2, y2, z1, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - y2),
                    x2, y1, z1, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - y1),
                    0, 0, -1, light, ov);
        }

        if (faces.contains(Direction.SOUTH)) {
            // +Z, UV: U=X, V=Y (flipped in X to match vanilla feel)
            face(vc, m, n,
                    x2, y1, z2, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - y1),
                    x2, y2, z2, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - y2),
                    x1, y2, z2, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - y2),
                    x1, y1, z2, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - y1),
                    0, 0, 1, light, ov);
        }

        if (faces.contains(Direction.WEST)) {
            // -X, UV: U=Z, V=Y
            face(vc, m, n,
                    x1, y1, z2, (float)uMap.applyAsDouble(z1), (float)vMap.applyAsDouble(1 - y1),
                    x1, y2, z2, (float)uMap.applyAsDouble(z1), (float)vMap.applyAsDouble(1 - y2),
                    x1, y2, z1, (float)uMap.applyAsDouble(z2), (float)vMap.applyAsDouble(1 - y2),
                    x1, y1, z1, (float)uMap.applyAsDouble(z2), (float)vMap.applyAsDouble(1 - y1),
                    -1, 0, 0, light, ov);
        }

        if (faces.contains(Direction.EAST)) {
            // +X, UV: U=Z, V=Y
            face(vc, m, n,
                    x2, y1, z1, (float)uMap.applyAsDouble(z1), (float)vMap.applyAsDouble(1 - y1),
                    x2, y2, z1, (float)uMap.applyAsDouble(z1), (float)vMap.applyAsDouble(1 - y2),
                    x2, y2, z2, (float)uMap.applyAsDouble(z2), (float)vMap.applyAsDouble(1 - y2),
                    x2, y1, z2, (float)uMap.applyAsDouble(z2), (float)vMap.applyAsDouble(1 - y1),
                    1, 0, 0, light, ov);
        }

        if (faces.contains(Direction.DOWN)) {
            // -Y, UV: U=X, V=Z
            face(vc, m, n,
                    x1, y1, z2, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - z2),
                    x1, y1, z1, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - z1),
                    x2, y1, z1, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - z1),
                    x2, y1, z2, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - z2),
                    0, -1, 0, light, ov);
        }

        if (faces.contains(Direction.UP)) {
            // +Y, UV: U=X, V=Z
            face(vc, m, n,
                    x1, y2, z1, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - z1),
                    x1, y2, z2, (float)uMap.applyAsDouble(x1), (float)vMap.applyAsDouble(1 - z2),
                    x2, y2, z2, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - z2),
                    x2, y2, z1, (float)uMap.applyAsDouble(x2), (float)vMap.applyAsDouble(1 - z1),
                    0, 1, 0, light, ov);
        }
    }

    /**
     * Emit a quad with CCW winding when viewed from outside (direction of normal).
     */
    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4,
                             float nx, float ny, float nz,
                             int light, int overlay) {

        vertex(vc, m, n, x1, y1, z1, u1, v1, nx, ny, nz, light, overlay);
        vertex(vc, m, n, x2, y2, z2, u2, v2, nx, ny, nz, light, overlay);
        vertex(vc, m, n, x3, y3, z3, u3, v3, nx, ny, nz, light, overlay);
        vertex(vc, m, n, x4, y4, z4, u4, v4, nx, ny, nz, light, overlay);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz,
                               int light, int overlay) {
        vc.vertex(m, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(overlay)
                .light(light)
                .normal(n, nx, ny, nz)
                .next();
    }
}
