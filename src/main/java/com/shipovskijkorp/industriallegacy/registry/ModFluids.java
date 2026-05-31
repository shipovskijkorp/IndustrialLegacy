package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.jetbrains.annotations.Nullable;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IL fluid registrations.
 *
 * <p>classic industrial tech 2.8 registers these fluids in BlocksItems.initFluids():
 * uu_matter, construction_foam, coolant, creosote, hot_coolant, pahoehoe_lava, biomass,
 * biogas, distilled_water, superheated_steam, steam, hot_water, weed_ex, air,
 * hydrogen, oxygen, heavy_water and milk. Deuterium exists in the enum, but IL does
 * not register it in this version, so it is intentionally not registered here.</p>
 */
public final class ModFluids {
    public static final Identifier AIR_ID = id("air");

    private static final Map<Identifier, LegacyFluidEntry> ENTRIES_BY_ID = new LinkedHashMap<>();
    private static final List<LegacyFluidEntry> ENTRIES = new ArrayList<>();

    public static final LegacyFluidEntry UU_MATTER = registerLegacy("uu_matter", 0xFF3B0533, 3000, 3000, 0, 300, false, true, false);
    public static final LegacyFluidEntry CONSTRUCTION_FOAM = registerLegacy("construction_foam", 0xFF202020, 10000, 50000, 0, 300, false, true, false);
    public static final LegacyFluidEntry COOLANT = registerLegacy("coolant", 0xFF145A6A, 1000, 3000, 0, 300, false, true, false);
    public static final LegacyFluidEntry CREOSOTE = registerLegacy("creosote", 0xFF3D390A, 10000, 50000, 0, 300, false, true, false);
    public static final LegacyFluidEntry HOT_COOLANT = registerLegacy("hot_coolant", 0xFFB52834, 1000, 3000, 0, 1200, false, true, false);
    public static final LegacyFluidEntry PAHOEHOE_LAVA = registerLegacy("pahoehoe_lava", 0xFF7B746C, 50000, 250000, 10, 1200, false, false, false);
    public static final LegacyFluidEntry BIOMASS = registerLegacy("biomass", 0xFF376F25, 1000, 3000, 0, 300, false, true, false);
    public static final LegacyFluidEntry BIOGAS = registerLegacy("biogas", 0xFFA7984C, 1000, 3000, 0, 300, true, false, true);
    public static final LegacyFluidEntry DISTILLED_WATER = registerLegacy("distilled_water", 0xFF4356F5, 1000, 1000, 0, 300, false, true, false);
    public static final LegacyFluidEntry SUPERHEATED_STEAM = registerLegacy("superheated_steam", 0xFFCAD1D1, -3000, 100, 0, 600, true, false, false);
    public static final LegacyFluidEntry STEAM = registerLegacy("steam", 0xFFBCBCBC, -800, 300, 0, 420, true, false, false);
    public static final LegacyFluidEntry HOT_WATER = registerLegacy("hot_water", 0xFF46DEFF, 1000, 1000, 0, 350, false, true, false);
    public static final LegacyFluidEntry WEED_EX = registerLegacy("weed_ex", 0xFF074F14, 1000, 1000, 0, 300, false, false, false);
    public static final LegacyFluidEntry AIR = registerLegacy("air", 0xFFDCDCDC, 0, 500, 0, 300, true, false, true);
    public static final LegacyFluidEntry HYDROGEN = registerLegacy("hydrogen", 0xFFDCDCDC, 0, 500, 0, 300, true, false, false);
    public static final LegacyFluidEntry OXYGEN = registerLegacy("oxygen", 0xFFDCDCDC, 0, 500, 0, 300, true, false, false);
    public static final LegacyFluidEntry HEAVY_WATER = registerLegacy("heavy_water", 0xFF4356F5, 1000, 1000, 0, 300, false, true, false);
    public static final LegacyFluidEntry MILK = registerLegacy("milk", 0xFFFCFCFC, 1050, 1000, 0, 300, false, true, false);

    private ModFluids() {}

    public static void register() {
        // classload triggers static registration
    }

    public static Collection<LegacyFluidEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static LegacyFluidEntry getEntry(String rawId) {
        Identifier identifier = normalizeIdentifier(rawId);
        return identifier == null ? null : ENTRIES_BY_ID.get(identifier);
    }

    public static Block getFluidBlock(String rawId) {
        LegacyFluidEntry entry = getEntry(rawId);
        return entry == null ? null : entry.block();
    }

