package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.screen.ChargepadMfsuScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class ChargepadMfsuBlockEntity extends AbstractChargepadBlockEntity {
    public ChargepadMfsuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGEPAD_MFSU, pos, state, 4, 2048, 40_000_000L);
    }

    @Override
    protected String getContainerTranslationKey() {
        return "block.industrial_legacy.chargepad_mfsu";
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new ChargepadMfsuScreenHandler(syncId, inv, this);
    }
}
