package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ChargepadCesuScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ChargepadCesuBlockEntity extends AbstractChargepadBlockEntity {
    public ChargepadCesuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGEPAD_CESU, pos, state, 2, 128, 300_000L);
    }

    @Override
    protected String getContainerTranslationKey() {
        return "block.industrial_legacy.chargepad_cesu";
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ChargepadCesuScreenHandler(syncId, inv, this);
    }
}
