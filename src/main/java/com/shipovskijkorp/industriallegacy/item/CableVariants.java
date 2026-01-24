package com.shipovskijkorp.industriallegacy.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CableVariants {
    private CableVariants() {}

    /** Если ты всё ещё используешь item-model predicate "industrial_legacy:variant" */
    public static final String NBT_VARIANT = "variant";

    /** Ровно 14 item-моделей, которые ты скинул */
    public static final int VARIANT_COUNT = 14;

    /**
     * Важно: имена должны совпадать с models/item/cable/*.json и textures/item/cable/*.png
     * (без .json/.png)
     */
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

    /**
     * Маппинг к твоему cable.json overrides:
     * 0  = copper_cable_0 (base model)
     * 1  = copper_cable_1
     * 2  = detector_cable
     * 3  = glass_cable
     * 4  = gold_cable_0
     * 5  = gold_cable_1
     * 6  = gold_cable_2
     * 7  = iron_cable_0
     * 8  = iron_cable_1
     * 9  = iron_cable_2
     * 10 = iron_cable_3
     * 11 = splitter_cable
     * 12 = tin_cable_0
     * 13 = tin_cable_1
     */
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

    /** Создаёт 14 стакoв с kind/insulation и (опционально) проставляет NBT variant */
    public static List<ItemStack> createAll(Item cableItem) {
        List<ItemStack> out = new ArrayList<>(VARIANT_COUNT);

        out.add(make(cableItem, CableKind.COPPER, 0));
        out.add(make(cableItem, CableKind.COPPER, 1));

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
