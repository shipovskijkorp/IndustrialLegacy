package com.shipovskijkorp.industriallegacy.block.entity;

import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * BlockEntity for {@link com.shipovskijkorp.industriallegacy.block.IronFurnaceBlock}.
 *
 * Uses vanilla smelting recipes (RecipeType.SMELTING).
 * Cook time is adjusted via mixin (see AbstractFurnaceBlockEntityMixin).
 */
public final class IronFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_FURNACE, pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.industrial_legacy.iron_furnace");
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FurnaceScreenHandler(syncId, playerInventory, this, this.propertyDelegate);
    }
}
