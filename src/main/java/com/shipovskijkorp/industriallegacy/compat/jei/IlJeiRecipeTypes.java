package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.recipe.CanningRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CompressorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MaceratorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MetalFormerRecipe;
import com.shipovskijkorp.industriallegacy.recipe.ThermalCentrifugeRecipe;
import mezz.jei.api.recipe.RecipeType;

public final class IlJeiRecipeTypes {
    private IlJeiRecipeTypes() {}

    public static final RecipeType<MaceratorRecipe> MACERATOR = RecipeType.create(IndustrialLegacy.MOD_ID, "macerator", MaceratorRecipe.class);
    public static final RecipeType<CompressorRecipe> COMPRESSOR = RecipeType.create(IndustrialLegacy.MOD_ID, "compressor", CompressorRecipe.class);
    public static final RecipeType<RecyclerJeiRecipe> RECYCLER = RecipeType.create(IndustrialLegacy.MOD_ID, "recycler", RecyclerJeiRecipe.class);
    public static final RecipeType<CanningRecipe> CANNING = RecipeType.create(IndustrialLegacy.MOD_ID, "canning", CanningRecipe.class);
    public static final RecipeType<CanningRecipe> SOLID_CANNING = RecipeType.create(IndustrialLegacy.MOD_ID, "solid_canning", CanningRecipe.class);
    public static final RecipeType<ThermalCentrifugeRecipe> THERMAL_CENTRIFUGE = RecipeType.create(IndustrialLegacy.MOD_ID, "thermal_centrifuge", ThermalCentrifugeRecipe.class);
    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_EXTRUDING = RecipeType.create(IndustrialLegacy.MOD_ID, "metal_former_extruding", MetalFormerRecipe.class);
    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_ROLLING = RecipeType.create(IndustrialLegacy.MOD_ID, "metal_former_rolling", MetalFormerRecipe.class);
    public static final RecipeType<MetalFormerRecipe> METAL_FORMER_CUTTING = RecipeType.create(IndustrialLegacy.MOD_ID, "metal_former_cutting", MetalFormerRecipe.class);
    public static final RecipeType<IlSpecialCraftingRecipe> SPECIAL_CRAFTING = RecipeType.create(IndustrialLegacy.MOD_ID, "special_crafting", IlSpecialCraftingRecipe.class);
    public static final RecipeType<ScrapBoxJeiRecipe> SCRAP_BOX = RecipeType.create(IndustrialLegacy.MOD_ID, "scrap_box", ScrapBoxJeiRecipe.class);
}
