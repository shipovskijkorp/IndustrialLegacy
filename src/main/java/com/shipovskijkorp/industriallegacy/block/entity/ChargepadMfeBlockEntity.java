package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ChargepadMfeScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ChargepadMfeBlockEntity extends AbstractChargepadBlockEntity {
    public ChargepadMfeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGEPAD_MFE, pos, state, 3, 512, 4_000_000L);
    }

    @Override
    protected String getContainerTranslationKey() {
        return "block.industrial_legacy.chargepad_mfe";
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ChargepadMfeScreenHandler(syncId, inv, this);
    }
}
