package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.entity.upgrade.UpgradableProperty;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Set;

/** IL-style machine upgrade item. */
public class MachineUpgradeItem extends Item {
    public enum UpgradeType {
        OVERCLOCKER(false),
        TRANSFORMER(false),
        ENERGY_STORAGE(false),
        REDSTONE_INVERTER(false),
        EJECTOR(true),
        ADVANCED_EJECTOR(true),
        PULLING(true),
        ADVANCED_PULLING(true),
        FLUID_EJECTOR(true),
        FLUID_PULLING(true),
        REMOTE_INTERFACE(false);

        private final boolean directional;

        UpgradeType(boolean directional) {
            this.directional = directional;
        }

        public boolean isDirectional() {
            return directional;
        }
    }

    private final UpgradeType type;

    public MachineUpgradeItem(Settings settings, UpgradeType type) {
        super(settings);
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return type;
    }

    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> properties) {
        if (stack.isEmpty() || stack.getItem() != this) return false;
        return switch (type) {
            case EJECTOR, ADVANCED_EJECTOR -> properties.contains(UpgradableProperty.ItemProducing);
            case PULLING, ADVANCED_PULLING -> properties.contains(UpgradableProperty.ItemConsuming);
            case FLUID_EJECTOR -> properties.contains(UpgradableProperty.FluidProducing);
            case FLUID_PULLING -> properties.contains(UpgradableProperty.FluidConsuming);
            case ENERGY_STORAGE -> properties.contains(UpgradableProperty.EnergyStorage);
            case OVERCLOCKER -> properties.contains(UpgradableProperty.Processing) || properties.contains(UpgradableProperty.Augmentable);
            case REDSTONE_INVERTER -> properties.contains(UpgradableProperty.RedstoneSensitive);
            case TRANSFORMER -> properties.contains(UpgradableProperty.Transformer);
            case REMOTE_INTERFACE -> properties.contains(UpgradableProperty.RemotelyAccessible);
        };
    }

    public static boolean isUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MachineUpgradeItem;
    }

    public static boolean isDirectional(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof MachineUpgradeItem upgrade
                && upgrade.getUpgradeType().isDirectional();
    }

    public static Direction getDirection(ItemStack stack) {
        if (!isDirectional(stack)) return null;
        NbtCompound nbt = stack.getOrCreateNbt();
        byte raw = nbt.getByte("dir");
        Direction[] dirs = Direction.values();
        if (raw < 1 || raw > dirs.length) return null;
        return dirs[raw - 1];
    }

    public static void setDirection(ItemStack stack, Direction direction) {
        if (!isDirectional(stack)) return;
        NbtCompound nbt = stack.getOrCreateNbt();
        if (direction == null) nbt.putByte("dir", (byte) 0);
        else nbt.putByte("dir", (byte) (direction.ordinal() + 1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (!isDirectional(stack)) return ActionResult.PASS;

        World world = context.getWorld();
        Direction clickedSide = context.getSide();
        Direction current = getDirection(stack);
        Direction next = current == clickedSide ? null : clickedSide;
        if (!world.isClient) {
            setDirection(stack, next);
            PlayerEntity player = context.getPlayer();
            if (player != null) {
                String side = next == null ? "any" : next.asString();
                player.sendMessage(Text.translatable("tooltip.industrial_legacy.upgrade.side", side), true);
            }
        }
        return ActionResult.SUCCESS;
    }
}
