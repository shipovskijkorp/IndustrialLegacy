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

    public static void register() {
        // classload triggers static init
    }
}
