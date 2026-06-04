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

/**
 * Iron fence with the magnetizer lift behavior ported from IC2 experimental's
 * BlockIC2Fence. The magnetizer lookup intentionally follows the original
 * side-facing and vertical scan rules instead of vanilla fence adjacency rules.
 */
public class MagnetizedIronFenceBlock extends FenceBlock {
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private static final double ASCEND_ACCELERATION = 0.075D;
    private static final double ASCEND_ACCELERATION_MULTIPLIER = 1.03D;
    private static final double MAX_SPEED_WITH_METAL_SHOES = 1.5D;
    private static final double MAX_SPEED_WITHOUT_METAL_SHOES = 0.5D;
    private static final double MAX_SPEED_ALT_SLOWDOWN = 0.1D;
    private static final double DESCEND_DAMPING_POWERED = 0.8D;
    private static final double DESCEND_DAMPING_UNPOWERED_METAL = 0.9D;
    private static final double SLOW_MIN_Y = -0.25D;
    private static final double SLOW_MAX_Y = 1.6D;

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
                .with(NORTH, shouldConnect(world, pos, Direction.NORTH, world.getBlockState(pos.north())))
                .with(EAST, shouldConnect(world, pos, Direction.EAST, world.getBlockState(pos.east())))
                .with(SOUTH, shouldConnect(world, pos, Direction.SOUTH, world.getBlockState(pos.south())))
                .with(WEST, shouldConnect(world, pos, Direction.WEST, world.getBlockState(pos.west())));
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
                return state.with(property, shouldConnect(world, pos, direction, neighborState));
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public boolean canConnect(BlockState state, boolean sideSolidFullSquare, Direction direction) {
        // Vanilla FenceBlock would connect to many solid blocks. IC2's iron fence only
        // connects to fences, plus a side-facing magnetizer handled in shouldConnect().
        return isFence(state);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        boolean powered = tryUseMagnetizers(world, pos);
        boolean metalShoes = hasMetalShoes(player);
        boolean descending = player.isSneaking();
        Vec3d velocity = player.getVelocity();

        // IC2 treats the player as safely controlled while moving inside this vertical
        // speed band. CFR decompiled the condition as a tautological OR in some dumps;
        // the actual lift behavior is the bounded interval used here.
        boolean slow = velocity.y >= SLOW_MIN_Y && velocity.y < SLOW_MAX_Y;
        if (slow) {
            player.fallDistance = 0.0F;
        }

        if (!powered) {
            if (descending && !slow && metalShoes) {
                setVerticalVelocity(player, velocity, velocity.y * DESCEND_DAMPING_UNPOWERED_METAL);
            }
            return;
        }

        if (descending) {
            if (!slow) {
                setVerticalVelocity(player, velocity, velocity.y * DESCEND_DAMPING_POWERED);
            }
            return;
        }

        double y = velocity.y + ASCEND_ACCELERATION;
        if (y > 0.0D) {
            y *= ASCEND_ACCELERATION_MULTIPLIER;
        }

        double maxSpeed = isMagnetizerAltSlowdownActive(player)
                ? MAX_SPEED_ALT_SLOWDOWN
                : (metalShoes ? MAX_SPEED_WITH_METAL_SHOES : MAX_SPEED_WITHOUT_METAL_SHOES);
        setVerticalVelocity(player, velocity, Math.min(y, maxSpeed));
    }

    private static void setVerticalVelocity(PlayerEntity player, Vec3d oldVelocity, double newY) {
        player.setVelocity(oldVelocity.x, newY, oldVelocity.z);
        player.velocityModified = true;
    }

    private boolean tryUseMagnetizers(World world, BlockPos start) {
        List<MagnetizerBlockEntity> magnetizers = getMagnetizers(world, start, true);
        if (magnetizers.isEmpty()) {
            return false;
        }

        double multiplier = 1.0D / (double) magnetizers.size();
        for (MagnetizerBlockEntity magnetizer : magnetizers) {
            magnetizer.boost(multiplier);
        }
        return true;
    }

