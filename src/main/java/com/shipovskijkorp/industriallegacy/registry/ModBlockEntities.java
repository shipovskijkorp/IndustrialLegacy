package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/** BlockEntityType registrations. */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    private static Identifier id(String path) {
        return new Identifier(IndustrialLegacy.MOD_ID, path);
    }

    public static final BlockEntityType<GeneratorBlockEntity> GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "generator"),
            FabricBlockEntityTypeBuilder.create(GeneratorBlockEntity::new, ModBlocks.GENERATOR).build()
    );

    public static final BlockEntityType<GeoGeneratorBlockEntity> GEO_GENERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("geo_generator"),
            FabricBlockEntityTypeBuilder.create(GeoGeneratorBlockEntity::new, ModBlocks.GEO_GENERATOR).build()
    );

    public static final BlockEntityType<LvTransformerBlockEntity> LV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("lv_transformer"),
                    FabricBlockEntityTypeBuilder.create(LvTransformerBlockEntity::new, ModBlocks.LV_TRANSFORMER).build());

    public static final BlockEntityType<MvTransformerBlockEntity> MV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("mv_transformer"),
                    FabricBlockEntityTypeBuilder.create(MvTransformerBlockEntity::new, ModBlocks.MV_TRANSFORMER).build());

    public static final BlockEntityType<HvTransformerBlockEntity> HV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("hv_transformer"),
                    FabricBlockEntityTypeBuilder.create(HvTransformerBlockEntity::new, ModBlocks.HV_TRANSFORMER).build());

    public static final BlockEntityType<EvTransformerBlockEntity> EV_TRANSFORMER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    id("ev_transformer"),
                    FabricBlockEntityTypeBuilder.create(EvTransformerBlockEntity::new, ModBlocks.EV_TRANSFORMER).build());


    public static final BlockEntityType<BatBoxBlockEntity> BATBOX = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "batbox"),
            FabricBlockEntityTypeBuilder.create(BatBoxBlockEntity::new, ModBlocks.BATBOX).build()
    );

    public static final BlockEntityType<CesuBlockEntity> CESU = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "cesu"),
            FabricBlockEntityTypeBuilder.create(CesuBlockEntity::new, ModBlocks.CESU).build()
    );
    public static final BlockEntityType<MfeBlockEntity> MFE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "mfe"),
            FabricBlockEntityTypeBuilder.create(MfeBlockEntity::new, ModBlocks.MFE).build()
    );


    public static final BlockEntityType<IronFurnaceBlockEntity> IRON_FURNACE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "iron_furnace"),
            FabricBlockEntityTypeBuilder.create(IronFurnaceBlockEntity::new, ModBlocks.IRON_FURNACE).build()
    );

    public static final BlockEntityType<MaceratorBlockEntity> MACERATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
            BlockEntityType.Builder.create(MaceratorBlockEntity::new, ModBlocks.MACERATOR).build(null)
    );

    public static final BlockEntityType<CompressorBlockEntity> COMPRESSOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            BlockEntityType.Builder.create(CompressorBlockEntity::new, ModBlocks.COMPRESSOR).build(null)
    );

    public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "electric_furnace"),
            BlockEntityType.Builder.create(ElectricFurnaceBlockEntity::new, ModBlocks.ELECTRIC_FURNACE).build(null)
    );

    public static final BlockEntityType<MetalFormerBlockEntity> METAL_FORMER = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former"),
            BlockEntityType.Builder.create(MetalFormerBlockEntity::new, ModBlocks.METAL_FORMER).build(null)
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