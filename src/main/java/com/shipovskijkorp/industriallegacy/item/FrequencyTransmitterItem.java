package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.entity.TeleporterBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IC2 Experimental frequency transmitter behavior port. */
public class FrequencyTransmitterItem extends Item {
    private static final String TARGET_SET_NBT = "targetSet";
    private static final String TARGET_JUST_SET_NBT = "targetJustSet";
    private static final String TARGET_X_NBT = "targetX";
    private static final String TARGET_Y_NBT = "targetY";
    private static final String TARGET_Z_NBT = "targetZ";

    public FrequencyTransmitterItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            NbtCompound nbt = stack.getOrCreateNbt();
            boolean hadJustSet = nbt.getBoolean(TARGET_JUST_SET_NBT);
            if (nbt.getBoolean(TARGET_SET_NBT) && !hadJustSet) {
                nbt.putBoolean(TARGET_SET_NBT, false);
                user.sendMessage(Text.translatable("message.industrial_legacy.frequency_transmitter.unlinked"), false);
            }
            if (hadJustSet) {
                nbt.putBoolean(TARGET_JUST_SET_NBT, false);
            }
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) return ActionResult.PASS;

        BlockPos pos = context.getBlockPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof TeleporterBlockEntity teleporter)) return ActionResult.PASS;

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        NbtCompound nbt = stack.getOrCreateNbt();
        boolean targetSet = nbt.getBoolean(TARGET_SET_NBT);
        boolean justSetTarget = true;
        BlockPos target = new BlockPos(nbt.getInt(TARGET_X_NBT), nbt.getInt(TARGET_Y_NBT), nbt.getInt(TARGET_Z_NBT));

        if (!targetSet) {
            targetSet = true;
            target = pos;
            send(player, Text.translatable("message.industrial_legacy.frequency_transmitter.linked"));
        } else if (pos.equals(target)) {
            send(player, Text.translatable("message.industrial_legacy.frequency_transmitter.self"));
        } else if (teleporter.hasTarget() && target.equals(teleporter.getTarget())) {
            send(player, Text.translatable("message.industrial_legacy.frequency_transmitter.unchanged"));
        } else {
            BlockEntity targetBe = world.getBlockEntity(target);
            if (targetBe instanceof TeleporterBlockEntity targetTeleporter) {
                teleporter.setTarget(target);
                targetTeleporter.setTarget(pos);
                send(player, Text.translatable("message.industrial_legacy.frequency_transmitter.established"));
            } else {
                justSetTarget = false;
                targetSet = false;
            }
        }

        nbt.putBoolean(TARGET_SET_NBT, targetSet);
        nbt.putBoolean(TARGET_JUST_SET_NBT, justSetTarget);
        nbt.putInt(TARGET_X_NBT, target.getX());
        nbt.putInt(TARGET_Y_NBT, target.getY());
        nbt.putInt(TARGET_Z_NBT, target.getZ());
        return ActionResult.SUCCESS;
    }

    private static void send(@Nullable PlayerEntity player, Text text) {
        if (player != null) player.sendMessage(text, false);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.getBoolean(TARGET_SET_NBT)) {
            tooltip.add(Text.translatable("tooltip.industrial_legacy.frequency_transmitter.target",
                    nbt.getInt(TARGET_X_NBT), nbt.getInt(TARGET_Y_NBT), nbt.getInt(TARGET_Z_NBT)));
        } else {
            tooltip.add(Text.translatable("tooltip.industrial_legacy.frequency_transmitter.blank"));
        }
    }
}