    public static int getFluidColor(String rawId, int fallbackArgb) {
        LegacyFluidEntry entry = getEntry(rawId);
        return entry == null ? fallbackArgb : entry.tintArgb();
    }

    public static Identifier normalizeIdentifier(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        String token = rawId.trim();
        String modPrefix = IndustrialLegacy.MOD_ID + ":";
        String legacyNamespace = legacyNamespace();
        String legacyPrefix = legacyNamespace + ":";

        if (token.startsWith(modPrefix)) {
            // already normalized
        } else if (token.startsWith(legacyPrefix)) {
            token = modPrefix + token.substring(legacyPrefix.length());
        } else if (token.startsWith(legacyNamespace) && token.length() > legacyNamespace.length() && token.indexOf(':') < 0) {
            token = modPrefix + token.substring(legacyNamespace.length());
        } else if (token.indexOf(':') < 0) {
            token = modPrefix + token;
        }
        return Identifier.tryParse(token);
    }

    private static String legacyNamespace() {
        return new String(new char[] { 'i', 'c', '2' });
    }

    private static LegacyFluidEntry registerLegacy(String path, int tintArgb, int density, int viscosity, int luminosity, int temperature, boolean gaseous, boolean hasFlowTexture, boolean vanishOnBlockItemPlacement) {
        Identifier identifier = id(path);
        LegacyFluidEntry entry = new LegacyFluidEntry(identifier, tintArgb, density, viscosity, luminosity, temperature, gaseous, hasFlowTexture, vanishOnBlockItemPlacement);

        entry.still = Registry.register(Registries.FLUID, identifier, new Still(entry));
        entry.flowing = Registry.register(Registries.FLUID, id("flowing_" + path), new Flowing(entry));

        entry.block = Registry.register(Registries.BLOCK, identifier, new IndustrialFluidBlock(entry.still, entry, FabricBlockSettings.create()
                .liquid()
                .replaceable()
                .noCollision()
                .strength(100.0f)
                .dropsNothing()
                .luminance(state -> entry.luminosity())));

        entry.item = Registry.register(Registries.ITEM, identifier, new BlockItem(entry.block, new FabricItemSettings()));

        ENTRIES_BY_ID.put(identifier, entry);
        ENTRIES.add(entry);
        return entry;
    }

    private static Identifier id(String path) {
        return new Identifier(IndustrialLegacy.MOD_ID, path);
    }

    public static final class LegacyFluidEntry {
        private final Identifier id;
        private final int tintArgb;
        private final int density;
        private final int viscosity;
        private final int luminosity;
        private final int temperature;
        private final boolean gaseous;
        private final boolean hasFlowTexture;
        private final boolean vanishOnBlockItemPlacement;
        private FlowableFluid still;
        private FlowableFluid flowing;
        private FluidBlock block;
        private Item item;

        private LegacyFluidEntry(Identifier id, int tintArgb, int density, int viscosity, int luminosity, int temperature, boolean gaseous, boolean hasFlowTexture, boolean vanishOnBlockItemPlacement) {
            this.id = id;
            this.tintArgb = tintArgb;
            this.density = density;
            this.viscosity = viscosity;
            this.luminosity = luminosity;
            this.temperature = temperature;
            this.gaseous = gaseous;
            this.hasFlowTexture = hasFlowTexture;
            this.vanishOnBlockItemPlacement = vanishOnBlockItemPlacement;
        }

        public Identifier id() { return id; }
        public String path() { return id.getPath(); }
        public int tintArgb() { return tintArgb; }
        public int tintRgb() { return tintArgb & 0x00FFFFFF; }
        public int density() { return density; }
        public int viscosity() { return viscosity; }
        public int luminosity() { return luminosity; }
        public int temperature() { return temperature; }
        public boolean gaseous() { return gaseous; }
        public boolean hasFlowTexture() { return hasFlowTexture; }
        public boolean rises() { return density <= 0; }
        public boolean vanishOnBlockItemPlacement() { return vanishOnBlockItemPlacement; }
        public FlowableFluid still() { return still; }
        public FlowableFluid flowing() { return flowing; }
        public FluidBlock block() { return block; }
        public Item item() { return item; }
        public Identifier stillTexture() { return ModFluids.id("block/fluid/" + path() + "_still"); }
        public Identifier flowingTexture() { return ModFluids.id("block/fluid/" + path() + (hasFlowTexture ? "_flow" : "_still")); }
        public int blockTickRate() { return Math.max(1, viscosity / 200); }
    }

