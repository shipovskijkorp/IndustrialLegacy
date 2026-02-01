package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.block.entity.IronFurnaceBlockEntity;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Speeds up smelting for Iron Furnace.
 *
 * Vanilla furnace cook time: 200 ticks (10s)
 * Iron Furnace cook time: 160 ticks (8s) => 0.8x
 *
 * Fuel burn time is unchanged because we only modify the cook time query.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin {

    @Inject(method = "getCookTime", at = @At("RETURN"), cancellable = true)
    private static void industriallegacy$adjustCookTime(World world, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Integer> cir) {
        if (furnace instanceof IronFurnaceBlockEntity) {
            int base = cir.getReturnValue();
            // 8s / 10s = 0.8x; IC2-style: floor (so 200 -> 160 exactly)
            int adjusted = (int) Math.floor(base * 0.8);
            if (adjusted < 1) adjusted = 1;
            cir.setReturnValue(adjusted);
        }
    }
}
