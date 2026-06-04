package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;


public final class ModScreenHandlers {
    public static final Identifier GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "generator");
    public static final Identifier BATBOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "batbox");
    public static final Identifier GEO_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "geo_generator");
    public static final Identifier SOLAR_PANEL_ID = new Identifier(IndustrialLegacy.MOD_ID, "solar_panel");
    public static final Identifier RT_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "rt_generator");
    public static final Identifier SEMIFLUID_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "semifluid_generator");
    public static final Identifier WATER_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "water_generator");
    public static final Identifier WIND_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "wind_generator");
    public static final Identifier KINETIC_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "kinetic_generator");
    public static final Identifier WIND_KINETIC_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "wind_kinetic_generator");
    public static final Identifier WATER_KINETIC_GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "water_kinetic_generator");
    public static final Identifier MAGNETIZER_ID = new Identifier(IndustrialLegacy.MOD_ID, "magnetizer");
    public static final Identifier ELECTRIC_FURNACE_ID = new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace");
    public static final Identifier INDUCTION_FURNACE_ID = new Identifier(IndustrialLegacy.MOD_ID, "induction_furnace");
    public static final Identifier CESU_ID = new Identifier(IndustrialLegacy.MOD_ID, "cesu");
    public static final Identifier MFE_ID = new Identifier(IndustrialLegacy.MOD_ID, "mfe");
    public static final Identifier MFSU_ID = new Identifier(IndustrialLegacy.MOD_ID, "mfsu");
    public static final Identifier CHARGEPAD_BATBOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "chargepad_batbox");
    public static final Identifier CHARGEPAD_CESU_ID = new Identifier(IndustrialLegacy.MOD_ID, "chargepad_cesu");
    public static final Identifier CHARGEPAD_MFE_ID = new Identifier(IndustrialLegacy.MOD_ID, "chargepad_mfe");
    public static final Identifier CHARGEPAD_MFSU_ID = new Identifier(IndustrialLegacy.MOD_ID, "chargepad_mfsu");
    public static final Identifier CONTAINMENT_BOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "containment_box");
    public static final Identifier TOOL_BOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "tool_box");
    public static final Identifier TOOL_SCANNER_ID = new Identifier(IndustrialLegacy.MOD_ID, "tool_scanner");
    public static final Identifier STORAGE_BOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "storage_box");
    public static final Identifier MV_TRANSFORMER_ID = new Identifier(IndustrialLegacy.MOD_ID, "mv_transformer");
    public static final Identifier HV_TRANSFORMER_ID = new Identifier(IndustrialLegacy.MOD_ID, "hv_transformer");
    public static final Identifier EV_TRANSFORMER_ID = new Identifier(IndustrialLegacy.MOD_ID, "ev_transformer");
    public static final ScreenHandlerType<MaceratorScreenHandler> MACERATOR =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
                    new ExtendedScreenHandlerType<>(MaceratorScreenHandler::new));
    public static final ScreenHandlerType<LvTransformerScreenHandler> LV_TRANSFORMER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "lv_transformer"),
                    new ExtendedScreenHandlerType<>(LvTransformerScreenHandler::new));
    public static final ScreenHandlerType<MvTransformerScreenHandler> MV_TRANSFORMER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "mv_transformer"),
                    new ExtendedScreenHandlerType<>(MvTransformerScreenHandler::new));
    public static final ScreenHandlerType<HvTransformerScreenHandler> HV_TRANSFORMER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "hv_transformer"),
                    new ExtendedScreenHandlerType<>(HvTransformerScreenHandler::new));
    public static final ScreenHandlerType<EvTransformerScreenHandler> EV_TRANSFORMER =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "ev_transformer"),
                    new ExtendedScreenHandlerType<>(EvTransformerScreenHandler::new));


    public static final ScreenHandlerType<GeoGeneratorScreenHandler> GEO_GENERATOR =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "geo_generator"),
                    new ExtendedScreenHandlerType<>(GeoGeneratorScreenHandler::new));

    public static final ScreenHandlerType<ElectricFurnaceScreenHandler> ELECTRIC_FURNACE =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace"),
                    new ExtendedScreenHandlerType<>(ElectricFurnaceScreenHandler::new));

    public static final ScreenHandlerType<InductionFurnaceScreenHandler> INDUCTION_FURNACE =
            Registry.register(Registries.SCREEN_HANDLER, INDUCTION_FURNACE_ID,
                    new ExtendedScreenHandlerType<>(InductionFurnaceScreenHandler::new));

    public static final ScreenHandlerType<CompressorScreenHandler> COMPRESSOR = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            new ExtendedScreenHandlerType<>(CompressorScreenHandler::new)
    );


    public static final ScreenHandlerType<ExtractorScreenHandler> EXTRACTOR = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "extractor"),
            new ExtendedScreenHandlerType<>(ExtractorScreenHandler::new)
    );

    public static final ScreenHandlerType<RecyclerScreenHandler> RECYCLER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "recycler"),
            new ExtendedScreenHandlerType<>(RecyclerScreenHandler::new)
    );

    public static final ScreenHandlerType<MetalFormerScreenHandler> METAL_FORMER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former"),
            new ExtendedScreenHandlerType<>(MetalFormerScreenHandler::new)
    );

    public static final ScreenHandlerType<SolidCannerScreenHandler> SOLID_CANNER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "solid_canner"),
            new ExtendedScreenHandlerType<>(SolidCannerScreenHandler::new)
    );

    public static final ScreenHandlerType<CannerScreenHandler> CANNER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "canner"),
            new ExtendedScreenHandlerType<>(CannerScreenHandler::new)
    );

    public static final ScreenHandlerType<FluidBottlerScreenHandler> FLUID_BOTTLER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "fluid_bottler"),
            new ExtendedScreenHandlerType<>(FluidBottlerScreenHandler::new)
    );

    public static final ScreenHandlerType<PumpScreenHandler> PUMP = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "pump"),
            new ExtendedScreenHandlerType<>(PumpScreenHandler::new)
    );


    public static final ScreenHandlerType<SolarDistillerScreenHandler> SOLAR_DISTILLER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "solar_distiller"),
            new ExtendedScreenHandlerType<>(SolarDistillerScreenHandler::new)
    );

    public static final ScreenHandlerType<ThermalCentrifugeScreenHandler> THERMAL_CENTRIFUGE = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "thermal_centrifuge"),
            new ExtendedScreenHandlerType<>(ThermalCentrifugeScreenHandler::new)
    );

    public static final ScreenHandlerType<OreWashingPlantScreenHandler> ORE_WASHING_PLANT = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "ore_washing_plant"),
            new ExtendedScreenHandlerType<>(OreWashingPlantScreenHandler::new)
    );

    public static final ScreenHandlerType<NuclearReactorScreenHandler> NUCLEAR_REACTOR = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "nuclear_reactor"),
            new ExtendedScreenHandlerType<>(NuclearReactorScreenHandler::new)
    );

    public static ScreenHandlerType<GeneratorScreenHandler> GENERATOR;
    public static ScreenHandlerType<BatBoxScreenHandler> BATBOX;
    public static ScreenHandlerType<SolarPanelScreenHandler> SOLAR_PANEL;
    public static ScreenHandlerType<RTGeneratorScreenHandler> RT_GENERATOR;
    public static ScreenHandlerType<SemifluidGeneratorScreenHandler> SEMIFLUID_GENERATOR;
    public static ScreenHandlerType<WaterGeneratorScreenHandler> WATER_GENERATOR;
    public static ScreenHandlerType<WindGeneratorScreenHandler> WIND_GENERATOR;
    public static ScreenHandlerType<KineticGeneratorScreenHandler> KINETIC_GENERATOR;
    public static ScreenHandlerType<WindKineticGeneratorScreenHandler> WIND_KINETIC_GENERATOR;
    public static ScreenHandlerType<WaterKineticGeneratorScreenHandler> WATER_KINETIC_GENERATOR;
    public static ScreenHandlerType<MagnetizerScreenHandler> MAGNETIZER;
    public static ScreenHandlerType<CesuScreenHandler> CESU;
    public static ScreenHandlerType<MfeScreenHandler> MFE;
    public static ScreenHandlerType<MfsuScreenHandler> MFSU;
    public static ScreenHandlerType<ChargepadBatBoxScreenHandler> CHARGEPAD_BATBOX;
    public static ScreenHandlerType<ChargepadCesuScreenHandler> CHARGEPAD_CESU;
    public static ScreenHandlerType<ChargepadMfeScreenHandler> CHARGEPAD_MFE;
    public static ScreenHandlerType<ChargepadMfsuScreenHandler> CHARGEPAD_MFSU;
    public static ScreenHandlerType<ContainmentBoxScreenHandler> CONTAINMENT_BOX;
    public static ScreenHandlerType<ToolboxScreenHandler> TOOL_BOX;
    public static ScreenHandlerType<ScannerScreenHandler> SCANNER;
    public static ScreenHandlerType<StorageBoxScreenHandler> STORAGE_BOX;


    private ModScreenHandlers() {}

    public static void register() {
        GENERATOR = ScreenHandlerRegistry.registerExtended(GENERATOR_ID, GeneratorScreenHandler::new);
        SOLAR_PANEL = ScreenHandlerRegistry.registerExtended(SOLAR_PANEL_ID, SolarPanelScreenHandler::new);
        RT_GENERATOR = ScreenHandlerRegistry.registerExtended(RT_GENERATOR_ID, RTGeneratorScreenHandler::new);
        SEMIFLUID_GENERATOR = ScreenHandlerRegistry.registerExtended(SEMIFLUID_GENERATOR_ID, SemifluidGeneratorScreenHandler::new);
        WATER_GENERATOR = ScreenHandlerRegistry.registerExtended(WATER_GENERATOR_ID, WaterGeneratorScreenHandler::new);
        WIND_GENERATOR = ScreenHandlerRegistry.registerExtended(WIND_GENERATOR_ID, WindGeneratorScreenHandler::new);
        KINETIC_GENERATOR = ScreenHandlerRegistry.registerExtended(KINETIC_GENERATOR_ID, KineticGeneratorScreenHandler::new);
        WIND_KINETIC_GENERATOR = ScreenHandlerRegistry.registerExtended(WIND_KINETIC_GENERATOR_ID, WindKineticGeneratorScreenHandler::new);
        WATER_KINETIC_GENERATOR = ScreenHandlerRegistry.registerExtended(WATER_KINETIC_GENERATOR_ID, WaterKineticGeneratorScreenHandler::new);
        MAGNETIZER = ScreenHandlerRegistry.registerExtended(MAGNETIZER_ID, MagnetizerScreenHandler::new);
        BATBOX = ScreenHandlerRegistry.registerExtended(BATBOX_ID, BatBoxScreenHandler::new);
        CESU = ScreenHandlerRegistry.registerExtended(CESU_ID, CesuScreenHandler::new);
        MFE = ScreenHandlerRegistry.registerExtended(MFE_ID, MfeScreenHandler::new);
        MFSU = ScreenHandlerRegistry.registerExtended(MFSU_ID, MfsuScreenHandler::new);
        CHARGEPAD_BATBOX = ScreenHandlerRegistry.registerExtended(CHARGEPAD_BATBOX_ID, ChargepadBatBoxScreenHandler::new);
        CHARGEPAD_CESU = ScreenHandlerRegistry.registerExtended(CHARGEPAD_CESU_ID, ChargepadCesuScreenHandler::new);
        CHARGEPAD_MFE = ScreenHandlerRegistry.registerExtended(CHARGEPAD_MFE_ID, ChargepadMfeScreenHandler::new);
        CHARGEPAD_MFSU = ScreenHandlerRegistry.registerExtended(CHARGEPAD_MFSU_ID, ChargepadMfsuScreenHandler::new);
        CONTAINMENT_BOX = ScreenHandlerRegistry.registerExtended(CONTAINMENT_BOX_ID, ContainmentBoxScreenHandler::new);
        TOOL_BOX = ScreenHandlerRegistry.registerExtended(TOOL_BOX_ID, ToolboxScreenHandler::new);
        SCANNER = ScreenHandlerRegistry.registerExtended(TOOL_SCANNER_ID, ScannerScreenHandler::new);
        STORAGE_BOX = ScreenHandlerRegistry.registerExtended(STORAGE_BOX_ID, StorageBoxScreenHandler::new);
    }
}