package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.CesuScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class CesuScreen extends AbstractElectricStorageScreen<CesuScreenHandler> {
    public CesuScreen(CesuScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
