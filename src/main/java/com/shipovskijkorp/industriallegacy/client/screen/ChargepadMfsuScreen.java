package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.ChargepadMfsuScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ChargepadMfsuScreen extends AbstractChargepadScreen<ChargepadMfsuScreenHandler> {
    public ChargepadMfsuScreen(ChargepadMfsuScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
