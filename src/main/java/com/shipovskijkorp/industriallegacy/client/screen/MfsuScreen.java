package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.MfsuScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class MfsuScreen extends AbstractElectricStorageScreen<MfsuScreenHandler> {
    public MfsuScreen(MfsuScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
