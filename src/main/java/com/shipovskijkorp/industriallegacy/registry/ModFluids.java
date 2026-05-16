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
 * IC2 fluid registrations.
 *
 * <p>IndustrialCraft 2 2.8 registers these fluids in BlocksItems.initFluids():
 * uu_matter, construction_foam, coolant, creosote, hot_coolant, pahoehoe_lava, biomass,
 * biogas, distilled_water, superheated_steam, steam, hot_water, weed_ex, air,
 * hydrogen, oxygen, heavy_water and milk. Deuterium exists in the enum, but IC2 does
 * not register it in this version, so it is intentionally not registered here.</p>
 */
public final class ModFluids {
    public static final Identifier AIR_ID = id("air");

    private static final Map<Identifier, Ic2FluidEntry> ENTRIES_BY_ID = new LinkedHashMap<>();
    private static final List<Ic2FluidEntry> ENTRIES = new ArrayList<>();

    public static final Ic2FluidEntry UU_MATTER = registerIc2("uu_matter", 0xFF3B0533, 3000, 3000, 0, 300, false, true, false);
    public static final Ic2FluidEntry CONSTRUCTION_FOAM = registerIc2("construction_foam", 0xFF202020, 10000, 50000, 0, 300, false, true, false);
    public static final Ic2FluidEntry COOLANT = registerIc2("coolant", 0xFF145A6A, 1000, 3000, 0, 300, false, true, false);
    public static final Ic2FluidEntry CREOSOTE = registerIc2("creosote", 0xFF3D390A, 10000, 50000, 0, 300, false, true, false);
    public static final Ic2FluidEntry HOT_COOLANT = registerIc2("hot_coolant", 0xFFB52834, 1000, 3000, 0, 1200, false, true, false);
    public static final Ic2FluidEntry PAHOEHOE_LAVA = registerIc2("pahoehoe_lava", 0xFF7B746C, 50000, 250000, 10, 1200, false, false, false);
    public static final Ic2FluidEntry BIOMASS = registerIc2("biomass", 0xFF376F25, 1000, 3000, 0, 300, false, true, false);
    public static final Ic2FluidEntry BIOGAS = registerIc2("biogas", 0xFFA7984C, 1000, 3000, 0, 300, true, false, true);
    public static final Ic2FluidEntry DISTILLED_WATER = registerIc2("distilled_water", 0xFF4356F5, 1000, 1000, 0, 300, false, true, false);
    public static final Ic2FluidEntry SUPERHEATED_STEAM = registerIc2("superheated_steam", 0xFFCAD1D1, -3000, 100, 0, 600, true, false, false);
    public static final Ic2FluidEntry STEAM = registerIc2("steam", 0xFFBCBCBC, -800, 300, 0, 420, true, false, false);
    public static final Ic2FluidEntry HOT_WATER = registerIc2("hot_water", 0xFF46DEFF, 1000, 1000, 0, 350, false, true, false);
    public static final Ic2FluidEntry WEED_EX = registerIc2("weed_ex", 0xFF074F14, 1000, 1000, 0, 300, false, false, false);
    public static final Ic2FluidEntry AIR = registerIc2("air", 0xFFDCDCDC, 0, 500, 0, 300, true, false, true);
    public static final Ic2FluidEntry HYDROGEN = registerIc2("hydrogen", 0xFFDCDCDC, 0, 500, 0, 300, true, false, false);
    public static final Ic2FluidEntry OXYGEN = registerIc2("oxygen", 0xFFDCDCDC, 0, 500, 0, 300, true, false, false);
    public static final Ic2FluidEntry HEAVY_WATER = registerIc2("heavy_water", 0xFF4356F5, 1000, 1000, 0, 300, false, true, false);
    public static final Ic2FluidEntry MILK = registerIc2("milk", 0xFFFCFCFC, 1050, 1000, 0, 300, false, true, false);

    private ModFluids() {}

    public static void register() {
        // classload triggers static registration
    }

    public static Collection<Ic2FluidEntry> entries() {
        return List.copyOf(ENTRIES);
    }

    public static Ic2FluidEntry getEntry(String rawId) {
        Identifier identifier = normalizeIdentifier(rawId);
        return identifier == null ? null : ENTRIES_BY_ID.get(identifier);
    }

