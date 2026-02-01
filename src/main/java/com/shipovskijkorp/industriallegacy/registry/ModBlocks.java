package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.block.*;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

/** Block + BlockItem registrations. */
public final class ModBlocks {
    private ModBlocks() {}

    public static final Block GENERATOR = register(
            "generator",
            new GeneratorBlock(FabricBlockSettings.create()
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> state.get(GeneratorBlock.LIT) ? 13 : 0))
    );

    public static final Block BATBOX = register(
            "batbox",
            new BatBoxBlock(FabricBlockSettings.create()
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool())
    );

    public static final Block MACERATOR = register(
            "macerator",
            new MaceratorBlock(FabricBlockSettings.create()
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(s -> s.get(MaceratorBlock.LIT) ? 13 : 0))
    );

    public static final Block COMPRESSOR = register(
            "compressor",
            new CompressorBlock(FabricBlockSettings.create()
                    .strength(2.0f)
                    .sounds(BlockSoundGroup.METAL)
                    .requiresTool()
                    .luminance(state -> state.get(CompressorBlock.LIT) ? 13 : 0))
    );


    /**
     * Iron Furnace — upgraded furnace (8s smelt, vanilla fuel burn time).
     * Light level matches vanilla furnace.
     */
    public static final Block IRON_FURNACE = register(
            "iron_furnace",
            new IronFurnaceBlock(FabricBlockSettings.copyOf(Blocks.FURNACE)
                    .requiresTool()
                    .luminance(state -> state.get(IronFurnaceBlock.LIT) ? 13 : 0))
    );

    /**
     * Basic Machine Casing (IL-like "machine casing").
     *
     * Building material + crafting component.
     * Strength is identical to vanilla iron block.
     */
    public static final Block MACHINE_CASING = register(
            "machine_casing",
            new Block(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).requiresTool())
    );

    public static final Block LEAD_ORE =
            register("lead_ore", new Block(FabricBlockSettings.copyOf(Blocks.IRON_ORE).requiresTool()));
    public static final Block TIN_ORE =
            register("tin_ore", new Block(FabricBlockSettings.copyOf(Blocks.IRON_ORE).requiresTool()));
    public static final Block URANIUM_ORE =
            register("uranium_ore", new Block(FabricBlockSettings.copyOf(Blocks.IRON_ORE).requiresTool()));

    public static final Block COPPER_CABLE_0 = registerNoItem(
            "copper_cable_0",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.COPPER, 0, "block/wiring/cable/copper_cable_0")
    );
    public static final Block COPPER_CABLE_1 = registerNoItem(
            "copper_cable_1",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.COPPER, 1, "block/wiring/cable/copper_cable_1_white")
    );

    public static final Block TIN_CABLE_0 = registerNoItem(
            "tin_cable_0",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.TIN, 0, "block/wiring/cable/tin_cable_0")
    );
    public static final Block TIN_CABLE_1 = registerNoItem(
            "tin_cable_1",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.TIN, 1, "block/wiring/cable/tin_cable_1_white")
    );

    public static final Block GOLD_CABLE_0 = registerNoItem(
            "gold_cable_0",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.GOLD, 0, "block/wiring/cable/gold_cable_0")
    );
    public static final Block GOLD_CABLE_1 = registerNoItem(
            "gold_cable_1",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.GOLD, 1, "block/wiring/cable/gold_cable_1_white")
    );
    public static final Block GOLD_CABLE_2 = registerNoItem(
            "gold_cable_2",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.GOLD, 2, "block/wiring/cable/gold_cable_2_white")
    );

    public static final Block IRON_CABLE_0 = registerNoItem(
            "iron_cable_0",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.IRON, 0, "block/wiring/cable/iron_cable_0")
    );
    public static final Block IRON_CABLE_1 = registerNoItem(
            "iron_cable_1",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.IRON, 1, "block/wiring/cable/iron_cable_1_white")
    );
    public static final Block IRON_CABLE_2 = registerNoItem(
            "iron_cable_2",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.IRON, 2, "block/wiring/cable/iron_cable_2_white")
    );
    public static final Block IRON_CABLE_3 = registerNoItem(
            "iron_cable_3",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.IRON, 3, "block/wiring/cable/iron_cable_3_white")
    );

    public static final Block GLASS_CABLE = registerNoItem(
            "glass_cable",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.GLASS),
                    CableKind.GLASS, 0, "block/wiring/cable/glass_cable_white")
    );

    public static final Block DETECTOR_CABLE = registerNoItem(
            "detector_cable",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.DETECTOR, 0, "block/wiring/cable/detector_cable")
    );

    public static final Block SPLITTER_CABLE = registerNoItem(
            "splitter_cable",
            new CableBlock(FabricBlockSettings.create().strength(0.2f).sounds(BlockSoundGroup.METAL),
                    CableKind.SPLITTER, 0, "block/wiring/cable/splitter_cable")
    );

    public static final Block RUBBER_LOG = register("rubber_log",
            new RubberLogBlock(FabricBlockSettings.copyOf(Blocks.OAK_LOG)));

    /**
     * Compatibility alias for code paths that already use the IL-style name.
     *
     * <p>Until the project fully migrates the registry id from {@code rubber_log}
     * to {@code rubber_wood}, keep this alias to avoid breaking compilation.
     * It refers to the same registered block instance.</p>
     */
    public static final Block RUBBER_WOOD = RUBBER_LOG;

    public static final Block RUBBER_LEAVES = register("rubber_leaves",
            new LeavesBlock(FabricBlockSettings.copyOf(Blocks.OAK_LEAVES)));
    public static final Block RUBBER_SAPLING = register("rubber_sapling",
            new SaplingBlock(new RubberSaplingGenerator(),
                    FabricBlockSettings.copyOf(Blocks.OAK_SAPLING)));

    private static Block register(String name, Block block) {
        return register(name, block, b -> new BlockItem(b, new Item.Settings()));
    }

    private static Block register(String name, Block block, Function<Block, Item> itemFactory) {
        Identifier id = new Identifier(IndustrialLegacy.MOD_ID, name);
        Block registered = Registry.register(Registries.BLOCK, id, block);
        // Matching block item
        Registry.register(Registries.ITEM, id, itemFactory.apply(registered));
        return registered;
    }

    /** Register a block without registering a matching BlockItem. */
    private static Block registerNoItem(String name, Block block) {
        Identifier id = new Identifier(IndustrialLegacy.MOD_ID, name);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static boolean isCable(Block block) {
        return block instanceof CableBlock;
    }

    /** Map cable item NBT (kind+insulation) to a cable block variant. */
    public static Block getCableBlock(CableKind kind, int insulation) {
        return switch (kind) {
            case COPPER -> (insulation <= 0 ? COPPER_CABLE_0 : COPPER_CABLE_1);
            case TIN -> (insulation <= 0 ? TIN_CABLE_0 : TIN_CABLE_1);
            case GOLD -> (insulation <= 0 ? GOLD_CABLE_0 : (insulation == 1 ? GOLD_CABLE_1 : GOLD_CABLE_2));
            case IRON -> switch (Math.max(0, Math.min(3, insulation))) {
                case 0 -> IRON_CABLE_0;
                case 1 -> IRON_CABLE_1;
                case 2 -> IRON_CABLE_2;
                default -> IRON_CABLE_3;
            };
            case GLASS -> GLASS_CABLE;
            case DETECTOR -> DETECTOR_CABLE;
            case SPLITTER -> SPLITTER_CABLE;
        };
    }

    public static void register() {
        // classload triggers static init
    }
}
