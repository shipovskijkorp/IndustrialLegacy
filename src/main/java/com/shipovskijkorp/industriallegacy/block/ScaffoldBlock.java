package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * IL Experimental scaffold behavior port.
 *
 * <p>IL stores these as one metadata block. IL keeps separate registry ids, but the
 * support, reinforcement, drops and climbing behavior are intentionally shared.</p>
 */
public class ScaffoldBlock extends Block {
    private static final Direction[] SUPPORTED_FACINGS = new Direction[] {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    private static final VoxelShape COLLISION_SHAPE = Block.createCuboidShape(0.5, 0.0, 0.5, 15.5, 16.0, 15.5);
    private static final VoxelShape OUTLINE_SHAPE = VoxelShapes.fullCube();

    private final ScaffoldType scaffoldType;

    public ScaffoldBlock(Settings settings, ScaffoldType scaffoldType) {
        super(settings.ticksRandomly().nonOpaque());
        this.scaffoldType = scaffoldType;
    }

    public ScaffoldType getScaffoldType() {
        return scaffoldType;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return COLLISION_SHAPE;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        // IL's metadata block checked placement support using the weakest scaffold type.
        return hasSupport(world, pos, ScaffoldType.WOOD);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (player.isSneaking()) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.isEmpty()) {
            return ActionResult.PASS;
        }

        BlockState reinforcedState = null;
        int consume = 0;

        if (scaffoldType == ScaffoldType.WOOD) {
            if (!stack.isOf(Items.STICK) || stack.getCount() < 2) {
                return ActionResult.PASS;
            }
            reinforcedState = ModBlocks.REINFORCED_SCAFFOLD.getDefaultState();
            consume = 2;
        } else if (scaffoldType == ScaffoldType.IRON) {
            if (!stack.isOf(ModBlocks.IRON_FENCE.asItem()) || stack.getCount() < 1) {
                return ActionResult.PASS;
            }
            reinforcedState = ModBlocks.REINFORCED_IRON_SCAFFOLD.getDefaultState();
            consume = 1;
        } else {
            return ActionResult.PASS;
        }

        if (!isPillar(world, pos)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            world.setBlockState(pos, reinforcedState, Block.NOTIFY_ALL);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(consume);
            }
        }