    public static Block getFluidBlock(String rawId) {
        Ic2FluidEntry entry = getEntry(rawId);
        return entry == null ? null : entry.block();
    }

    public static int getFluidColor(String rawId, int fallbackArgb) {
        Ic2FluidEntry entry = getEntry(rawId);
        return entry == null ? fallbackArgb : entry.tintArgb();
    }

    public static Identifier normalizeIdentifier(String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        String token = rawId.trim();
        if (token.startsWith("ic2:") && token.length() > 4) {
            token = IndustrialLegacy.MOD_ID + ":" + token.substring(4);
        } else if (token.startsWith("ic2") && token.length() > 3 && token.indexOf(':') < 0) {
            token = IndustrialLegacy.MOD_ID + ":" + token.substring(3);
        } else if (token.indexOf(':') < 0) {
            token = IndustrialLegacy.MOD_ID + ":" + token;
        }
        return Identifier.tryParse(token);
    }

    private static Ic2FluidEntry registerIc2(String path, int tintArgb, int density, int viscosity, int luminosity, int temperature, boolean gaseous, boolean hasFlowTexture, boolean vanishOnBlockItemPlacement) {
        Identifier identifier = id(path);
        Ic2FluidEntry entry = new Ic2FluidEntry(identifier, tintArgb, density, viscosity, luminosity, temperature, gaseous, hasFlowTexture, vanishOnBlockItemPlacement);

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

    public static final class Ic2FluidEntry {
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

        private Ic2FluidEntry(Identifier id, int tintArgb, int density, int viscosity, int luminosity, int temperature, boolean gaseous, boolean hasFlowTexture, boolean vanishOnBlockItemPlacement) {
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
        protected final Ic2FluidEntry entry;

        protected BaseFluid(Ic2FluidEntry entry) {
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
        public Optional<SoundEvent> getBucketFillSound() {
            return Optional.empty();
        }
    }

    public static final class Flowing extends BaseFluid {
        private Flowing(Ic2FluidEntry entry) {
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
        private Still(Ic2FluidEntry entry) {
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
        private final Ic2FluidEntry entry;

        private IndustrialFluidBlock(FlowableFluid fluid, Ic2FluidEntry entry, AbstractBlock.Settings settings) {
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

        private void tickRisingGas(ServerWorld world, BlockPos pos, BlockState state) {
            int level = state.contains(Properties.LEVEL_15) ? state.get(Properties.LEVEL_15) : 0;
            boolean source = level == 0;
            int nextLevel = source ? 1 : Math.min(7, level + 1);

            GasFlowResult upward = tryFlowGas(world, pos.up(), nextLevel);
            boolean moved = upward == GasFlowResult.MOVED;

            // IC2/Forge negative-density fluids prefer the density direction first.
            // If the block above already contains this gas, do not spill sideways
            // from the source every tick; let the gas column above continue moving.
            if (upward == GasFlowResult.BLOCKED) {
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    if (tryFlowGas(world, pos.offset(direction), nextLevel) == GasFlowResult.MOVED) {
                        moved = true;
                    }
                }
            }

            if (!source) {
                if (moved || nextLevel >= 7) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                } else {
                    world.setBlockState(pos, state.with(Properties.LEVEL_15, nextLevel), Block.NOTIFY_ALL);
                    world.scheduleBlockTick(pos, this, entry.blockTickRate());
                }
            } else {
                world.scheduleBlockTick(pos, this, entry.blockTickRate());
            }
        }

        private GasFlowResult tryFlowGas(ServerWorld world, BlockPos targetPos, int level) {
            BlockState targetState = world.getBlockState(targetPos);
            if (!targetState.isAir() && !targetState.isReplaceable() && !targetState.isOf(this)) {
                return GasFlowResult.BLOCKED;
            }
            if (targetState.isOf(this)) {
                int oldLevel = targetState.contains(Properties.LEVEL_15) ? targetState.get(Properties.LEVEL_15) : 0;
                return oldLevel <= level ? GasFlowResult.OCCUPIED : GasFlowResult.BLOCKED;
            }
            world.setBlockState(targetPos, getDefaultState().with(Properties.LEVEL_15, level), Block.NOTIFY_ALL);
            world.scheduleBlockTick(targetPos, this, entry.blockTickRate());
            return GasFlowResult.MOVED;
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
