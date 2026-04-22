package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.ChargepadBatBoxScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ChargepadBatBoxScreen extends AbstractChargepadScreen<ChargepadBatBoxScreenHandler> {
    public ChargepadBatBoxScreen(ChargepadBatBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