    private abstract static class BaseFluid extends FlowableFluid {
        protected final LegacyFluidEntry entry;

        protected BaseFluid(LegacyFluidEntry entry) {
            this.entry = entry;
        }

        @Override
        public Fluid getStill() {
            return entry.still();
        }

        @Override
        public Fluid getFlowing() {
            return entry.flowing();
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
            BlockEntity blockEntity = state.hasBlockEntity() ? world.getBlockEntity(pos) : null;
            Block.dropStacks(state, world, pos, blockEntity);
        }

        @Override
        protected int getFlowSpeed(WorldView world) {
            if (entry.viscosity() >= 50000) return 1;
            if (entry.viscosity() >= 3000) return 2;
            return 4;
        }

        @Override
        protected int getLevelDecreasePerBlock(WorldView world) {
            return 1;
        }

        @Override
        public int getTickRate(WorldView world) {
            return entry.blockTickRate();
        }

        @Override
        protected float getBlastResistance() {
            return 100.0f;
        }

        @Override
        protected BlockState toBlockState(FluidState state) {
            return entry.block().getDefaultState().with(Properties.LEVEL_15, getBlockStateLevel(state));
        }

        @Override
        public boolean matchesType(Fluid fluid) {
            return fluid == entry.still() || fluid == entry.flowing();
        }

        @Override
        protected boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos, Fluid fluid, Direction direction) {
            return false;
        }

