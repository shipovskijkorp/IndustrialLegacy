package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.DebugWrenchItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.shipovskijkorp.industriallegacy.item.TreetapItem;


public final class ModItems {
    private ModItems() {}

    /**
     * IC2-style cable item (NBT variants).
     */
    public static final Item CABLE = register("cable", new CableItem(new FabricItemSettings()));

    public static final Item DEBUG_WRENCH = register("debug_wrench", new DebugWrenchItem(new FabricItemSettings().maxCount(1)));

    public static final Item SILVER_INGOT =
            register("silver_ingot", new Item(new Item.Settings()));

    public static final Item TIN_INGOT =
            register("tin_ingot", new Item(new Item.Settings()));

    public static final Item LEAD_INGOT =
            register("lead_ingot", new Item(new Item.Settings()));

    public static final Item SULFUR =
            register("sulfur", new Item(new Item.Settings()));

    public static final Item STICKY_RESIN = register("sticky_resin",
            new Item(new FabricItemSettings()));

    public static final Item RUBBER = register("rubber",
            new Item(new FabricItemSettings()));

    public static final Item TREETAP = register("treetap",
            new TreetapItem(new FabricItemSettings().maxCount(1).maxDamage(16)));

    /**
     * IC2 early-game tools.
     *
     * Note: crafting-reagent behavior (remaining item + durability loss per craft)
     * will be implemented later. For now we expose the tools and give them the
     * correct durability limits from IC2:
     *  - Forge Hammer: 80
     *  - Cutter: 60
     */
    public static final Item FORGE_HAMMER = register("forge_hammer",
            new Item(new FabricItemSettings().maxCount(1).maxDamage(80)));

    public static final Item CUTTER = register("cutter",
            new Item(new FabricItemSettings().maxCount(1).maxDamage(60)));


    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(IndustrialLegacy.MOD_ID, path), item);
    }

    public static void register() {
        // classload triggers static init
    }
}
