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

    public static final Item NICKEL_INGOT =
            register("nickel_ingot", new Item(new Item.Settings()));

    public static final Item ALUMINIUM_INGOT =
            register("aluminium_ingot", new Item(new Item.Settings()));

    public static final Item SULFUR =
            register("sulfur", new Item(new Item.Settings()));

    public static final Item STICKY_RESIN = register("sticky_resin",
            new Item(new FabricItemSettings()));

    public static final Item RUBBER = register("rubber",
            new Item(new FabricItemSettings()));

    public static final Item TREETAP = register("treetap",
            new TreetapItem(new FabricItemSettings().maxDamage(64)));


    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(IndustrialLegacy.MOD_ID, path), item);
    }

    public static void register() {
        // classload triggers static init
    }
}
