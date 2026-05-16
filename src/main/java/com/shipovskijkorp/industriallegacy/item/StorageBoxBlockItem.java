package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.StorageBoxBlock;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StorageBoxBlockItem extends BlockItem {
    public static final String BLOCK_ENTITY_TAG = "BlockEntityTag";

    private final StorageBoxBlock.Type type;

    public StorageBoxBlockItem(Block block, Settings settings, StorageBoxBlock.Type type) {
        super(block, settings.maxCount(1));
        this.type = type;
    }

    public StorageBoxBlock.Type getStorageBoxType() {
        return type;
    }

    public static void writeInventoryToStack(ItemStack stack, DefaultedList<ItemStack> items) {
        if (stack.isEmpty()) return;

        boolean empty = true;
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                empty = false;
                break;
            }
        }

        NbtCompound nbt = stack.getOrCreateNbt();
        if (empty) {
            nbt.remove(BLOCK_ENTITY_TAG);
            nbt.remove("Items");
            if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            return;
        }

        NbtCompound beTag = new NbtCompound();
        Inventories.writeNbt(beTag, items);
        nbt.put(BLOCK_ENTITY_TAG, beTag);
        stack.setNbt(nbt);
    }

    public static void readInventoryFromStack(ItemStack stack, DefaultedList<ItemStack> items) {
        if (stack.isEmpty()) return;

        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }

        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return;

        if (nbt.contains(BLOCK_ENTITY_TAG, 10)) {
            Inventories.readNbt(nbt.getCompound(BLOCK_ENTITY_TAG), items);
        } else {
            // Compatibility with older/internal stacks that stored Items at root.
            Inventories.readNbt(nbt, items);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("tooltip.industrial_legacy.storage_box_keeps_items").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.storage_box_inventory_size", type.slots()).formatted(Formatting.GRAY));
    }
}
