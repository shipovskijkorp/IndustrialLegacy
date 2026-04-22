package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ChargepadBatBoxScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ChargepadBatBoxBlockEntity extends AbstractChargepadBlockEntity {
    public ChargepadBatBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGEPAD_BATBOX, pos, state, 1, 32, 40_000L);
    }

    @Override
    protected String getContainerTranslationKey() {
        return "block.industrial_legacy.chargepad_batbox";
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ChargepadBatBoxScreenHandler(syncId, inv, this);
    }
}
