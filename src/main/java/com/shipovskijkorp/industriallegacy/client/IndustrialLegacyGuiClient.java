package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.client.screen.BatBoxScreen;
import com.shipovskijkorp.industriallegacy.client.screen.CesuScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MfeScreen;
import com.shipovskijkorp.industriallegacy.client.screen.CompressorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.GeneratorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MaceratorScreen;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class IndustrialLegacyGuiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.GENERATOR, GeneratorScreen::new);
        HandledScreens.register(ModScreenHandlers.BATBOX, BatBoxScreen::new);
        HandledScreens.register(ModScreenHandlers.CESU, CesuScreen::new);
        
        HandledScreens.register(ModScreenHandlers.MFE, MfeScreen::new);
ScreenRegistry.register(ModScreenHandlers.MACERATOR, MaceratorScreen::new);
        HandledScreens.register(ModScreenHandlers.COMPRESSOR, CompressorScreen::new);
    }
}
