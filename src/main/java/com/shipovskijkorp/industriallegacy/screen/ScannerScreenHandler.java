package com.shipovskijkorp.industriallegacy.screen;

import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScannerScreenHandler extends ScreenHandler {
    public static final int MAX_RESULTS = 10;

    private final List<ScanEntry> results;

    public ScannerScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, readResults(buf));
    }

    public ScannerScreenHandler(int syncId, List<ScanEntry> results) {
        super(ModScreenHandlers.SCANNER, syncId);
        this.results = List.copyOf(results);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    public List<ScanEntry> getResults() {
        return results;
    }

    public static void writeResults(PacketByteBuf buf, List<ScanEntry> results) {
        int size = Math.min(MAX_RESULTS, results.size());
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            ScanEntry entry = results.get(i);
            ItemStack stack = entry.stack().copy();
            stack.setCount(1);
            buf.writeItemStack(stack);
            buf.writeVarInt(entry.count());
        }
    }

    private static List<ScanEntry> readResults(PacketByteBuf buf) {
        int size = Math.min(MAX_RESULTS, buf.readVarInt());
        List<ScanEntry> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItemStack();
            int count = buf.readVarInt();
            ret.add(new ScanEntry(stack, count));
        }
        return Collections.unmodifiableList(ret);
    }

    public record ScanEntry(ItemStack stack, int count) {}
}
