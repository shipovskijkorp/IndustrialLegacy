package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.config.ILConfig;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies IL-style over-voltage side effects: conductor meltdown, insulation breakdown,
 * entity shocks, and sink explosions.
 */
final class OverVoltageProcessor {
    private OverVoltageProcessor() {}

    static void applyCableEffects(World world, List<BlockPos> cables, double packet, Map<LivingEntity, Double> shockEnergyMap) {
        if (!(world instanceof ServerWorld sw)) return;
        if (packet <= 0.0) return;

        // IL gates the whole cable-effects pass behind misc/enableEnetCableMeltdown.
        if (!ILConfig.getBool("misc/enableEnetCableMeltdown", true)) return;

        IdentityHashMap<LivingEntity, Double> localShockEnergyMap = new IdentityHashMap<>();
        Set<BlockPos> cablesToRemove = new HashSet<>();
        Set<BlockPos> cablesToStrip = new HashSet<>();

        for (BlockPos p : cables) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof CableBlock cb)) continue;

            CableKind kind = cb.getKind();
            int insulation = cb.getInsulation();

            // IL: conductor breaks only when packet is strictly above capacity + 1.
            if (packet > kind.getConductorBreakdownEnergy()) {
                cablesToRemove.add(p);
            } else if (insulation > 0 && packet > kind.getInsulationBreakdownEnergy()) {
                // IL: insulation is stripped by insulation breakdown energy, not by shock absorption.
                cablesToStrip.add(p);
            }

            double absorb = kind.getInsulationEnergyAbsorption(insulation);
            if (packet > absorb) {
                int shockEnergy = (int) (packet - absorb);
                recordShockEnergy(sw, p, shockEnergy, localShockEnergyMap);
            }
        }

        cablesToStrip.removeAll(cablesToRemove);
        for (BlockPos p : cablesToRemove) {
            world.breakBlock(p, false);
        }
        for (BlockPos p : cablesToStrip) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof CableBlock cb) {
                stripOneInsulationLayer(world, p, cb.getKind(), cb.getInsulation());
            }
        }

        for (Map.Entry<LivingEntity, Double> entry : localShockEnergyMap.entrySet()) {
            shockEnergyMap.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }

    private static void stripOneInsulationLayer(World world, BlockPos pos, CableKind kind, int insulation) {
        int newIns = Math.max(0, insulation - 1);
        int oldColor = -1;
        if (world.getBlockEntity(pos) instanceof CableBlockEntity oldCableBe) {
            oldColor = oldCableBe.getColor();
        }

        BlockState ns = ModBlocks.getCableBlock(kind, newIns).getDefaultState();
        world.setBlockState(pos, ns, 3);
        if (world.getBlockEntity(pos) instanceof CableBlockEntity newCableBe) {
            newCableBe.setColor(kind.canBeColored(newIns) ? oldColor : -1);
            newCableBe.refreshDerivedState();
        }
    }

    private static void recordShockEnergy(ServerWorld world, BlockPos pos, int shockEnergy, Map<LivingEntity, Double> localShockEnergyMap) {
        if (shockEnergy <= 0) return;

        // IL checks a 3x3x3 area around each conducted cable block.
        Box box = new Box(pos).expand(1.0D);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            Double previous = localShockEnergyMap.get(entity);
            if (previous == null || previous < shockEnergy) {
                localShockEnergyMap.put(entity, (double) shockEnergy);
            }
        }
    }

    static void applyAccumulatedShockDamage(ServerWorld world, Map<LivingEntity, Double> shockEnergyMap) {
        if (shockEnergyMap.isEmpty()) return;

        var src = world.getDamageSources().lightningBolt();
        for (Map.Entry<LivingEntity, Double> entry : shockEnergyMap.entrySet()) {
            LivingEntity target = entry.getKey();
            int damage = (int) Math.ceil(entry.getValue() / 64.0D);
            if (target.isAlive() && damage > 0) {
                target.damage(src, (float) damage);
            }
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
