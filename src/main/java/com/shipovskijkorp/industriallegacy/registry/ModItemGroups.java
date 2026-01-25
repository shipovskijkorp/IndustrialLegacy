package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final ItemGroup MAIN = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(IndustrialLegacy.MOD_ID, "main"),
            FabricItemGroup.builder()
                    // Icon: copper cable (uninsulated)
                    .icon(() -> CableItem.createStack(ModItems.CABLE, CableKind.COPPER, 0))
                    .displayName(Text.translatable("itemGroup.industrial_legacy.main"))
                    .entries((ctx, entries) -> {
                        // Put cable variants first (IC2-style: one item, many NBT variants).
                        for (ItemStack stack : CableVariants.createAll(ModItems.CABLE)) {
                            entries.add(stack);
                        }

                        // Then add every other item from our namespace (blocks included via their BlockItem).
                        // This keeps the creative tab "complete" without having to maintain a manual list.
                        List<Identifier> ids = new ArrayList<>();
                        for (Identifier id : Registries.ITEM.getIds()) {
                            if (IndustrialLegacy.MOD_ID.equals(id.getNamespace())) {
                                ids.add(id);
                            }
                        }
                        ids.sort(Comparator.comparing(Identifier::getPath));

                        for (Identifier id : ids) {
                            Item item = Registries.ITEM.get(id);
                            if (item == ModItems.CABLE) continue; // variants already added above
                            entries.add(item);
                        }
                    })
                    .build()
    );

    public static void register() {
        // classload triggers static init
    }
}
