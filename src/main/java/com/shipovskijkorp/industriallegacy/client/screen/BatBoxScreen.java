package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.BatBoxScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class BatBoxScreen extends AbstractElectricStorageScreen<BatBoxScreenHandler> {
    public BatBoxScreen(BatBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
