package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.entity.projectile.MiningLaserEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** Entity registrations. */
public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<MiningLaserEntity> MINING_LASER = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "mining_laser"),
            FabricEntityTypeBuilder.<MiningLaserEntity>create(SpawnGroup.MISC, MiningLaserEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static void register() {
        // classload only
    }
}
