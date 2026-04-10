package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.Optional;

/**
 * Minimal non-placeable air fluid registration.
 *
 * This mirrors IC2's concept of air as a fluid without trying to make it a
 * world-placeable liquid. It mainly exists so capsules and future machines can
 * reference a dedicated air fluid identity.
 */
public final class ModFluids {
    public static final Identifier AIR_ID = new Identifier(IndustrialLegacy.MOD_ID, "air");
    public static final FlowableFluid AIR = Registry.register(Registries.FLUID, AIR_ID, new Still());
    public static final FlowableFluid FLOWING_AIR = Registry.register(Registries.FLUID, new Identifier(IndustrialLegacy.MOD_ID, "flowing_air"), new Flowing());

    private ModFluids() {}

    public static void register() {
    }

    private abstract static class BaseAirFluid extends FlowableFluid {
        @Override
        public Fluid getStill() {
            return AIR;
        }

        @Override
        public Fluid getFlowing() {
            return FLOWING_AIR;
        }

        @Override
        public Item getBucketItem() {
            return Items.AIR;
        }

        @Override
        protected boolean isInfinite(World world) {
            return false;
        }

        @Override
        protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        }

        @Override
        protected int getFlowSpeed(WorldView world) {
            return 0;
        }

        @Override
        protected int getLevelDecreasePerBlock(WorldView world) {
            return 0;
        }

        @Override
        public int getTickRate(WorldView world) {
            return 5;
        }

        @Override
        protected float getBlastResistance() {
            return 0.0f;
        }

        @Override
        protected BlockState toBlockState(FluidState state) {
            return Blocks.AIR.getDefaultState();
        }

        @Override
        public boolean matchesType(Fluid fluid) {
            return fluid == AIR || fluid == FLOWING_AIR;
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        public Optional<SoundEvent> getBucketFillSound() {
            return Optional.empty();
        }
    }

    public static final class Flowing extends BaseAirFluid {
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }

        @Override
        public boolean isStill(FluidState state) {
            return false;
        }
    }

    public static final class Still extends BaseAirFluid {
        @Override
        public int getLevel(FluidState state) {
            return 8;
        }

        @Override
        public boolean isStill(FluidState state) {
            return true;
        }
    }
}
