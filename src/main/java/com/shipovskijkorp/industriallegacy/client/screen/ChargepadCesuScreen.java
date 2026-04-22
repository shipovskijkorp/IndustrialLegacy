package com.shipovskijkorp.industriallegacy.client.screen;

import com.shipovskijkorp.industriallegacy.screen.ChargepadCesuScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public class ChargepadCesuScreen extends AbstractChargepadScreen<ChargepadCesuScreenHandler> {
    public ChargepadCesuScreen(ChargepadCesuScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
}
