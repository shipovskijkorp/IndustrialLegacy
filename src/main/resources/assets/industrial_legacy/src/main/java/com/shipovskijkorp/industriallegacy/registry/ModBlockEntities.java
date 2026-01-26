package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.BatBoxBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.GeneratorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** BlockEntityType registrations. */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final BlockEntityType<GeneratorBlockEntity> GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "generator"),
            FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, ModBlocks.GENERATOR).build()
    );

    public static final BlockEntityType<BatBoxBlockEntity> BATBOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "batbox"),
            FabricBlockEntityTypeBuilder.create(BatBoxBlockEntity::new, ModBlocks.BATBOX).build()
    );

    /** Cable BE used for thin cable rendering + detector/splitter behavior and EU-net bookkeeping. */
    public static final BlockEntityType<CableBlockEntity> CABLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "cable"),
            FabricBlockEntityTypeBuilder.create(CableBlockEntity::new,
                    ModBlocks.COPPER_CABLE_0,
                    ModBlocks.COPPER_CABLE_1,
                    ModBlocks.TIN_CABLE_0,
                    ModBlocks.TIN_CABLE_1,
                    ModBlocks.GOLD_CABLE_0,
                    ModBlocks.GOLD_CABLE_1,
                    ModBlocks.GOLD_CABLE_2,
                    ModBlocks.IRON_CABLE_0,
                    ModBlocks.IRON_CABLE_1,
                    ModBlocks.IRON_CABLE_2,
                    ModBlocks.IRON_CABLE_3,
                    ModBlocks.GLASS_CABLE,
                    ModBlocks.DETECTOR_CABLE,
                    ModBlocks.SPLITTER_CABLE
            ).build()
    );

    public static void register() {
        // classload triggers static init
    }
}
