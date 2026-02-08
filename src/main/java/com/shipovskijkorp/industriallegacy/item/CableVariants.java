package com.shipovskijkorp.industriallegacy.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CableVariants {
    private CableVariants() {}

    /** Если ты всё ещё используешь item-model predicate "industrial_legacy:variant" */
    public static final String NBT_VARIANT = "variant";

    /**
     * Базовых моделей/вариантов = 14 (как у тебя в overrides).
     * В креативе мы показываем больше, потому что copper_0 заменяем на 4 стадии окисления.
     */
    public static final int BASE_VARIANT_COUNT = 14;

    public static String modelName(CableKind kind, int insulation) {
        return switch (kind) {
            case COPPER -> "copper_cable_" + (insulation <= 0 ? 0 : 1);
            case TIN -> "tin_cable_" + (insulation <= 0 ? 0 : 1);
            case GOLD -> "gold_cable_" + (insulation <= 0 ? 0 : (insulation == 1 ? 1 : 2));
            case IRON -> "iron_cable_" + Math.max(0, Math.min(3, insulation));
            case GLASS -> "glass_cable";
            case DETECTOR -> "detector_cable";
            case SPLITTER -> "splitter_cable";
        };
    }

    public static int variantId(CableKind kind, int insulation) {
        return switch (kind) {
            case COPPER -> (insulation <= 0 ? 0 : 1);
            case DETECTOR -> 2;
            case GLASS -> 3;
            case GOLD -> (insulation <= 0 ? 4 : (insulation == 1 ? 5 : 6));
            case IRON -> 7 + Math.max(0, Math.min(3, insulation)); // 7..10
            case SPLITTER -> 11;
            case TIN -> (insulation <= 0 ? 12 : 13);
        };
    }

    /** Создаёт стаки для вкладки креатива */
    public static List<ItemStack> createAll(Item cableItem) {
        // 14 базовых - 1 (copper_0) + 4 окисления = 17
        List<ItemStack> out = new ArrayList<>(17);

        // --- Copper uninsulated: 4 oxidation stages (0..3) ---
        for (int ox = 0; ox <= 3; ox++) {
            ItemStack s = CableItem.createStack(cableItem, CableKind.COPPER, 0);
            s.getOrCreateNbt().putInt(CableItem.NBT_OXIDATION, ox);
            out.add(s);
        }

        // Copper insulated
        out.add(make(cableItem, CableKind.COPPER, 1));

        // Others
        out.add(make(cableItem, CableKind.DETECTOR, 0));
        out.add(make(cableItem, CableKind.GLASS, 0));

        out.add(make(cableItem, CableKind.GOLD, 0));
        out.add(make(cableItem, CableKind.GOLD, 1));
        out.add(make(cableItem, CableKind.GOLD, 2));

        out.add(make(cableItem, CableKind.IRON, 0));
        out.add(make(cableItem, CableKind.IRON, 1));
        out.add(make(cableItem, CableKind.IRON, 2));
        out.add(make(cableItem, CableKind.IRON, 3));

        out.add(make(cableItem, CableKind.SPLITTER, 0));

        out.add(make(cableItem, CableKind.TIN, 0));
        out.add(make(cableItem, CableKind.TIN, 1));

        return out;
    }

    private static ItemStack make(Item cableItem, CableKind kind, int insulation) {
        // CableItem.createStack() already writes kind/insulation and derived "variant".
        return CableItem.createStack(cableItem, kind, insulation);
    }
}
