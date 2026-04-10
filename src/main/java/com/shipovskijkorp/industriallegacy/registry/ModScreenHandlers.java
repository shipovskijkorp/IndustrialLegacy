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
    public static final Identifier ELECTRIC_FURNACE_ID = new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace");
    public static final Identifier CESU_ID = new Identifier(IndustrialLegacy.MOD_ID, "cesu");
    public static final Identifier MFE_ID = new Identifier(IndustrialLegacy.MOD_ID, "mfe");
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

    public static final ScreenHandlerType<CompressorScreenHandler> COMPRESSOR = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            new ExtendedScreenHandlerType<>(CompressorScreenHandler::new)
    );

    public static final ScreenHandlerType<MetalFormerScreenHandler> METAL_FORMER = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former"),
            new ExtendedScreenHandlerType<>(MetalFormerScreenHandler::new)
    );

    public static ScreenHandlerType<GeneratorScreenHandler> GENERATOR;
    public static ScreenHandlerType<BatBoxScreenHandler> BATBOX;
    public static ScreenHandlerType<CesuScreenHandler> CESU;
    public static ScreenHandlerType<MfeScreenHandler> MFE;


    private ModScreenHandlers() {}

    public static void register() {
        GENERATOR = ScreenHandlerRegistry.registerExtended(GENERATOR_ID, GeneratorScreenHandler::new);
        BATBOX = ScreenHandlerRegistry.registerExtended(BATBOX_ID, BatBoxScreenHandler::new);
        CESU = ScreenHandlerRegistry.registerExtended(CESU_ID, CesuScreenHandler::new);
        MFE = ScreenHandlerRegistry.registerExtended(MFE_ID, MfeScreenHandler::new);
    }
}