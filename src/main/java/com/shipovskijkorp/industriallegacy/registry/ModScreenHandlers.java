package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.BatBoxScreenHandler;
import com.shipovskijkorp.industriallegacy.screen.CesuScreenHandler;
import com.shipovskijkorp.industriallegacy.screen.GeneratorScreenHandler;
import com.shipovskijkorp.industriallegacy.screen.MaceratorScreenHandler;
import com.shipovskijkorp.industriallegacy.screen.CompressorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final Identifier GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "generator");
    public static final Identifier BATBOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "batbox");
    public static final Identifier CESU_ID = new Identifier(IndustrialLegacy.MOD_ID, "cesu");
    public static final ScreenHandlerType<MaceratorScreenHandler> MACERATOR =
            Registry.register(Registries.SCREEN_HANDLER, new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
                    new ExtendedScreenHandlerType<>(MaceratorScreenHandler::new));

    public static final ScreenHandlerType<CompressorScreenHandler> COMPRESSOR = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            new ExtendedScreenHandlerType<>(CompressorScreenHandler::new)
    );


    public static ScreenHandlerType<GeneratorScreenHandler> GENERATOR;
    public static ScreenHandlerType<BatBoxScreenHandler> BATBOX;
    public static ScreenHandlerType<CesuScreenHandler> CESU;

    private ModScreenHandlers() {}

    public static void register() {
        GENERATOR = ScreenHandlerRegistry.registerExtended(GENERATOR_ID, GeneratorScreenHandler::new);
        BATBOX = ScreenHandlerRegistry.registerExtended(BATBOX_ID, BatBoxScreenHandler::new);
        CESU = ScreenHandlerRegistry.registerExtended(CESU_ID, CesuScreenHandler::new);
    }
}
