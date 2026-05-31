package com.shipovskijkorp.industriallegacy.item.tool;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** IL chainsaw semantics: axe + sword + shears style electric tool. */
public final class ElectricChainsawItem extends AbstractElectricToolItem implements IModeSwitchableItem {
    private static final String NBT_DISABLE_SHEAR = "disableShear";
    private static final double ATTACK_DAMAGE = 9.0;
    private static final double ATTACK_SPEED = -3.0;

    public ElectricChainsawItem(Settings settings) {
        super(settings, 100L, 2, 30_000L, 100L, 1, 12.0f);
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return state.isIn(BlockTags.AXE_MINEABLE)
                || state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.WOOL)
                || state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.REDSTONE_WIRE)
                || state.isOf(Blocks.TRIPWIRE)
                || state.isOf(Blocks.PUMPKIN)
                || state.isOf(Blocks.CARVED_PUMPKIN)
                || state.isOf(Blocks.MELON);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.getWorld().isClient) {
            useEnergy(stack, attacker, operationEnergyCost);
        }
        return true;
    }

    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(ItemStack stack, EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND || !canUse(stack, operationEnergyCost)) {
            return ImmutableMultimap.of();
        }
        return ImmutableMultimap.of(
                EntityAttributes.GENERIC_ATTACK_DAMAGE,
                new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", ATTACK_DAMAGE, EntityAttributeModifier.Operation.ADDITION),
                EntityAttributes.GENERIC_ATTACK_SPEED,
                new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", ATTACK_SPEED, EntityAttributeModifier.Operation.ADDITION)
        );
    }

    @Override
    public int cycleMode(ItemStack stack, ServerPlayerEntity player) {
        boolean disabled = !isShearingDisabled(stack);
        stack.getOrCreateNbt().putBoolean(NBT_DISABLE_SHEAR, disabled);
        return disabled ? 1 : 0;
    }

    @Override
    public Text getModeName(ItemStack stack) {
        return Text.translatable(isShearingDisabled(stack)
                ? "message.industrial_legacy.chainsaw.no_shear"
                : "message.industrial_legacy.chainsaw.normal");
    }

    public static boolean isShearingDisabled(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.getBoolean(NBT_DISABLE_SHEAR);
    }
}