        @Override
        public void onScheduledTick(World world, BlockPos pos, FluidState state) {
            if (entry.rises()) {
                if (!world.isClient) {
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.getBlock() == entry.block()) {
                        world.scheduleBlockTick(pos, entry.block(), getTickRate(world));
                    }
                }
                return;
            }
            super.onScheduledTick(world, pos, state);
        }

        @Override
        public Optional<SoundEvent> getBucketFillSound() {
            return Optional.empty();
        }
    }

    public static final class Flowing extends BaseFluid {
        private Flowing(LegacyFluidEntry entry) {
            super(entry);
        }

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

    public static final class Still extends BaseFluid {
        private Still(LegacyFluidEntry entry) {
            super(entry);
        }

        @Override
        public int getLevel(FluidState state) {
            return 8;
        }

        @Override
        public boolean isStill(FluidState state) {
            return true;
        }
    }

    private static final class IndustrialFluidBlock extends FluidBlock {
        private final LegacyFluidEntry entry;

        private IndustrialFluidBlock(FlowableFluid fluid, LegacyFluidEntry entry, AbstractBlock.Settings settings) {
            super(fluid, settings);
            this.entry = entry;
        }

        @Override
        public BlockState getPlacementState(ItemPlacementContext ctx) {
            BlockState state = super.getPlacementState(ctx);
            return state == null ? getDefaultState() : state.with(Properties.LEVEL_15, 0);
        }

        @Override
        public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (!world.isClient && entry.rises()) {
                world.scheduleBlockTick(pos, this, entry.blockTickRate());
            }
        }

        @Override
        public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
            super.onPlaced(world, pos, state, placer, itemStack);
            if (!world.isClient && entry.vanishOnBlockItemPlacement()) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        @Override
        public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, net.minecraft.util.math.random.Random random) {
            if (entry.rises()) {
                tickRisingGas(world, pos, state);
                return;
            }
            super.scheduledTick(state, world, pos, random);
        }

        private static final int GAS_MAX_LEVEL = 7;

        private void tickRisingGas(ServerWorld world, BlockPos pos, BlockState state) {
            int level = getGasLevel(state);

            if (level != 0) {
                int recalculatedLevel = getStableGasLevel(world, pos);
                if (recalculatedLevel > GAS_MAX_LEVEL) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                    return;
                }
                if (recalculatedLevel != level) {
                    level = recalculatedLevel;
                    state = state.with(Properties.LEVEL_15, level);
                    world.setBlockState(pos, state, Block.NOTIFY_ALL);
                }
            }

            BlockPos upPos = pos.up();
            GasFlowResult upward = tryFlowGasUp(world, upPos);

            // Forge BlockFluidClassic with negative density behaves like a normal liquid with
            // the vertical preference inverted: gas goes UP first, while source blocks can
            // also emit the short four-way horizontal arms that then rise as falling water
            // would fall. Non-source flowing gas must not spread horizontally while it can
            // keep rising, or every vertical column starts producing a pyramid.
            if (level == 0) {
                spreadGasHorizontally(world, pos, 1);
                world.scheduleBlockTick(pos, this, entry.blockTickRate());
                return;
            }

            if (upward == GasFlowResult.MOVED || upward == GasFlowResult.OCCUPIED) {
                world.scheduleBlockTick(pos, this, entry.blockTickRate());
                return;
            }

            // Only when the preferred upward path is blocked do flowing gas blocks spread
            // sideways to search for a new upward path, matching IL's inverted fluid flow.
            if (level < GAS_MAX_LEVEL) {
                spreadGasHorizontally(world, pos, level + 1);
            }

            world.scheduleBlockTick(pos, this, entry.blockTickRate());
        }

        private int getGasLevel(BlockState state) {
            return state.contains(Properties.LEVEL_15) ? state.get(Properties.LEVEL_15) : 0;
        }

        private int getStableGasLevel(ServerWorld world, BlockPos pos) {
            int best = GAS_MAX_LEVEL + 1;

            BlockState belowState = world.getBlockState(pos.down());
            if (belowState.isOf(this)) {
                best = 1;
            }

            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockState neighborState = world.getBlockState(pos.offset(direction));
                if (!neighborState.isOf(this)) continue;

                int neighborLevel = getGasLevel(neighborState);
                if (neighborLevel == 0) {
                    best = Math.min(best, 1);
                } else if (neighborLevel < GAS_MAX_LEVEL) {
                    best = Math.min(best, neighborLevel + 1);
                }
            }

            return best;
        }

        private GasFlowResult tryFlowGasUp(ServerWorld world, BlockPos targetPos) {
            BlockState targetState = world.getBlockState(targetPos);
            if (targetState.isOf(this)) {
                int oldLevel = getGasLevel(targetState);
                if (oldLevel > 1) {
                    world.setBlockState(targetPos, targetState.with(Properties.LEVEL_15, 1), Block.NOTIFY_ALL);
                    world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
                    return GasFlowResult.MOVED;
                }
                world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
                return GasFlowResult.OCCUPIED;
            }
            if (!canGasDisplace(targetState)) {
                return GasFlowResult.BLOCKED;
            }

            world.setBlockState(targetPos, getDefaultState().with(Properties.LEVEL_15, 1), Block.NOTIFY_ALL);
            world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
            return GasFlowResult.MOVED;
        }

        private void spreadGasHorizontally(ServerWorld world, BlockPos pos, int level) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos targetPos = pos.offset(direction);
                BlockState targetState = world.getBlockState(targetPos);

                if (targetState.isOf(this)) {
                    int oldLevel = getGasLevel(targetState);
                    if (oldLevel != 0 && oldLevel > level) {
                        world.setBlockState(targetPos, targetState.with(Properties.LEVEL_15, level), Block.NOTIFY_ALL);
                        world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
                    }
                    continue;
                }

                if (!canGasDisplace(targetState)) continue;

                world.setBlockState(targetPos, getDefaultState().with(Properties.LEVEL_15, level), Block.NOTIFY_ALL);
                world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
            }
        }

        private boolean canGasDisplace(BlockState state) {
            return state.isAir() || state.isReplaceable();
        }

        private enum GasFlowResult {
            MOVED,
            OCCUPIED,
            BLOCKED
        }

        @Override
        public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
            super.onEntityCollision(state, world, pos, entity);
            if (world.isClient) return;

            switch (entry.path()) {
                case "pahoehoe_lava" -> entity.setOnFireFor(10);
                case "hot_coolant" -> entity.setOnFireFor(30);
                case "construction_foam" -> addStatus(entity, StatusEffects.SLOWNESS, 300, 2);
                case "uu_matter" -> addStatus(entity, StatusEffects.REGENERATION, 100, 1);
                case "steam", "superheated_steam" -> addStatus(entity, StatusEffects.BLINDNESS, 300, 0);
                case "hot_water" -> {
                    if (entity instanceof LivingEntity living) {
                        if (living.isUndead()) {
                            living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, world.random.nextInt(2), true, true));
                        } else {
                            living.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, world.random.nextInt(2), true, true));
                        }
                    }
                }
                default -> {
                }
            }
        }

        private static void addStatus(Entity entity, net.minecraft.entity.effect.StatusEffect effect, int duration, int amplifier) {
            if (entity instanceof LivingEntity living && !living.hasStatusEffect(effect)) {
                living.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier, true, true));
            }
        }
    }
}
