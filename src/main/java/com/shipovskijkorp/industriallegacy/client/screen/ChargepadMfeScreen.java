package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.ChargepadMfeScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ChargepadMfeScreen extends AbstractChargepadScreen<ChargepadMfeScreenHandler> {
    public ChargepadMfeScreen(ChargepadMfeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