    private List<MagnetizerBlockEntity> getMagnetizers(BlockView world, BlockPos start, boolean checkPower) {
        ArrayList<MagnetizerBlockEntity> ret = new ArrayList<>();

        for (Direction facing : HORIZONTALS) {
            BlockPos neighbor = start.offset(facing);
            BlockState state = world.getBlockState(neighbor);
            if (isFence(state)) {
                return Collections.emptyList();
            }
            MagnetizerBlockEntity magnetizer = getMagnetizer(world, neighbor, facing, state, checkPower);
            if (magnetizer != null) {
                ret.add(magnetizer);
            }
        }

        if (!ret.isEmpty()) {
            return ret;
        }

        int minDir = 0;
        int maxDir = 2;
        for (int dy = 1; dy <= MagnetizerBlockEntity.MAX_FENCE_RANGE; ++dy) {
            boolean abort = false;

            directionLoop:
            for (int dir = minDir; dir < maxDir; ++dir) {
                int offset = dir * 2 - 1;
                BlockPos center = start.add(0, offset * dy, 0);
                BlockState centerState = world.getBlockState(center);

                if (!isBoostableFence(centerState)) {
                    if (dir == 0) {
                        minDir = 1;
                    } else {
                        maxDir = 1;
                    }
                    if (minDir != maxDir) {
                        break;
                    }
                    abort = true;
                    break;
                }

                int oldSize = ret.size();
                for (Direction facing : HORIZONTALS) {
                    BlockPos neighbor = center.offset(facing);
                    BlockState state = world.getBlockState(neighbor);

                    if (isFence(state)) {
                        if (dir == 0) {
                            minDir = 1;
                        } else {
                            maxDir = 1;
                        }
                        if (minDir == maxDir) {
                            abort = true;
                        }
                        while (ret.size() > oldSize) {
                            ret.remove(ret.size() - 1);
                        }
                        continue directionLoop;
                    }

                    MagnetizerBlockEntity magnetizer = getMagnetizer(world, neighbor, facing, state, checkPower);
                    if (magnetizer != null) {
                        abort = true;
                        ret.add(magnetizer);
                    }
                }
            }

            if (abort) {
                break;
            }
        }

        return ret;
    }

    private MagnetizerBlockEntity getMagnetizer(BlockView world, BlockPos pos, Direction side, BlockState state, boolean checkPower) {
        if (!state.isOf(ModBlocks.MAGNETIZER)) {
            return null;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof MagnetizerBlockEntity magnetizer)) {
            return null;
        }
        if (side != null && side.getOpposite() != magnetizer.getFacing()) {
            return null;
        }
        if (checkPower && !magnetizer.canBoost()) {
            return null;
        }
        return magnetizer;
    }

    private boolean shouldConnect(BlockView world, BlockPos pos, Direction direction, BlockState neighborState) {
        if (direction.getAxis().isHorizontal()) {
            if (isFence(neighborState)) {
                return true;
            }
            if (isPole(world, pos)) {
                BlockPos neighbor = pos.offset(direction);
                return getMagnetizer(world, neighbor, direction, neighborState, false) != null;
            }
        }
        return false;
    }

    private boolean isPole(BlockView world, BlockPos pos) {
        for (Direction facing : HORIZONTALS) {
            if (isFence(world.getBlockState(pos.offset(facing)))) {
                return false;
            }
        }
        return true;
    }

    private boolean isFence(BlockState state) {
        return state.getBlock() instanceof FenceBlock;
    }

    private boolean isBoostableFence(BlockState state) {
        return state.isOf(ModBlocks.IRON_FENCE);
    }

    private boolean isMagnetizerAltSlowdownActive(PlayerEntity player) {
        // IC2 checks its synced Alt-key state here. Industrial Legacy does not have
        // a synced Alt key for this mechanic yet, so the normal non-Alt path is used.
        return false;
    }

    public static boolean hasMetalShoes(PlayerEntity player) {
        return hasMetalShoesStack(player.getEquippedStack(EquipmentSlot.FEET));
    }

    public static boolean hasMetalShoesStack(ItemStack shoes) {
        if (shoes.isEmpty()) {
            return false;
        }
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
}
