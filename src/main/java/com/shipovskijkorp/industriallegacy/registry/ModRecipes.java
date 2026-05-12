package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.recipe.*;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Recipe types/serializers.
 */
public final class ModRecipes {
    private ModRecipes() {}

    public static final RecipeType<CutterCableRecipe> CUTTER_CABLE_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "cutter_cable"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":cutter_cable";
                }
            }
    );

    public static final RecipeSerializer<CutterCableRecipe> CUTTER_CABLE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "cutter_cable"),
            new CutterCableRecipeSerializer()
    );

    public static final RecipeType<InsulateCableRecipe> INSULATE_CABLE_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "insulate_cable"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":insulate_cable";
                }
            }
    );

    public static final RecipeSerializer<ElectronicCircuitRecipe> ELECTRONIC_CIRCUIT_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "electronic_circuit"),
            new ElectronicCircuitRecipeSerializer()
    );

    public static final RecipeSerializer<CoilRecipe> COIL_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "coil"),
            new CoilRecipeSerializer()
    );

    public static final RecipeSerializer<ElectricMotorRecipe> ELECTRIC_MOTOR_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "electric_motor"),
            new ElectricMotorRecipeSerializer()
    );

    public static final RecipeSerializer<InsulateCableRecipe> INSULATE_CABLE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "insulate_cable"),
            new InsulateCableRecipeSerializer()
    );

    public static final RecipeType<HammerPlateRecipe> HAMMER_PLATE_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "hammer_plate"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":hammer_plate";
                }
            }
    );

    public static final RecipeSerializer<HammerPlateRecipe> HAMMER_PLATE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "hammer_plate"),
            new HammerPlateRecipeSerializer()
    );

    public static final RecipeSerializer<ReBatteryRecipe> RE_BATTERY_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "re_battery"),
            new ReBatteryRecipeSerializer()
    );

    public static final RecipeSerializer<AdvancedReBatteryRecipe> ADVANCED_RE_BATTERY_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "advanced_re_battery"),
            new AdvancedReBatteryRecipeSerializer()
    );

    public static final RecipeSerializer<LuminatorRecipe> LUMINATOR_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "luminator"),
            new LuminatorRecipeSerializer()
    );

    public static final RecipeSerializer<CableVariantCraftingRecipe> CABLE_VARIANT_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "cable_variant"),
            new CableVariantCraftingRecipeSerializer()
    );


    public static final RecipeSerializer<BatBoxRecipe> BATBOX_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "batbox"),
            new BatBoxRecipeSerializer()
    );

    public static final RecipeSerializer<CesuRecipe> CESU_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "cesu"),
            new CesuRecipeSerializer()
    );

    

    public static final RecipeSerializer<MfeRecipe> MFE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "mfe"),
            new MfeRecipeSerializer()
    );
public static final RecipeType<MaceratorRecipe> MACERATOR_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":macerator";
                }
            }
    );

    public static final RecipeSerializer<MaceratorRecipe> MACERATOR_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "macerator"),
            new MaceratorRecipeSerializer()
    );

    public static final RecipeType<CompressorRecipe> COMPRESSOR_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":compressor";
                }
            }
    );

    public static final RecipeSerializer<CompressorRecipe> COMPRESSOR_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "compressor"),
            new CompressorRecipeSerializer()
    );


    public static final RecipeType<CanningRecipe> CANNING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "canning"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":canning";
                }
            }
    );

    public static final RecipeSerializer<CanningRecipe> CANNING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "canning"),
            new CanningRecipeSerializer()
    );

    public static final RecipeType<ThermalCentrifugeRecipe> THERMAL_CENTRIFUGE_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "thermal_centrifuge"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":thermal_centrifuge";
                }
            }
    );

    public static final RecipeSerializer<ThermalCentrifugeRecipe> THERMAL_CENTRIFUGE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "thermal_centrifuge"),
            new ThermalCentrifugeRecipeSerializer()
    );

    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_EXTRUDING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_extruding"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":metal_former_extruding";
                }
            }
    );

    public static final RecipeSerializer<MetalFormerRecipe> METAL_FORMER_EXTRUDING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_extruding"),
            new MetalFormerRecipeSerializer(METAL_FORMER_EXTRUDING_TYPE)
    );

    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_ROLLING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_rolling"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":metal_former_rolling";
                }
            }
    );

    public static final RecipeSerializer<MetalFormerRecipe> METAL_FORMER_ROLLING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_rolling"),
            new MetalFormerRecipeSerializer(METAL_FORMER_ROLLING_TYPE)
    );

    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_CUTTING_TYPE = Registry.register(
            Registries.RECIPE_TYPE,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_cutting"),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return IndustrialLegacy.MOD_ID + ":metal_former_cutting";
                }
            }
    );

    public static final RecipeSerializer<MetalFormerRecipe> METAL_FORMER_CUTTING_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER,
            new Identifier(IndustrialLegacy.MOD_ID, "metal_former_cutting"),
            new MetalFormerRecipeSerializer(METAL_FORMER_CUTTING_TYPE)
    );

    public static RecipeType<MetalFormerRecipe> typeForMode(com.shipovskijkorp.industriallegacy.block.entity.MetalFormerBlockEntity.Mode mode) {
        return switch (mode) {
            case ROLLING -> METAL_FORMER_ROLLING_TYPE;
            case CUTTING -> METAL_FORMER_CUTTING_TYPE;
            default -> METAL_FORMER_EXTRUDING_TYPE;
        };
    }

    public static void register() {
        // classload triggers static init
    }
}
