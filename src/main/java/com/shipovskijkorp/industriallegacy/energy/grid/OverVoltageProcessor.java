package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

/**
 * Applies IC2-like over-voltage side effects: conductor meltdown, insulation breakdown, entity shocks,
 * and sink explosions.
 */
final class OverVoltageProcessor {
    private OverVoltageProcessor() {}

    static void applyCableEffects(World world, List<BlockPos> cables, double packet) {
        if (!(world instanceof ServerWorld sw)) return;
        if (packet <= 0.0) return;

        boolean cableMeltdown = ILConfig.getBool("misc/enableEnetCableMeltdown", true);
        for (BlockPos p : cables) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof CableBlock cb)) continue;

            CableKind kind = cb.getKind();
            int insulation = cb.getInsulation();

            // Conductor breakdown (capacity + 1)
            if (packet >= kind.getConductorBreakdownEnergy()) {
                if (cableMeltdown) {
                    world.breakBlock(p, false);
                }
                continue;
            }

            // Insulation absorption: strip one layer if packet exceeds absorption.
            double absorb = kind.getInsulationEnergyAbsorption(insulation);
            if (insulation > 0 && packet >= absorb) {
                int newIns = Math.max(0, insulation - 1);
                BlockState ns = ModBlocks.getCableBlock(kind, newIns).getDefaultState();
                world.setBlockState(p, ns, 3);
            }

            // Shock entities near uninsulated cables when a meaningful packet is conducted.
            if (insulation <= 0 && packet >= 32.0) {
                shockEntities(sw, p, packet);
            }
        }
    }

    private static void shockEntities(ServerWorld world, BlockPos pos, double packet) {
        // Very small AABB around the cable block.
        Box box = new Box(pos).expand(0.15);
        float damage = (float) Math.min(20.0, packet / 64.0); // IC2-ish scaling (rough)
        if (damage <= 0.0f) return;

        var src = world.getDamageSources().lightningBolt();
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, ent -> ent.isAlive() && !ent.isSpectator())) {
            // IC2 doesn't shock in creative, keep it mild here.
            if (e instanceof PlayerEntity p && p.getAbilities().creativeMode) continue;
            e.damage(src, damage);
        }
    }

    static void explodeSink(World world, BlockPos sinkPos, double packet) {
        if (!(world instanceof ServerWorld)) return;
        boolean explosions = ILConfig.getBool("misc/enableEnetExplosions", true);
        if (!explosions) return;

        // Tier-ish scaling based on packet size.
        float strength;
        if (packet <= 32.0) strength = 2.0f;
        else if (packet <= 128.0) strength = 2.5f;
        else if (packet <= 512.0) strength = 3.5f;
        else if (packet <= 2048.0) strength = 4.5f;
        else if (packet <= 8192.0) strength = 6.0f;
        else strength = 8.0f;

        world.createExplosion(null, sinkPos.getX() + 0.5, sinkPos.getY() + 0.5, sinkPos.getZ() + 0.5, strength, World.ExplosionSourceType.BLOCK);
    }
}
