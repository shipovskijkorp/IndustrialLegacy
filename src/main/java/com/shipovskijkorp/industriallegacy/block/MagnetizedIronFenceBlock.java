package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.block.entity.MagnetizerBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagnetizedIronFenceBlock extends FenceBlock {
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public MagnetizedIronFenceBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);
        return getDefaultState()
                .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER)
                .with(NORTH, canConnectTo(world, pos, Direction.NORTH, world.getBlockState(pos.north())))
                .with(EAST, canConnectTo(world, pos, Direction.EAST, world.getBlockState(pos.east())))
                .with(SOUTH, canConnectTo(world, pos, Direction.SOUTH, world.getBlockState(pos.south())))
                .with(WEST, canConnectTo(world, pos, Direction.WEST, world.getBlockState(pos.west())));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        if (direction.getAxis().isHorizontal()) {
            BooleanProperty property = switch (direction) {
                case NORTH -> NORTH;
                case EAST -> EAST;
                case SOUTH -> SOUTH;
                case WEST -> WEST;
                default -> null;
            };
            if (property != null) {
                return state.with(property, canConnectTo(world, pos, direction, neighborState));
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) return;

        boolean metalShoes = hasMetalShoes(player);
        if (!metalShoes) return;

        boolean powered = isPowered(world, pos);
        boolean descending = player.isSneaking();
        Vec3d velocity = player.getVelocity();

        // The original magnetizer clears fall distance while the player is on the pole.
        // Keep doing that unconditionally here so a player who briefly loses lift at the
        // top of a fence section does not take fall damage while being caught again.
        player.fallDistance = 0.0F;

        if (!powered) {
            if (descending && velocity.y < -0.25D) {
                player.setVelocity(velocity.x, velocity.y * 0.9D, velocity.z);
            }
            return;
        }

        if (descending) {
            if (velocity.y < -0.25D) {
                player.setVelocity(velocity.x, velocity.y * 0.8D, velocity.z);
            }
            return;
        }

        // Re-capture the player immediately when they fall back into the magnetic pole.
        // Without this clamp, a player can overshoot the upper fence tip, fall back at a
        // high negative velocity, and then wait several seconds while +0.075/t slowly
        // cancels the fall before the lift starts moving upward again.
        double y = Math.max(0.0D, velocity.y) + 0.075D;
        if (y > 0.0D) {
            y *= 1.03D;
        }
        player.setVelocity(velocity.x, Math.min(y, 1.5D), velocity.z);
    }

    private boolean isPowered(World world, BlockPos start) {
        List<MagnetizerBlockEntity> magnetizers = getMagnetizers(world, start, true);
        if (magnetizers.isEmpty()) return false;
        double multiplier = 1.0D / (double) magnetizers.size();
        for (MagnetizerBlockEntity magnetizer : magnetizers) {
            magnetizer.boost(multiplier);
        }
        return true;
    }

    private List<MagnetizerBlockEntity> getMagnetizers(BlockView world, BlockPos start, boolean checkPower) {
        List<MagnetizerBlockEntity> ret = findAroundPole(world, start, checkPower);
        if (!ret.isEmpty()) return ret;

        boolean scanDown = true;
        boolean scanUp = true;
        for (int dy = 1; dy <= MagnetizerBlockEntity.MAX_FENCE_RANGE; dy++) {
            boolean abort = false;

            if (scanDown) {
                SearchResult result = scanPoleSection(world, start.down(dy), checkPower);
                if (result.blocked()) scanDown = false;
                if (!result.magnetizers().isEmpty()) {
                    ret.addAll(result.magnetizers());
                    abort = true;
                }
            }

            if (scanUp) {
                SearchResult result = scanPoleSection(world, start.up(dy), checkPower);
                if (result.blocked()) scanUp = false;
                if (!result.magnetizers().isEmpty()) {
                    ret.addAll(result.magnetizers());
                    abort = true;
                }
            }

            if (abort || (!scanDown && !scanUp)) break;
        }
        return ret;
    }

    private SearchResult scanPoleSection(BlockView world, BlockPos center, boolean checkPower) {
        if (!isBoostableFence(world.getBlockState(center))) {
            return new SearchResult(Collections.emptyList(), true);
        }

        List<MagnetizerBlockEntity> found = new ArrayList<>();
        for (Direction direction : HORIZONTALS) {
            BlockPos neighbor = center.offset(direction);
            BlockState neighborState = world.getBlockState(neighbor);
            if (isFence(neighborState)) {
                return new SearchResult(Collections.emptyList(), true);
            }
            MagnetizerBlockEntity magnetizer = getMagnetizer(world, neighbor, direction, checkPower);
            if (magnetizer != null) {
                found.add(magnetizer);
            }
        }
        return new SearchResult(found, false);
    }

    private List<MagnetizerBlockEntity> findAroundPole(BlockView world, BlockPos center, boolean checkPower) {
        List<MagnetizerBlockEntity> ret = new ArrayList<>();
        for (Direction direction : HORIZONTALS) {
            BlockPos neighbor = center.offset(direction);
            if (isFence(world.getBlockState(neighbor))) {
                return Collections.emptyList();
            }
            MagnetizerBlockEntity magnetizer = getMagnetizer(world, neighbor, direction, checkPower);
            if (magnetizer != null) ret.add(magnetizer);
        }
        return ret;
    }

    private MagnetizerBlockEntity getMagnetizer(BlockView world, BlockPos pos, Direction side, boolean checkPower) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof MagnetizerBlockEntity magnetizer)) return null;
        if (!facesPole(magnetizer, side)) return null;
        if (checkPower && !magnetizer.canBoost()) return null;
        return magnetizer;
    }

    private boolean canConnectTo(BlockView world, BlockPos pos, Direction direction, BlockState neighborState) {
        if (direction.getAxis().isHorizontal() && getMagnetizer(world, pos.offset(direction), direction, false) != null) {
            return true;
        }
        return super.canConnect(neighborState,
                neighborState.isSideSolidFullSquare(world, pos.offset(direction), direction.getOpposite()),
                direction.getOpposite());
    }

    private boolean facesPole(MagnetizerBlockEntity magnetizer, Direction side) {
        return magnetizer.getFacing() == side.getOpposite();
    }

    private boolean isFence(BlockState state) {
        return state.getBlock() instanceof FenceBlock;
    }

    private boolean isBoostableFence(BlockState state) {
        return state.isOf(ModBlocks.IRON_FENCE);
    }

    public static boolean hasMetalShoes(PlayerEntity player) {
        return hasMetalShoesStack(player.getEquippedStack(EquipmentSlot.FEET));
    }

    public static boolean hasMetalShoesStack(ItemStack shoes) {
        if (shoes.isEmpty()) return false;
        Item item = shoes.getItem();
        return item == Items.IRON_BOOTS
                || item == Items.GOLDEN_BOOTS
                || item == Items.CHAINMAIL_BOOTS
                || item == Items.NETHERITE_BOOTS
                || item == ModItems.BRONZE_BOOTS
                || item == ModItems.NANO_BOOTS
                || item == ModItems.QUANTUM_BOOTS
                || item == ModItems.STATIC_BOOTS;
    }

    private record SearchResult(List<MagnetizerBlockEntity> magnetizers, boolean blocked) {}
}
