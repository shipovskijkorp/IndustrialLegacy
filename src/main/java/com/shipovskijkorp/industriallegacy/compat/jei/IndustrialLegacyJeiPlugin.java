package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.client.screen.CannerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.CompressorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.ElectricFurnaceScreen;
import com.shipovskijkorp.industriallegacy.client.screen.ExtractorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.InductionFurnaceScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MaceratorScreen;
import com.shipovskijkorp.industriallegacy.client.screen.MetalFormerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.OreWashingPlantScreen;
import com.shipovskijkorp.industriallegacy.client.screen.RecyclerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.SolidCannerScreen;
import com.shipovskijkorp.industriallegacy.client.screen.ThermalCentrifugeScreen;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.recipe.CompressorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.ExtractorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MaceratorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MachineRecipeManager;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;

import java.util.List;

@JeiPlugin
public final class IndustrialLegacyJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return IlJeiUtil.id("jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        IIngredientSubtypeInterpreter<ItemStack> cableSubtype = (stack, context) -> CableItem.getKind(stack).id()
                + ":" + CableItem.getInsulation(stack)
                + ":" + CableItem.getColor(stack)
                + ":" + CableItem.getOxidation(stack);
        registration.registerSubtypeInterpreter(ModItems.CABLE, cableSubtype);

        IIngredientSubtypeInterpreter<ItemStack> fluidCellSubtype = (stack, context) -> UniversalFluidCellItem.getFluid(stack).id;
        registration.registerSubtypeInterpreter(ModItems.FLUID_CELL, fluidCellSubtype);

        IIngredientSubtypeInterpreter<ItemStack> ignoreEnergy = (stack, context) -> IIngredientSubtypeInterpreter.NONE;
        ignore(registration, ignoreEnergy,
                ModItems.RE_BATTERY,
                ModItems.ADVANCED_RE_BATTERY,
                ModItems.ENERGY_CRYSTAL,
                ModItems.LAPOTRON_CRYSTAL,
                ModItems.DRILL,
                ModItems.DIAMOND_DRILL,
                ModItems.IRIDIUM_DRILL,
                ModItems.CHAINSAW,
                ModItems.ELECTRIC_TREETAP,
                ModItems.ELECTRIC_HOE,
                ModItems.ELECTRIC_WRENCH,
                ModItems.JETPACK_ELECTRIC,
                ModItems.NANO_HELMET,
                ModItems.NANO_CHESTPLATE,
                ModItems.NANO_LEGGINGS,
                ModItems.NANO_BOOTS,
                ModItems.QUANTUM_HELMET,
                ModItems.QUANTUM_CHESTPLATE,
                ModItems.QUANTUM_LEGGINGS,
                ModItems.QUANTUM_BOOTS,
                ModItems.NANO_SABER,
                ModItems.MINING_LASER,
                ModBlocks.GENERATOR.asItem(),
                ModBlocks.GEO_GENERATOR.asItem(),
                ModBlocks.SOLAR_PANEL.asItem(),
                ModBlocks.RT_GENERATOR.asItem(),
                ModBlocks.BATBOX.asItem(),
                ModBlocks.CESU.asItem(),
                ModBlocks.MFE.asItem(),
                ModBlocks.MFSU.asItem(),
                ModBlocks.CHARGEPAD_BATBOX.asItem(),
                ModBlocks.CHARGEPAD_CESU.asItem(),
                ModBlocks.CHARGEPAD_MFE.asItem(),
                ModBlocks.CHARGEPAD_MFSU.asItem(),
                ModBlocks.MACERATOR.asItem(),
                ModBlocks.COMPRESSOR.asItem(),
                ModBlocks.EXTRACTOR.asItem(),
                ModBlocks.RECYCLER.asItem(),
                ModBlocks.ELECTRIC_FURNACE.asItem(),
                ModBlocks.INDUCTION_FURNACE.asItem(),
                ModBlocks.METAL_FORMER.asItem(),
                ModBlocks.SOLID_CANNER.asItem(),
                ModBlocks.CANNER.asItem(),
                ModBlocks.THERMAL_CENTRIFUGE.asItem(),
                ModBlocks.ORE_WASHING_PLANT.asItem(),
                ModBlocks.LV_TRANSFORMER.asItem(),
                ModBlocks.MV_TRANSFORMER.asItem(),
                ModBlocks.HV_TRANSFORMER.asItem(),
                ModBlocks.EV_TRANSFORMER.asItem()
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new SimpleMachineJeiCategory<>(gui, IlJeiRecipeTypes.MACERATOR,
                        "jei.industrial_legacy.macerator", stack(ModBlocks.MACERATOR),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getIngredientCount(), recipe -> recipe.getOutputStack(),
                        SimpleMachineJeiCategory.Progress.CRUSH),
                new SimpleMachineJeiCategory<>(gui, IlJeiRecipeTypes.COMPRESSOR,
                        "jei.industrial_legacy.compressor", stack(ModBlocks.COMPRESSOR),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getIngredientCount(), recipe -> recipe.getOutputStack(),
                        SimpleMachineJeiCategory.Progress.TRIANGLE),
                new SimpleMachineJeiCategory<>(gui, IlJeiRecipeTypes.EXTRACTOR,
                        "jei.industrial_legacy.extractor", stack(ModBlocks.EXTRACTOR),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getIngredientCount(), recipe -> recipe.getOutputStack(),
                        SimpleMachineJeiCategory.Progress.DROP),
                new RecyclerJeiCategory(gui, stack(ModBlocks.RECYCLER)),
                new CanningJeiCategory(gui, stack(ModBlocks.CANNER)),
                new CanningFluidJeiCategory(gui, CanningFluidJeiCategory.Kind.EMPTY_LIQUID, stack(ModBlocks.CANNER)),
                new CanningFluidJeiCategory(gui, CanningFluidJeiCategory.Kind.BOTTLE_LIQUID, stack(ModBlocks.CANNER)),
                new CanningEnrichJeiCategory(gui, stack(ModBlocks.CANNER)),
                new SolidCanningJeiCategory(gui, stack(ModBlocks.SOLID_CANNER)),
                new ThermalCentrifugeJeiCategory(gui, stack(ModBlocks.THERMAL_CENTRIFUGE)),
                new OreWashingPlantJeiCategory(gui, stack(ModBlocks.ORE_WASHING_PLANT)),
                new MetalFormerJeiCategory(gui, MetalFormerJeiCategory.Mode.EXTRUDING, stack(ModBlocks.METAL_FORMER),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getInputCount(), recipe -> recipe.getOutputStack()),
                new MetalFormerJeiCategory(gui, MetalFormerJeiCategory.Mode.ROLLING, stack(ModBlocks.METAL_FORMER),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getInputCount(), recipe -> recipe.getOutputStack()),
                new MetalFormerJeiCategory(gui, MetalFormerJeiCategory.Mode.CUTTING, stack(ModBlocks.METAL_FORMER),
                        recipe -> recipe.getIngredient(), recipe -> recipe.getInputCount(), recipe -> recipe.getOutputStack()),
                new SpecialCraftingJeiCategory(gui),
                new ScrapBoxJeiCategory(gui)
        );
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // IC2 guidef recipe buttons are transparent overlays on machine progress gauges.
        // JEI renders the hover hint and opens the matching category from these areas.
        registration.addRecipeClickArea(MaceratorScreen.class, 80, 38, 21, 11, IlJeiRecipeTypes.MACERATOR);
        registration.addRecipeClickArea(CompressorScreen.class, 80, 35, 22, 15, IlJeiRecipeTypes.COMPRESSOR);
        registration.addRecipeClickArea(ExtractorScreen.class, 80, 35, 22, 15, IlJeiRecipeTypes.EXTRACTOR);
        registration.addRecipeClickArea(RecyclerScreen.class, 80, 35, 18, 15, IlJeiRecipeTypes.RECYCLER);
        registration.addRecipeClickArea(CannerScreen.class, CannerScreen.RECIPE_BUTTON_X, CannerScreen.RECIPE_BUTTON_Y, CannerScreen.RECIPE_BUTTON_W, CannerScreen.RECIPE_BUTTON_H,
                IlJeiRecipeTypes.CANNING, IlJeiRecipeTypes.CANNING_EMPTY_LIQUID, IlJeiRecipeTypes.CANNING_BOTTLE_LIQUID, IlJeiRecipeTypes.CANNING_ENRICH);
        registration.addRecipeClickArea(ElectricFurnaceScreen.class, 80, 35, 22, 15, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(InductionFurnaceScreen.class, 81, 35, 22, 15, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(SolidCannerScreen.class, 88, 35, 22, 15, IlJeiRecipeTypes.SOLID_CANNING);
        registration.addRecipeClickArea(ThermalCentrifugeScreen.class, 84, 25, 3, 28, IlJeiRecipeTypes.THERMAL_CENTRIFUGE);
        registration.addRecipeClickArea(OreWashingPlantScreen.class, OreWashingPlantScreen.RECIPE_CLICK_X, OreWashingPlantScreen.RECIPE_CLICK_Y, OreWashingPlantScreen.RECIPE_CLICK_W, OreWashingPlantScreen.RECIPE_CLICK_H, IlJeiRecipeTypes.ORE_WASHING);

        registration.addRecipeClickArea(MetalFormerScreen.class, 52, 39, 46, 9,
                IlJeiRecipeTypes.METAL_FORMER_EXTRUDING,
                IlJeiRecipeTypes.METAL_FORMER_ROLLING,
                IlJeiRecipeTypes.METAL_FORMER_CUTTING);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        addRecipes(registration, IlJeiRecipeTypes.MACERATOR, MachineRecipeManager.getMaceratorRecipes().stream()
                .filter(IndustrialLegacyJeiPlugin::hasVisibleOutput)
                .filter(recipe -> !isDuplicateLegacyOreMaceratorRecipe(recipe))
                .toList());
        addRecipes(registration, IlJeiRecipeTypes.COMPRESSOR, MachineRecipeManager.getCompressorRecipes().stream()
                .filter(IndustrialLegacyJeiPlugin::hasVisibleOutput)
                .toList());

        List<com.shipovskijkorp.industriallegacy.recipe.CanningRecipe> canningRecipes = MachineRecipeManager.getCanningRecipes();
        addRecipes(registration, IlJeiRecipeTypes.CANNING, canningRecipes);
        registration.addRecipes(IlJeiRecipeTypes.CANNING_EMPTY_LIQUID, MachineRecipeManager.getCanningEmptyLiquidRecipes());
        registration.addRecipes(IlJeiRecipeTypes.CANNING_BOTTLE_LIQUID, MachineRecipeManager.getCanningBottleLiquidRecipes());
        registration.addRecipes(IlJeiRecipeTypes.CANNING_ENRICH, MachineRecipeManager.getCanningEnrichRecipes());
        addRecipes(registration, IlJeiRecipeTypes.SOLID_CANNING, canningRecipes);
        addRecipes(registration, IlJeiRecipeTypes.METAL_FORMER_EXTRUDING, MachineRecipeManager.getMetalFormerExtrudingRecipes());
        addRecipes(registration, IlJeiRecipeTypes.METAL_FORMER_ROLLING, MachineRecipeManager.getMetalFormerRollingRecipes());
        addRecipes(registration, IlJeiRecipeTypes.METAL_FORMER_CUTTING, MachineRecipeManager.getMetalFormerCuttingRecipes());
        addRecipes(registration, IlJeiRecipeTypes.THERMAL_CENTRIFUGE, MachineRecipeManager.getThermalCentrifugeRecipes());
        addRecipes(registration, IlJeiRecipeTypes.ORE_WASHING, MachineRecipeManager.getOreWashingRecipes());

        RecipeManager manager = recipeManager();
        if (manager == null) {
            return;
        }

        addRecipes(registration, IlJeiRecipeTypes.EXTRACTOR, manager.listAllOfType(ModRecipes.EXTRACTOR_TYPE).stream()
                .filter(IndustrialLegacyJeiPlugin::hasVisibleOutput)
                .toList());
        registration.addRecipes(IlJeiRecipeTypes.RECYCLER, List.of(RecyclerJeiRecipe.create()));
        registration.addRecipes(IlJeiRecipeTypes.SPECIAL_CRAFTING, IlSpecialCraftingRecipeFactory.create(manager));
        registration.addRecipes(IlJeiRecipeTypes.SCRAP_BOX, ScrapBoxJeiRecipeFactory.create());
    }

    private static boolean hasVisibleOutput(MaceratorRecipe recipe) {
        return !recipe.getOutputStack().isEmpty();
    }

    private static boolean hasVisibleOutput(CompressorRecipe recipe) {
        return !recipe.getOutputStack().isEmpty();
    }

    private static boolean hasVisibleOutput(ExtractorRecipe recipe) {
        return !recipe.getOutputStack().isEmpty();
    }

    private static boolean isDuplicateLegacyOreMaceratorRecipe(MaceratorRecipe recipe) {
        String path = recipe.getId().getPath();
        return path.startsWith("macerator/oredict_ore") && path.contains("_crushed_ore");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(stack(ModBlocks.MACERATOR), IlJeiRecipeTypes.MACERATOR);
        registration.addRecipeCatalyst(stack(ModBlocks.COMPRESSOR), IlJeiRecipeTypes.COMPRESSOR);
        registration.addRecipeCatalyst(stack(ModBlocks.EXTRACTOR), IlJeiRecipeTypes.EXTRACTOR);
        registration.addRecipeCatalyst(stack(ModBlocks.RECYCLER), IlJeiRecipeTypes.RECYCLER);
        registration.addRecipeCatalyst(stack(ModBlocks.CANNER), IlJeiRecipeTypes.CANNING, IlJeiRecipeTypes.CANNING_EMPTY_LIQUID, IlJeiRecipeTypes.CANNING_BOTTLE_LIQUID, IlJeiRecipeTypes.CANNING_ENRICH);
        registration.addRecipeCatalyst(stack(ModBlocks.SOLID_CANNER), IlJeiRecipeTypes.SOLID_CANNING);
        registration.addRecipeCatalyst(stack(ModBlocks.THERMAL_CENTRIFUGE), IlJeiRecipeTypes.THERMAL_CENTRIFUGE);
        registration.addRecipeCatalyst(stack(ModBlocks.ORE_WASHING_PLANT), IlJeiRecipeTypes.ORE_WASHING);
        registration.addRecipeCatalyst(stack(ModBlocks.METAL_FORMER),
                IlJeiRecipeTypes.METAL_FORMER_EXTRUDING,
                IlJeiRecipeTypes.METAL_FORMER_ROLLING,
                IlJeiRecipeTypes.METAL_FORMER_CUTTING);
        registration.addRecipeCatalyst(stack(ModBlocks.ELECTRIC_FURNACE), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(stack(ModBlocks.INDUCTION_FURNACE), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(stack(ModBlocks.IRON_FURNACE), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(stack(ModBlocks.IRON_FURNACE), RecipeTypes.FUELING);
        registration.addRecipeCatalyst(stack(ModBlocks.GENERATOR), RecipeTypes.FUELING);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), IlJeiRecipeTypes.SPECIAL_CRAFTING);
        registration.addRecipeCatalyst(stack(ModItems.FORGE_HAMMER), IlJeiRecipeTypes.SPECIAL_CRAFTING);
        registration.addRecipeCatalyst(stack(ModItems.CUTTER), IlJeiRecipeTypes.SPECIAL_CRAFTING);
        registration.addRecipeCatalyst(stack(ModItems.PAINTER), IlJeiRecipeTypes.SPECIAL_CRAFTING);
        registration.addRecipeCatalyst(stack(ModItems.SCRAP_BOX), IlJeiRecipeTypes.SCRAP_BOX);
    }

    private static RecipeManager recipeManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world == null ? null : client.world.getRecipeManager();
    }

    private static <T extends Recipe<?>> void addRecipes(IRecipeRegistration registration, RecipeType<T> jeiType, List<T> recipes) {
        registration.addRecipes(jeiType, recipes);
    }

    private static ItemStack stack(ItemConvertible item) {
        return new ItemStack(item);
    }

    private static void ignore(ISubtypeRegistration registration, IIngredientSubtypeInterpreter<ItemStack> interpreter, Item... items) {
        for (Item item : items) {
            registration.registerSubtypeInterpreter(item, interpreter);
        }
    }
}
