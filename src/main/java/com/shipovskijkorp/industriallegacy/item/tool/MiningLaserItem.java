package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.entity.projectile.MiningLaserEntity;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2-style mining laser.
 * For now only the standard mining mode is implemented, but the item already plugs into
 * the universal held-item mode switch key.
 */
public final class MiningLaserItem extends Item implements IElectricItem, IModeSwitchableItem {
    public static final long CAPACITY_EU = 300_000L;
    public static final long TRANSFER_LIMIT_EU_T = 512L;
    public static final int TIER = 3;

    private static final long EU_MINING_SHOT = 1_250L;

    private static final String NBT_ENERGY = "energy";
    private static final String NBT_MODE = "mode";

    public MiningLaserItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(stack);
        }

        if (this.getMode(stack) != 0) {
            this.setMode(stack, 0);
        }

        if (this.getEnergy(stack) < EU_MINING_SHOT) {
            return TypedActionResult.fail(stack);
        }

        this.setEnergy(stack, this.getEnergy(stack) - EU_MINING_SHOT);

        Vec3d look = user.getRotationVec(1.0f).normalize();
        Vec3d start = user.getEyePos().add(look.multiply(0.2));

        MiningLaserEntity laser = new MiningLaserEntity(world, user, start, look, 999999.0f, 5.0f, Integer.MAX_VALUE);
        world.spawnEntity(laser);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,
                SoundCategory.PLAYERS, 0.35f, 1.8f);

        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), CAPACITY_EU, 3)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.mode", this.getModeName(stack)).formatted(Formatting.GRAY));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < CAPACITY_EU;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(((float) getEnergy(stack) / (float) CAPACITY_EU) * 13.0f);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    @Override
    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) {
            return 0L;
        }
        return Math.max(0L, Math.min(CAPACITY_EU, nbt.getLong(NBT_ENERGY)));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long clamped = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (clamped == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) {
                    stack.setNbt(null);
                }
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, clamped);
    }

    @Override
    public long getCapacity(ItemStack stack) {
        return CAPACITY_EU;
    }

    @Override
    public long getTransferLimit(ItemStack stack) {
        return TRANSFER_LIMIT_EU_T;
    }

    @Override
    public int getTier(ItemStack stack) {
        return TIER;
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        // Only one mode for now, but keep the infrastructure in place.
        this.setMode(stack, 0);
        return 0;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable("tooltip.industrial_legacy.mode.mining");
    }

    public int getMode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_MODE)) {
            return 0;
        }
        return nbt.getInt(NBT_MODE);
    }

    private void setMode(ItemStack stack, int mode) {
        stack.getOrCreateNbt().putInt(NBT_MODE, mode);
    }
}
