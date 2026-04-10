package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.item.flight.IFlightChestItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Quantum chestplate provides jetpack flight with IC2 quantum parameters.
 */
public final class QuantumChestplateItem extends QuantumArmorItem implements IFlightChestItem {
    private static final float POWER = 1.0f;
    private static final float DROP_PERCENTAGE = 0.05f;
    private static final float HOVER_UP = 0.1f;
    private static final float HOVER_DOWN = 0.1f;
    private static final float WORLD_HEIGHT_DIVISOR = 0.9f;

    public QuantumChestplateItem(Settings settings) {
        super(Type.CHESTPLATE, settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (world.isClient) return;
        if (entity instanceof ServerPlayerEntity player && player.getEquippedStack(EquipmentSlot.CHEST) == stack) {
            player.extinguish();
        }
    }

    @Override
    public boolean isFlightActive(ItemStack stack) {
        return true;
    }

    @Override
    public void tickFlightServer(ServerPlayerEntity player, ItemStack stack, boolean jump, boolean sneak, boolean forward) {
        boolean hoverMode = isHoverModeActive(stack);
        boolean used = false;

        if (jump || hoverMode) {
            used = useQuantumFlight(player, stack, hoverMode, jump, sneak, forward, true);
            if (player.isOnGround() && hoverMode) {
                setHoverModeActive(stack, false);
                onGroundHoverDisabled(player, stack);
            }
        }

        if (used) {
            player.currentScreenHandler.sendContentUpdates();
        }
    }

    @Override
    public void tickFlightClient(PlayerEntity player, ItemStack stack, boolean jump, boolean sneak, boolean forward) {
        boolean hoverMode = isHoverModeActive(stack);
        if (jump || hoverMode) {
            useQuantumFlight(player, stack, hoverMode, jump, sneak, forward, false);
            if (player.isOnGround() && hoverMode) {
                setHoverModeActive(stack, false);
            }
        }
    }

    private boolean useQuantumFlight(PlayerEntity player, ItemStack stack, boolean hoverMode, boolean jump, boolean sneak, boolean forward, boolean consumeEnergy) {
        double chargeLevel = (double) getEnergy(stack) / (double) CAPACITY_EU;
        if (chargeLevel <= 0.0D) {
            return false;
        }

        float power = POWER;
        if (chargeLevel <= DROP_PERCENTAGE) {
            power *= (float) (chargeLevel / DROP_PERCENTAGE);
        }

        if (forward) {
            float retruster = hoverMode ? 1.0f : 0.15f;
            float boost = 0.0f;
            float forwardPower = power * retruster * 2.0f;
            if (forwardPower > 0.0f) {
                player.updateVelocity(0.02f + boost, new Vec3d(0.0D, 0.0D, 0.4f * forwardPower + boost));
            }
        }

        int worldHeight = player.getWorld().getTopY();
        int maxFlightHeight = (int) (worldHeight / WORLD_HEIGHT_DIVISOR);
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power *= (float) ((maxFlightHeight - y) / 25.0D);
        }

        double prevMotionY = player.getVelocity().y;
        double motionY = Math.min(player.getVelocity().y + power * 0.2f, 0.6000000238418579D);
        if (hoverMode) {
            float maxHoverY = 0.0f;
            if (jump) {
                maxHoverY += HOVER_UP;
            }
            if (sneak) {
                maxHoverY += -HOVER_DOWN;
            }
            if (motionY > maxHoverY) {
                motionY = maxHoverY;
                if (prevMotionY > motionY) {
                    motionY = prevMotionY;
                }
            }
        }

        if (consumeEnergy && !player.isOnGround()) {
            int consume = hoverMode ? 1 : 2;
            if (drainJetpackEnergy(stack, consume, true) < consume + 6L) {
                return false;
            }
            drainJetpackEnergy(stack, consume, false);
        }

        player.setVelocity(player.getVelocity().x, motionY, player.getVelocity().z);
        player.fallDistance = 0.0f;
        return true;
    }

    private static long drainJetpackEnergy(ItemStack stack, int amount, boolean simulate) {
        long total = amount + 6L;
        return drainIgnoreLimit(stack, total, simulate);
    }
}