        return ActionResult.success(world.isClient);
    }

    @Override
    public void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (world.isClient) {
            return;
        }

        ItemStack stack = player.getMainHandStack();
        if (!(stack.getItem() instanceof BlockItem blockItem) || !(blockItem.getBlock() instanceof ScaffoldBlock scaffoldBlock)) {
            return;
        }

        BlockPos placePos = pos;
        while (world.getBlockState(placePos).getBlock() instanceof ScaffoldBlock) {
            placePos = placePos.up();
        }

        if (placePos.getY() >= world.getTopY()) {
            return;
        }

        placeScaffold(world, placePos, player, stack, scaffoldBlock.getDefaultState());
    }

    private static void placeScaffold(World world, BlockPos pos, PlayerEntity player, ItemStack stack, BlockState placeState) {
        if (!world.getBlockState(pos).isAir()) {
            return;
        }
        if (!placeState.canPlaceAt(world, pos)) {
            return;
        }
        if (!world.setBlockState(pos, placeState, Block.NOTIFY_ALL)) {
            return;
        }

        BlockSoundGroup sound = placeState.getSoundGroup();
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);

        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity rawEntity) {
        if (!(rawEntity instanceof LivingEntity entity)) {
            return;
        }

        entity.fallDistance = 0.0f;
        Vec3d velocity = entity.getVelocity();
        double x = MathHelper.clamp(velocity.x, -0.15, 0.15);
        double y;
        double z = MathHelper.clamp(velocity.z, -0.15, 0.15);

        if (entity.isSneaking() && entity instanceof PlayerEntity) {
            y = entity.isTouchingWater() ? 0.02 : 0.08;
        } else if (entity.horizontalCollision) {
            y = 0.2;
        } else {
            y = Math.max(velocity.y, -0.07);
        }

        entity.setVelocity(x, y, z);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        checkSupport(world, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(8) == 0) {
            checkSupport(world, pos);
        }
    }

    private boolean isPillar(World world, BlockPos pos) {
        while (world.getBlockState(pos).getBlock() instanceof ScaffoldBlock) {
            pos = pos.down();
        }
        return isGroundSupport(world, pos);
    }

    private static boolean hasSupport(WorldView world, BlockPos start, ScaffoldType type) {
        Support support = calculateSupport(world, start, type).get(start);
        return support != null && support.strength >= 0;
    }

    private static void checkSupport(World world, BlockPos start) {
        BlockState startState = world.getBlockState(start);
        if (!(startState.getBlock() instanceof ScaffoldBlock scaffold)) {
            return;
        }

        Map<BlockPos, Support> results = calculateSupport(world, start, scaffold.getScaffoldType());
        boolean droppedAny = false;

        for (Support support : results.values()) {
            if (support.strength >= 0) {
                continue;
            }

            if (world.getBlockState(support.pos).getBlock() instanceof ScaffoldBlock) {
                world.breakBlock(support.pos, true);
                droppedAny = true;
            }
        }

        if (droppedAny) {
            for (Support support : results.values()) {
                if (support.strength < 0) {
                    world.updateNeighbors(support.pos, Blocks.AIR);
                }
            }
        }
    }

    private static Map<BlockPos, Support> calculateSupport(WorldView world, BlockPos start, ScaffoldType startType) {
        Map<BlockPos, Support> results = new HashMap<>();
        Queue<Support> queue = new ArrayDeque<>();
        Set<BlockPos> groundSupports = new HashSet<>();

        Support support = new Support(start, startType, -1);
        results.put(start, support);
        queue.add(support);

        while ((support = queue.poll()) != null) {
            for (Direction dir : Direction.values()) {
                BlockPos pos = support.pos.offset(dir);
                if (results.containsKey(pos)) {
                    continue;
                }

                BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof ScaffoldBlock scaffold) {
                    Support connected = new Support(pos, scaffold.getScaffoldType(), -1);
                    results.put(pos, connected);
                    queue.add(connected);
                } else if (isGroundSupport(world, pos)) {
                    groundSupports.add(pos);
                }
            }
        }

        queue.clear();

        for (BlockPos groundPos : groundSupports) {
            BlockPos pos = groundPos.up();
            int propagatedStrength = 0;

            while (true) {
                support = results.get(pos);
                if (support == null) {
                    break;
                }

                int strength;
                if (support.type.strength >= propagatedStrength) {
                    strength = support.type.strength;
                    propagatedStrength = strength - 1;
                } else {
                    strength = propagatedStrength;
                    propagatedStrength--;
                }

                if (support.strength < strength) {
                    support.strength = strength;
                    for (Direction dir : Direction.Type.HORIZONTAL) {
                        Support neighbor = results.get(pos.offset(dir));
                        if (neighbor != null && neighbor.strength < strength) {
                            neighbor.strength = strength - 1;
                            queue.add(neighbor);
                        }
                    }
                }

                pos = pos.up();
            }
        }

        while ((support = queue.poll()) != null) {
            for (Direction dir : SUPPORTED_FACINGS) {
                Support neighbor = results.get(support.pos.offset(dir));
                if (neighbor != null && neighbor.strength < support.strength) {
                    neighbor.strength = support.strength - 1;
                    if (neighbor.strength > 0) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return results;
    }

    private static boolean isGroundSupport(WorldView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isSideSolidFullSquare(world, pos, Direction.UP);
    }

    private static final class Support {
        final BlockPos pos;
        final ScaffoldType type;
        int strength;

        Support(BlockPos pos, ScaffoldType type, int strength) {
            this.pos = pos;
            this.type = type;
            this.strength = strength;
        }
    }

    public enum ScaffoldType {
        WOOD(2),
        REINFORCED_WOOD(5),
        IRON(5),
        REINFORCED_IRON(12);

        final int strength;

        ScaffoldType(int strength) {
            this.strength = strength;
        }
    }
}
