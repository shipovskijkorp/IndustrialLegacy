package com.shipovskijkorp.industriallegacy.item.armor;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IL CF Pack: chest utility armor carrying 80,000 mB of construction foam. */
public class FoamPackItem extends ArmorItem {
    private static final String NBT_FOAM = "foam";
    public static final int CAPACITY_MB = 80_000;

    public FoamPackItem(Settings settings) {
        super(ModArmorMaterials.BATPACK, Type.CHESTPLATE, settings.maxCount(1));
    }

    public static int getFoam(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : Math.max(0, Math.min(CAPACITY_MB, nbt.getInt(NBT_FOAM)));
    }

    public static void setFoam(ItemStack stack, int amountMb) {
        int clamped = Math.max(0, Math.min(CAPACITY_MB, amountMb));
        NbtCompound nbt = stack.getOrCreateNbt();
        if (clamped <= 0) {
            nbt.remove(NBT_FOAM);
            if (nbt.getKeys().isEmpty()) stack.setNbt(null);
        } else {
            nbt.putInt(NBT_FOAM, clamped);
        }
    }

    public static boolean canFill(ItemStack stack) {
        return stack.getItem() instanceof FoamPackItem && getFoam(stack) < CAPACITY_MB;
    }

    public static int fill(ItemStack stack, int availableMb) {
        int fill = Math.min(Math.max(0, availableMb), CAPACITY_MB - getFoam(stack));
        if (fill > 0) setFoam(stack, getFoam(stack) + fill);
        return fill;
    }

    public static int drain(ItemStack stack, int maxDrainMb) {
        int drain = Math.min(Math.max(0, maxDrainMb), getFoam(stack));
        if (drain > 0) setFoam(stack, getFoam(stack) - drain);
        return drain;
    }

    public static ItemStack createFilledStack() {
        ItemStack stack = new ItemStack(ModItems.CF_PACK);
        setFoam(stack, CAPACITY_MB);
        return stack;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return getFoam(stack) < CAPACITY_MB;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round((float) getFoam(stack) * 13.0f / (float) CAPACITY_MB);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float ratio = getItemBarStep(stack) / 13.0f;
        return MathHelper.hsvToRgb(Math.max(0.0f, ratio / 3.0f), 1.0f, 1.0f);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.industrial_legacy.cf_pack.foam", getFoam(stack), CAPACITY_MB).formatted(Formatting.GRAY));
    }
}
