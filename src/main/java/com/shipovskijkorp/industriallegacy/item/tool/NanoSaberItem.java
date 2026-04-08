package com.shipovskijkorp.industriallegacy.item.tool;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;

/**
 * IC2 Experimental Nano Saber (electric sword).
 *
 * Visuals:
 * - model swap via predicate industrial_legacy:active (0/1) to nano_saber_active model
 * - animated texture driven by nano_saber_active.png.mcmeta
 * - no enchant glint (requested)
 *
 * Charge bar:
 * - always visible like NanoSuit armor
 */
public final class NanoSaberItem extends SwordItem implements IElectricItem {

    public static final long CAPACITY_EU = 160_000L;
    public static final int TIER = 3;
    public static final long TRANSFER_LIMIT_EU_T = 500L;

    private static final long EU_TOGGLE_ON = 16L;
    private static final long EU_HIT = 400L;
    private static final long EU_BREAK = 80L;

    private static final long EU_DRAIN_HOTBAR = 64L; // per 16 ticks
    private static final long EU_DRAIN_OTHER = 16L;  // per 64 ticks

    private static final String NBT_ENERGY = "energy";
    private static final String NBT_ACTIVE = "active";

    private static final float ATTACK_SPEED = -2.4f;

    public NanoSaberItem(Settings settings) {
        // Diamond sword base damage is 3; +1 => 4 (IC2 inactive)
        super(ToolMaterials.DIAMOND, 1, ATTACK_SPEED, settings.maxCount(1));
    }

    // ---------------- IElectricItem ----------------
    @Override
    public long getEnergy(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        long e = nbt.getLong(NBT_ENERGY);
        if (e < 0L) e = 0L;
        if (e > CAPACITY_EU) e = CAPACITY_EU;
        return e;
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        long e = Math.max(0L, Math.min(CAPACITY_EU, energy));
        if (e == 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, e);
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
    public float getChargeRatio(ItemStack stack) {
        return (float) getEnergy(stack) / (float) CAPACITY_EU;
    }

    // ---------------- State helpers ----------------
    public static boolean isActive(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_ACTIVE);
    }

    private static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean(NBT_ACTIVE, active);
    }

    private static boolean canUse(ItemStack stack, long amount) {
        return getEnergyStatic(stack) >= amount;
    }

    private static long drainOrTurnOff(ItemStack stack, long amount) {
        long stored = getEnergyStatic(stack);
        long extracted = Math.min(amount, stored);
        if (extracted > 0L) setEnergyStatic(stack, stored - extracted);
        if (extracted < amount) setActive(stack, false);
        return extracted;
    }

    private static long getEnergyStatic(ItemStack stack) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        return ei.getEnergy(stack);
    }

    private static void setEnergyStatic(ItemStack stack, long energy) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return;
        ei.setEnergy(stack, energy);
    }

    // ---------------- IC2-like behaviour ----------------

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) return TypedActionResult.pass(stack);

        if (isActive(stack)) {
            setActive(stack, false);
            return TypedActionResult.success(stack);
        }

        if (canUse(stack, EU_TOGGLE_ON)) {
            setActive(stack, true);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, net.minecraft.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient) return;
        if (!isActive(stack)) return;
        if (!(entity instanceof PlayerEntity)) return;

        if (entity.age % 16 == 0 && slot < 9) {
            drainOrTurnOff(stack, EU_DRAIN_HOTBAR);
        } else if (entity.age % 64 == 0 && slot >= 9) {
            drainOrTurnOff(stack, EU_DRAIN_OTHER);
        }
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isActive(stack)) {
            drainOrTurnOff(stack, EU_HIT);
        }
        return super.postHit(stack, target, attacker);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, net.minecraft.block.BlockState state, net.minecraft.util.math.BlockPos pos, LivingEntity miner) {
        if (isActive(stack)) {
            drainOrTurnOff(stack, EU_BREAK);
        }
        return super.postMine(stack, world, state, pos, miner);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return false;
    }

    // Always show charge bar (like Nano armor)
    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        long cap = getCapacity(stack);
        if (cap <= 0L) return false;
        return getEnergy(stack) < cap;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        double r = (double) getEnergy(stack) / (double) CAPACITY_EU;
        return (int) Math.round(r * 13.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x55FF55;
    }

    // Stack-aware attribute modifiers (Fabric)
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return ImmutableMultimap.of();
        }

        int dmg = 4;
        if (isActive(stack) && canUse(stack, EU_HIT)) {
            dmg = 20;
        }

        return ImmutableMultimap.of(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", dmg, EntityAttributeModifier.Operation.ADDITION),
                EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", ATTACK_SPEED, EntityAttributeModifier.Operation.ADDITION)
        );
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        long eu = getEnergy(stack);
        long cap = CAPACITY_EU;

        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(eu, cap, 3)).formatted(Formatting.GRAY));
    }
}
