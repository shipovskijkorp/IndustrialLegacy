package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IC2-style electric tool base.
 *
 * The tool does not use vanilla durability. Its visible bar is the stored EU,
 * and block/entity operations consume EU instead of damaging the item.
 */
public abstract class AbstractElectricToolItem extends Item implements IElectricItem {
    protected static final String NBT_ENERGY = "energy";

    private final long capacityEu;
    private final long transferLimitEu;
    private final int tier;
    protected final long operationEnergyCost;
    protected final float chargedEfficiency;
    protected final int harvestLevel;

    protected AbstractElectricToolItem(Settings settings, long operationEnergyCost, int harvestLevel,
                                       long capacityEu, long transferLimitEu, int tier, float chargedEfficiency) {
        super(settings.maxCount(1));
        this.operationEnergyCost = operationEnergyCost;
        this.harvestLevel = harvestLevel;
        this.capacityEu = capacityEu;
        this.transferLimitEu = transferLimitEu;
        this.tier = tier;
        this.chargedEfficiency = chargedEfficiency;
    }

    @Override
    public long getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        return Math.max(0L, Math.min(capacityEu, nbt.getLong(NBT_ENERGY)));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        if (stack == null || stack.isEmpty()) return;
        long clamped = Math.max(0L, Math.min(capacityEu, energy));
        if (clamped <= 0L) {
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
        return capacityEu;
    }

    @Override
    public long getTransferLimit(ItemStack stack) {
        return transferLimitEu;
    }

    @Override
    public int getTier(ItemStack stack) {
        return tier;
    }

    protected boolean canUse(ItemStack stack, long amount) {
        return getEnergy(stack) >= amount;
    }

    protected boolean useEnergy(ItemStack stack, LivingEntity user, long amount) {
        if (amount <= 0L) return true;
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) return true;
        if (!canUse(stack, amount)) return false;
        long extracted = ElectricItemManager.discharge(stack, amount, false);
        return extracted >= amount;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < capacityEu;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (capacityEu <= 0L) return 0;
        return Math.round((float) getEnergy(stack) * 13.0f / (float) capacityEu);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        if (!canUse(stack, operationEnergyCost)) {
            return 1.0f;
        }
        return isEffectiveOn(state) ? chargedEfficiency : 1.0f;
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return isEffectiveOn(state) && canHarvestRequiredTier(state);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && state.getHardness(world, pos) != 0.0f) {
            useEnergy(stack, miner, operationEnergyCost);
        }
        return true;
    }

    protected boolean canHarvestRequiredTier(BlockState state) {
        if (state.isIn(BlockTags.NEEDS_DIAMOND_TOOL)) return harvestLevel >= 3;
        if (state.isIn(BlockTags.NEEDS_IRON_TOOL)) return harvestLevel >= 2;
        if (state.isIn(BlockTags.NEEDS_STONE_TOOL)) return harvestLevel >= 1;
        return true;
    }

    protected abstract boolean isEffectiveOn(BlockState state);

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), capacityEu, tier)).formatted(Formatting.GRAY));
    }
}
