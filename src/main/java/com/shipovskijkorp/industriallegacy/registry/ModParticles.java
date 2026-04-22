package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
    public static final DefaultParticleType CHARGEPAD = Registry.register(
            Registries.PARTICLE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "chargepad"),
            FabricParticleTypes.simple()
    );

    private ModParticles() {
    }

    public static void register() {
        IndustrialLegacy.LOGGER.info("Industrial Legacy particles registered");
    }
}
