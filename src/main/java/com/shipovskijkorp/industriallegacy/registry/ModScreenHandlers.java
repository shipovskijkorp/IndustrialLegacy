package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.screen.BatBoxScreenHandler;
import com.shipovskijkorp.industriallegacy.screen.GeneratorScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final Identifier GENERATOR_ID = new Identifier(IndustrialLegacy.MOD_ID, "generator");
    public static final Identifier BATBOX_ID = new Identifier(IndustrialLegacy.MOD_ID, "batbox");

    public static ScreenHandlerType<GeneratorScreenHandler> GENERATOR;
    public static ScreenHandlerType<BatBoxScreenHandler> BATBOX;

    private ModScreenHandlers() {}

    public static void register() {
        GENERATOR = ScreenHandlerRegistry.registerExtended(GENERATOR_ID, GeneratorScreenHandler::new);
        BATBOX = ScreenHandlerRegistry.registerExtended(BATBOX_ID, BatBoxScreenHandler::new);
    }
}
