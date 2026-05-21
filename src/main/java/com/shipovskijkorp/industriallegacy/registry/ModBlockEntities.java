package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** BlockEntityType registrations. */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    private static Identifier id(String path) {
        return new Identifier(IndustrialLegacy.MOD_ID, path);
    }

    public static final BlockEntityType<GeneratorBlockEntity> GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "generator"),
            FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, ModBlocks.GENERATOR).build()
    );

    public static final BlockEntityType<GeoGeneratorBlockEntity> GEO_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("geo_generator"),
            FabricBlockEntityTypeBuilder.create(GeoGeneratorBlockEntity::new, ModBlocks.GEO_GENERATOR).build()
    );

    public static final BlockEntityType<SolarPanelBlockEntity> SOLAR_PANEL = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("solar_panel"),
            FabricBlockEntityTypeBuilder.create(SolarPanelBlockEntity::new, ModBlocks.SOLAR_PANEL).build()
    );

    public static final BlockEntityType<RTGeneratorBlockEntity> RT_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("rt_generator"),
            FabricBlockEntityTypeBuilder.create(RTGeneratorBlockEntity::new, ModBlocks.RT_GENERATOR).build()
    );

    public static final BlockEntityType<SemifluidGeneratorBlockEntity> SEMIFLUID_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("semifluid_generator"),
            FabricBlockEntityTypeBuilder.create(SemifluidGeneratorBlockEntity::new, ModBlocks.SEMIFLUID_GENERATOR).build()
    );


    public static final BlockEntityType<KineticGeneratorBlockEntity> KINETIC_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("kinetic_generator"),
            FabricBlockEntityTypeBuilder.create(KineticGeneratorBlockEntity::new, ModBlocks.KINETIC_GENERATOR).build()
    );

    public static final BlockEntityType<WindKineticGeneratorBlockEntity> WIND_KINETIC_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("wind_kinetic_generator"),
            FabricBlockEntityTypeBuilder.create(WindKineticGeneratorBlockEntity::new, ModBlocks.WIND_KINETIC_GENERATOR).build()
    );

    public static final BlockEntityType<LvTransformerBlockEntity> LV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("lv_transformer"),
                    FabricBlockEntityTypeBuilder.create(LvTransformerBlockEntity::new, ModBlocks.LV_TRANSFORMER).build());

    public static final BlockEntityType<MvTransformerBlockEntity> MV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("mv_transformer"),
                    FabricBlockEntityTypeBuilder.create(MvTransformerBlockEntity::new, ModBlocks.MV_TRANSFORMER).build());

    public static final BlockEntityType<HvTransformerBlockEntity> HV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("hv_transformer"),
                    FabricBlockEntityTypeBuilder.create(HvTransformerBlockEntity::new, ModBlocks.HV_TRANSFORMER).build());

    public static final BlockEntityType<EvTransformerBlockEntity> EV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("ev_transformer"),
                    FabricBlockEntityTypeBuilder.create(EvTransformerBlockEntity::new, ModBlocks.EV_TRANSFORMER).build());


    public static final BlockEntityType<BatBoxBlockEntity> BATBOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "batbox"),
            FabricBlockEntityTypeBuilder.create(BatBoxBlockEntity::new, ModBlocks.BATBOX).build()
    );

    public static final BlockEntityType<CesuBlockEntity> CESU = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "cesu"),
            FabricBlockEntityTypeBuilder.create(CesuBlockEntity::new, ModBlocks.CESU).build()
    );
    public static final BlockEntityType<MfsuBlockEntity> MFSU = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "mfsu"),
            FabricBlockEntityTypeBuilder.create(MfsuBlockEntity::new, ModBlocks.MFSU).build()
    );

    public static final BlockEntityType<MfeBlockEntity> MFE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "mfe"),
            FabricBlockEntityTypeBuilder.create(MfeBlockEntity::new, ModBlocks.MFE).build()
    );


    public static final BlockEntityType<ChargepadBatBoxBlockEntity> CHARGEPAD_BATBOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "chargepad_batbox"),
            FabricBlockEntityTypeBuilder.create(ChargepadBatBoxBlockEntity::new, ModBlocks.CHARGEPAD_BATBOX).build()
    );

    public static final BlockEntityType<ChargepadCesuBlockEntity> CHARGEPAD_CESU = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "chargepad_cesu"),
            FabricBlockEntityTypeBuilder.create(ChargepadCesuBlockEntity::new, ModBlocks.CHARGEPAD_CESU).build()
    );

    public static final BlockEntityType<ChargepadMfeBlockEntity> CHARGEPAD_MFE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "chargepad_mfe"),
            FabricBlockEntityTypeBuilder.create(ChargepadMfeBlockEntity::new, ModBlocks.CHARGEPAD_MFE).build()
    );

    public static final BlockEntityType<ChargepadMfsuBlockEntity> CHARGEPAD_MFSU = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "chargepad_mfsu"),
            FabricBlockEntityTypeBuilder.create(ChargepadMfsuBlockEntity::new, ModBlocks.CHARGEPAD_MFSU).build()
    );

    public static final BlockEntityType<LuminatorBlockEntity> LUMINATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "luminator"),
            FabricBlockEntityTypeBuilder.create(LuminatorBlockEntity::new, ModBlocks.LUMINATOR).build()
    );


    public static final BlockEntityType<IronFurnaceBlockEntity> IRON_FURNACE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "iron_furnace"),
            FabricBlockEntityTypeBuilder.create(IronFurnaceBlockEntity::new, ModBlocks.IRON_FURNACE).build()
    );

    public static final BlockEntityType<MaceratorBlockEntity> MACERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
            BlockEntityType.Builder.create(MaceratorBlockEntity::new, ModBlocks.MACERATOR).build(null)
    );

    public static final BlockEntityType<CompressorBlockEntity> COMPRESSOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            BlockEntityType.Builder.create(CompressorBlockEntity::new, ModBlocks.COMPRESSOR).build(null)
    );


    public static final BlockEntityType<ExtractorBlockEntity> EXTRACTOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "extractor"),
            BlockEntityType.Builder.create(ExtractorBlockEntity::new, ModBlocks.EXTRACTOR).build(null)
    );

    public static final BlockEntityType<RecyclerBlockEntity> RECYCLER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "recycler"),
            BlockEntityType.Builder.create(RecyclerBlockEntity::new, ModBlocks.RECYCLER).build(null)
    );

    public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace"),
            BlockEntityType.Builder.create(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE).build(null)
    );

    public static final BlockEntityType<InductionFurnaceBlockEntity> INDUCTION_FURNACE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "induction_furnace"),
            BlockEntityType.Builder.create(InductionFurnaceBlockEntity::new, ModBlocks.INDUCTION_FURNACE).build(null)
    );

    public static final BlockEntityType<MetalFormerBlockEntity> METAL_FORMER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former"),
            BlockEntityType.Builder.create(MetalFormerBlockEntity::new, ModBlocks.METAL_FORMER).build(null)
    );

    public static final BlockEntityType<SolidCannerBlockEntity> SOLID_CANNER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "solid_canner"),
            BlockEntityType.Builder.create(SolidCannerBlockEntity::new, ModBlocks.SOLID_CANNER).build(null)
    );

    public static final BlockEntityType<CannerBlockEntity> CANNER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "canner"),
            BlockEntityType.Builder.create(CannerBlockEntity::new, ModBlocks.CANNER).build(null)
    );


    public static final BlockEntityType<FluidBottlerBlockEntity> FLUID_BOTTLER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "fluid_bottler"),
            BlockEntityType.Builder.create(FluidBottlerBlockEntity::new, ModBlocks.FLUID_BOTTLER).build(null)
    );

    public static final BlockEntityType<PumpBlockEntity> PUMP = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "pump"),
            BlockEntityType.Builder.create(PumpBlockEntity::new, ModBlocks.PUMP).build(null)
    );


    public static final BlockEntityType<SolarDistillerBlockEntity> SOLAR_DISTILLER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("solar_distiller"),
            FabricBlockEntityTypeBuilder.create(SolarDistillerBlockEntity::new, ModBlocks.SOLAR_DISTILLER).build()
    );

    public static final BlockEntityType<ThermalCentrifugeBlockEntity> THERMAL_CENTRIFUGE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "thermal_centrifuge"),
            BlockEntityType.Builder.create(ThermalCentrifugeBlockEntity::new, ModBlocks.THERMAL_CENTRIFUGE).build(null)
    );

    public static final BlockEntityType<OreWashingPlantBlockEntity> ORE_WASHING_PLANT = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "ore_washing_plant"),
            BlockEntityType.Builder.create(OreWashingPlantBlockEntity::new, ModBlocks.ORE_WASHING_PLANT).build(null)
    );

    public static final BlockEntityType<NuclearReactorBlockEntity> NUCLEAR_REACTOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "nuclear_reactor"),
            BlockEntityType.Builder.create(NuclearReactorBlockEntity::new, ModBlocks.NUCLEAR_REACTOR).build(null)
    );


    public static final BlockEntityType<StorageBoxBlockEntity> STORAGE_BOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("storage_box"),
            FabricBlockEntityTypeBuilder.create(StorageBoxBlockEntity::new,
                    ModBlocks.WOODEN_STORAGE_BOX,
                    ModBlocks.IRON_STORAGE_BOX,
                    ModBlocks.BRONZE_STORAGE_BOX,
                    ModBlocks.STEEL_STORAGE_BOX,
                    ModBlocks.IRIDIUM_STORAGE_BOX
            ).build()
    );


    /** Cable BE used for thin cable rendering + detector/splitter behavior and EU-net bookkeeping. */
    public static final BlockEntityType<CableBlockEntity> CABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "cable"),
            FabricBlockEntityTypeBuilder.create(CableBlockEntity::new,
                    ModBlocks.COPPER_CABLE_0,
                    ModBlocks.COPPER_CABLE_1,
                    ModBlocks.TIN_CABLE_0,
                    ModBlocks.TIN_CABLE_1,
                    ModBlocks.GOLD_CABLE_0,
                    ModBlocks.GOLD_CABLE_1,
                    ModBlocks.GOLD_CABLE_2,
                    ModBlocks.IRON_CABLE_0,
                    ModBlocks.IRON_CABLE_1,
                    ModBlocks.IRON_CABLE_2,
                    ModBlocks.IRON_CABLE_3,
                    ModBlocks.GLASS_CABLE,
                    ModBlocks.DETECTOR_CABLE,
                    ModBlocks.SPLITTER_CABLE
            ).build()
    );

    public static void register() {
        // classload triggers static init
    }
}