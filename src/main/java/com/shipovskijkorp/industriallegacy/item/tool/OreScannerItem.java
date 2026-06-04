package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.screen.ScannerScreenHandler;
import com.shipovskijkorp.industriallegacy.util.EnergyDisplayUtil;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * IC2 experimental-style handheld ore scanner.
 *
 * <p>Source of truth: ic2.core.item.tool.ItemScanner, ItemScannerAdv,
 * ContainerToolScanner and GuiToolScanner. The old handheld GUI is represented
 * by a Fabric extended screen handler carrying the scan result list.</p>
 */
public class OreScannerItem extends Item implements IElectricItem {
    private static final String NBT_ENERGY = "energy";
    private final long capacity;
    private final long transferLimit;
    private final int tier;
    private final int scanRange;
    private final long scanCost;

    public OreScannerItem(Settings settings, long capacity, long transferLimit, int tier, int scanRange, long scanCost) {
        super(settings.maxCount(1));
        this.capacity = capacity;
        this.transferLimit = transferLimit;
        this.tier = tier;
        this.scanRange = scanRange;
        this.scanCost = scanCost;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.success(stack, true);
        }

        if (!useEnergy(stack, user, scanCost)) {
            user.sendMessage(Text.translatable("message.industrial_legacy.scanner.no_power"), true);
            return TypedActionResult.fail(stack);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                SoundCategory.PLAYERS, 0.35F, 1.7F);

        List<ScannerScreenHandler.ScanEntry> results = scan(world, user.getBlockPos(), scanRange);
        user.openHandledScreen(new ScannerGuiFactory(stack.copy(), results));
        return TypedActionResult.success(stack, false);
    }

    /** IC2 miner integration equivalent: scanner consumes energy and returns half range. */
    public int startLayerScan(ItemStack stack) {
        return ElectricItemManager.discharge(stack, scanCost, tier, true, false, false) >= scanCost ? scanRange / 2 : 0;
    }

    public int getScanRange() {
        return scanRange;
    }

    public long getScanCost() {
        return scanCost;
    }

    private boolean useEnergy(ItemStack stack, LivingEntity user, long amount) {
        if (user instanceof PlayerEntity player && player.getAbilities().creativeMode) return true;
        return ElectricItemManager.discharge(stack, amount, tier, true, false, false) >= amount;
    }

    private List<ScannerScreenHandler.ScanEntry> scan(World world, BlockPos center, int range) {
        Map<String, MutableScanResult> map = new LinkedHashMap<>();
        int minY = Math.max(world.getBottomY(), center.getY() - range);
        int maxY = Math.min(world.getTopY() - 1, center.getY() + range);

        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int y = minY; y <= maxY; y++) {
            for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                for (int x = center.getX() - range; x <= center.getX() + range; x++) {
                    pos.set(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (!hasOreValue(state)) continue;

                    ItemStack stack = new ItemStack(state.getBlock().asItem());
                    if (stack.isEmpty()) continue;

                    String key = Registries.ITEM.getId(stack.getItem()).toString();
                    MutableScanResult result = map.get(key);
                    if (result == null) {
                        map.put(key, new MutableScanResult(stack, 1));
                    } else {
                        result.count++;
                    }
                }
            }
        }

        ArrayList<MutableScanResult> mutable = new ArrayList<>(map.values());
        mutable.sort(Comparator.comparingInt((MutableScanResult r) -> r.count).reversed()
                .thenComparing(r -> r.stack.getName().getString()));

        ArrayList<ScannerScreenHandler.ScanEntry> ret = new ArrayList<>(mutable.size());
        for (MutableScanResult result : mutable) {
            ItemStack stack = result.stack.copy();
            stack.setCount(1);
            ret.add(new ScannerScreenHandler.ScanEntry(stack, result.count));
        }
        return ret;
    }

    private boolean hasOreValue(BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath();
        return path.endsWith("_ore")
                || path.contains("_ore_")
                || path.startsWith("ore_")
                || path.endsWith("_debris")
                || path.contains("_debris_")
                || path.startsWith("debris_")
                || path.startsWith("raw_")
                || path.endsWith("_cluster");
    }

    @Override
    public long getEnergy(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(NBT_ENERGY)) return 0L;
        return Math.max(0L, Math.min(capacity, nbt.getLong(NBT_ENERGY)));
    }

    @Override
    public void setEnergy(ItemStack stack, long energy) {
        if (stack == null || stack.isEmpty()) return;
        long clamped = Math.max(0L, Math.min(capacity, energy));
        if (clamped <= 0L) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                nbt.remove(NBT_ENERGY);
                if (nbt.getKeys().isEmpty()) stack.setNbt(null);
            }
            return;
        }
        stack.getOrCreateNbt().putLong(NBT_ENERGY, clamped);
    }

    @Override public long getCapacity(ItemStack stack) { return capacity; }
    @Override public long getTransferLimit(ItemStack stack) { return transferLimit; }
    @Override public int getTier(ItemStack stack) { return tier; }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getEnergy(stack) < capacity;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (capacity <= 0L) return 0;
        return Math.round((float) getEnergy(stack) * 13.0F / (float) capacity);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return 0x33CCFF;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(EnergyDisplayUtil.formatEuStorage(getEnergy(stack), capacity, tier)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.industrial_legacy.scanner.range", scanRange).formatted(Formatting.GRAY));
    }

    private static final class MutableScanResult {
        final ItemStack stack;
        int count;

        MutableScanResult(ItemStack stack, int count) {
            this.stack = stack;
            this.count = count;
        }
    }

    private static final class ScannerGuiFactory implements ExtendedScreenHandlerFactory {
        private final ItemStack scannerStack;
        private final List<ScannerScreenHandler.ScanEntry> results;

        private ScannerGuiFactory(ItemStack scannerStack, List<ScannerScreenHandler.ScanEntry> results) {
            this.scannerStack = scannerStack;
            this.results = List.copyOf(results);
        }

        @Override
        public Text getDisplayName() {
            return scannerStack.getName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
            return new ScannerScreenHandler(syncId, results);
        }

        @Override
        public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
            ScannerScreenHandler.writeResults(buf, results);
        }
    }
}
