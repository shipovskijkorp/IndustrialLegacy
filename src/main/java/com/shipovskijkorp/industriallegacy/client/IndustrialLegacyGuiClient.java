package com.shipovskijkorp.industriallegacy.client;

import com.shipovskijkorp.industriallegacy.client.screen.BatBoxScreen;
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
        ScreenRegistry.register(ModScreenHandlers.MACERATOR, MaceratorScreen::new);
    }
}
