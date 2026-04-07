package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.MfeScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MfeScreen extends AbstractElectricStorageScreen<MfeScreenHandler> {
    public MfeScreen(MfeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
