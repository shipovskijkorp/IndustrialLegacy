package com.shipovskijkorp.industriallegacy.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

/**
 * Direct 1.20 analogue of IL's EntityILFX used by chargepads:
 * - blue tint 0.2,0.2,1.0
 * - alpha 0.6
 * - gravity 0
 * - lifetime 60
 * - size scaled by rand * 0.6 + 0.5
 * - fixed upward drift of 0.02/tick
 */
@Environment(EnvType.CLIENT)
public class ChargepadParticle extends SpriteBillboardParticle {
    private final SpriteProvider spriteProvider;

    protected ChargepadParticle(ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, SpriteProvider spriteProvider) {
        super(world, x, y, z, 0.0, 0.0, 0.0);
        this.spriteProvider = spriteProvider;
        this.setSpriteForAge(spriteProvider);
        this.red = 0.2f;
        this.green = 0.2f;
        this.blue = 1.0f;
        this.alpha = 0.6f;
        this.gravityStrength = 0.0f;
        this.maxAge = 60;
        this.scale *= this.random.nextFloat() * 0.6f + 0.5f;
        this.velocityX = 0.0;
        this.velocityY = 0.0;
        this.velocityZ = 0.0;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }

        this.move(0.0, 0.02, 0.0);
        this.setSpriteForAge(this.spriteProvider);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<DefaultParticleType> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        @Override
        public Particle createParticle(DefaultParticleType parameters, ClientWorld world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
            return new ChargepadParticle(world, x, y, z, velocityX, velocityY, velocityZ, this.spriteProvider);
        }
    }
}
