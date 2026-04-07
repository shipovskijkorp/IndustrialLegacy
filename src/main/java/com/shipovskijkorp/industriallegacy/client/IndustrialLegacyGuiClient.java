package com.shipovskijkorp.industriallegacy.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Legacy compatibility stub. Screen registration now lives in {@link IndustrialLegacyClient}.
 */
@Deprecated
public final class IndustrialLegacyGuiClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // No-op. Kept only to avoid breaking existing references outside fabric.mod.json.
    }
}
