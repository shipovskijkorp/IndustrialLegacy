package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.List;

/** IC2 construction foam block. Non-solid while wet; random-ticks into CF wall/reinforced stone. */
public class ConstructionFoamBlock extends Block {
    public static final EnumProperty<FoamType> TYPE = EnumProperty.of("type", FoamType.class);

    public ConstructionFoamBlock(Settings settings) {
        this(settings, FoamType.NORMAL);
    }

    public ConstructionFoamBlock(Settings settings, FoamType defaultType) {
        super(settings.ticksRandomly().nonOpaque());
        setDefaultState(getDefaultState().with(TYPE, defaultType));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return VoxelShapes.fullCube();
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int tickSpeed = world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
        if (tickSpeed <= 0) return;

        FoamType type = state.get(TYPE);
        float chance = getHardenChance(world, pos, state, type) * 4096.0f / (float) tickSpeed;
        if (random.nextFloat() < chance) {
            harden(world, pos, type);
        }
    }

    /** Mirrors IC2 BlockFoam#getHardenChance: 1 / (hardenTime * (16 - light) * 20). */
    public static float getHardenChance(World world, BlockPos pos, BlockState state, FoamType type) {
        int light = getNeighborAwareLight(world, pos, state);
        int lightPenalty = Math.max(1, 16 - light);
        int avgTimeTicks = type.hardenTime * lightPenalty * 20;
        return 1.0f / (float) avgTimeTicks;
    }

    private static int getNeighborAwareLight(World world, BlockPos pos, BlockState state) {
        int light = world.getLightLevel(pos);
        if (state.getOpacity(world, pos) == 0) {
            for (Direction direction : Direction.values()) {
                light = Math.max(light, world.getLightLevel(pos.offset(direction)));
            }
        }
        return Math.min(15, light);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(Items.SAND)) return ActionResult.PASS;
        if (!world.isClient) {
            if (!player.getAbilities().creativeMode) stack.decrement(1);
            harden(world, pos, state.get(TYPE));
        }
        return ActionResult.success(world.isClient);
    }

    private static void harden(World world, BlockPos pos, FoamType type) {
        if (type == FoamType.REINFORCED) {
            world.setBlockState(pos, ModBlocks.REINFORCED_STONE.getDefaultState(), Block.NOTIFY_ALL);
        } else {
            world.setBlockState(pos, ModBlocks.FOAM_CONCRETE.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        if (state.get(TYPE) == FoamType.REINFORCED) {
            return List.of(new ItemStack(ModBlocks.IRON_SCAFFOLD));
        }
        return List.of();
    }

    public enum FoamType implements StringIdentifiable {
        NORMAL("normal", 300),
        REINFORCED("reinforced", 600);

        private final String name;
        private final int hardenTime;

        FoamType(String name, int hardenTime) {
            this.name = name;
            this.hardenTime = hardenTime;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
