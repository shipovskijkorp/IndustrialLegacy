package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.item.flight.IFlightChestItem;
import com.shipovskijkorp.industriallegacy.registry.ModFluids;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IL Experimental biogas Jetpack.
 *
 * Source values: ItemArmorJetpack(FluidName.biogas, capacity=30,000 mB),
 * power=1.0, dropPercentage=0.2, hoverMultiplier=0.2, worldHeightDivisor=1.0.
 * Refilling is intentionally left to the future canning/bottling machine; the
 * item already stores IL-like biogas NBT so that machine can fill it later.
 */
public final class BiogasJetpackItem extends ArmorItem implements IFlightChestItem {
    public static final int CAPACITY_MB = 30_000;

    private static final float POWER = 1.0f;
    private static final float DROP_PERCENTAGE = 0.2f;
    private static final float HOVER_UP = 0.2f;
    private static final float HOVER_DOWN = 0.2f;
    private static final float WORLD_HEIGHT_DIVISOR = 1.0f;

    private static final int FUEL_HOVER_MB_T = 1;
    private static final int FUEL_NORMAL_MB_T = 2;

    private static final String NBT_FLUID = "Fluid";
    private static final String NBT_FLUID_NAME = "FluidName";
    private static final String NBT_AMOUNT = "Amount";
    private static final String NBT_LEGACY_FUEL = "fuelMb";
    private static final String BIOGAS_ID = "industrial_legacy:biogas";
    private static final String IL_BIOGAS_ID = "industrial_legacy:biogas";

    public BiogasJetpackItem(Settings settings) {
        super(ModArmorMaterials.JETPACK, Type.CHESTPLATE, settings.maxCount(1));
    }

    public static ItemStack createFilledStack() {
        ItemStack stack = new ItemStack(com.shipovskijkorp.industriallegacy.registry.ModItems.JETPACK);
        setFuel(stack, CAPACITY_MB);
        return stack;
    }

    public static int getFuel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return 0;

        if (nbt.contains(NBT_FLUID, NbtElement.COMPOUND_TYPE)) {
            NbtCompound fluid = nbt.getCompound(NBT_FLUID);
            String fluidName = fluid.getString(NBT_FLUID_NAME);
            if (BIOGAS_ID.equals(fluidName) || IL_BIOGAS_ID.equals(fluidName) || ModFluids.BIOGAS.id().toString().equals(fluidName)) {
                return MathHelper.clamp(fluid.getInt(NBT_AMOUNT), 0, CAPACITY_MB);
            }
        }

        if (nbt.contains(NBT_LEGACY_FUEL)) {
            return MathHelper.clamp(nbt.getInt(NBT_LEGACY_FUEL), 0, CAPACITY_MB);
        }

        return 0;
    }

    public static void setFuel(ItemStack stack, int amountMb) {
        if (stack == null || stack.isEmpty()) return;
        int clamped = MathHelper.clamp(amountMb, 0, CAPACITY_MB);
        NbtCompound nbt = stack.getOrCreateNbt();

        if (clamped <= 0) {
            nbt.remove(NBT_FLUID);
            nbt.remove(NBT_LEGACY_FUEL);
            if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            return;
        }

        NbtCompound fluid = new NbtCompound();
        fluid.putString(NBT_FLUID_NAME, BIOGAS_ID);
        fluid.putInt(NBT_AMOUNT, clamped);
        nbt.put(NBT_FLUID, fluid);
        nbt.remove(NBT_LEGACY_FUEL);
    }

    public static float getFuelRatio(ItemStack stack) {
        return (float) getFuel(stack) / (float) CAPACITY_MB;
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
            used = useJetpack(player, hoverMode, stack, jump, sneak, forward, true);
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
            useJetpack(player, hoverMode, stack, jump, sneak, forward, false);
            if (player.isOnGround() && hoverMode) {
                setHoverModeActive(stack, false);
            }
        }
    }

    private boolean useJetpack(PlayerEntity player, boolean hoverMode, ItemStack stack, boolean jump, boolean sneak, boolean forward, boolean consumeFuel) {
        double chargeLevel = getFuelRatio(stack);
        if (chargeLevel <= 0.0) {
            return false;
        }

        int cost = hoverMode ? FUEL_HOVER_MB_T : FUEL_NORMAL_MB_T;
        if (consumeFuel && !player.isOnGround() && getFuel(stack) < cost) {
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
                player.updateVelocity(0.02f + boost, new Vec3d(0.0, 0.0, 0.4f * forwardPower + boost));
            }
        }

        int worldHeight = player.getWorld().getTopY();
        int maxFlightHeight = (int) (worldHeight / WORLD_HEIGHT_DIVISOR);
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power *= (float) ((maxFlightHeight - y) / 25.0);
        }

        double prevMotionY = player.getVelocity().y;
        double motionY = Math.min(player.getVelocity().y + power * 0.2f, 0.6000000238418579D);

        if (hoverMode) {
            float maxHoverY = 0.0f;
            if (jump) {
                maxHoverY += HOVER_UP;
            }
            if (sneak) {
                maxHoverY -= HOVER_DOWN;
            }
            if (motionY > maxHoverY) {
                motionY = maxHoverY;
                if (prevMotionY > motionY) {
                    motionY = prevMotionY;
                }
            }
        }

        if (consumeFuel && !player.isOnGround()) {
            setFuel(stack, getFuel(stack) - cost);
        }

        player.setVelocity(player.getVelocity().x, motionY, player.getVelocity().z);
        player.fallDistance = 0.0f;
        return true;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getFuel(stack) < CAPACITY_MB;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getFuel(stack) * 13.0f / (float) CAPACITY_MB);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.industrial_legacy.fluid_tank", Integer.toString(getFuel(stack)), Integer.toString(CAPACITY_MB), Text.translatable("fluid.industrial_legacy.biogas")).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(isHoverModeActive(stack)
                ? "message.industrial_legacy.flight.state_hover_on"
                : "message.industrial_legacy.flight.state_hover_off").formatted(Formatting.DARK_GRAY));
    }
}
