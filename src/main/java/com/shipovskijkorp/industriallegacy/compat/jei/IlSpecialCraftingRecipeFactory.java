package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.recipe.AdvancedReBatteryRecipe;
import com.shipovskijkorp.industriallegacy.recipe.BatBoxRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CesuRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CableVariantCraftingRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CoilRecipe;
import com.shipovskijkorp.industriallegacy.recipe.CutterCableRecipe;
import com.shipovskijkorp.industriallegacy.recipe.ElectricMotorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.ElectronicCircuitRecipe;
import com.shipovskijkorp.industriallegacy.recipe.HammerPlateRecipe;
import com.shipovskijkorp.industriallegacy.recipe.InsulateCableRecipe;
import com.shipovskijkorp.industriallegacy.recipe.LuminatorRecipe;
import com.shipovskijkorp.industriallegacy.recipe.MfeRecipe;
import com.shipovskijkorp.industriallegacy.recipe.ReBatteryRecipe;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

final class IlSpecialCraftingRecipeFactory {
    private IlSpecialCraftingRecipeFactory() {}

    static List<IlSpecialCraftingRecipe> create(RecipeManager manager) {
        List<IlSpecialCraftingRecipe> out = new ArrayList<>();
        for (Recipe<?> recipe : manager.values()) {
            if (recipe instanceof HammerPlateRecipe hammer) {
                out.add(shapeless(hammer.getId(), hammer.resultStack(), IlJeiUtil.ingredient(hammer.tool(), 1), IlJeiUtil.ingredient(hammer.material(), 1)));
            } else if (recipe instanceof CutterCableRecipe cutter) {
                out.add(shapeless(cutter.getId(), cutter.resultStack(), IlJeiUtil.ingredient(cutter.tool(), 1), IlJeiUtil.ingredient(cutter.material(), 1)));
            } else if (recipe instanceof InsulateCableRecipe insulate) {
                addInsulationRecipes(out, insulate);
            } else if (recipe instanceof CableVariantCraftingRecipe cableVariant) {
                addCableVariantRecipe(out, cableVariant);
            } else if (recipe instanceof ReBatteryRecipe reBattery) {
                out.add(shaped(reBattery.getId(), reBattery.resultStack(),
                        e(), cable(CableKind.TIN, 1), e(),
                        item(ModItems.TIN_CASING), item(Items.REDSTONE), item(ModItems.TIN_CASING),
                        item(ModItems.TIN_CASING), item(Items.REDSTONE), item(ModItems.TIN_CASING)));
            } else if (recipe instanceof AdvancedReBatteryRecipe advanced) {
                out.add(shaped(advanced.getId(), advanced.resultStack(),
                        cable(CableKind.COPPER, 1), item(ModItems.BRONZE_CASING), cable(CableKind.COPPER, 1),
                        item(ModItems.BRONZE_CASING), item(ModItems.SULFUR), item(ModItems.BRONZE_CASING),
                        item(ModItems.BRONZE_CASING), item(ModItems.LEAD_DUST), item(ModItems.BRONZE_CASING)));
            } else if (recipe instanceof BatBoxRecipe batBox) {
                out.add(shaped(batBox.getId(), batBox.resultStack(),
                        item(Items.OAK_PLANKS), cable(CableKind.TIN, 1), item(Items.OAK_PLANKS),
                        item(ModItems.RE_BATTERY), item(ModItems.RE_BATTERY), item(ModItems.RE_BATTERY),
                        item(Items.OAK_PLANKS), item(Items.OAK_PLANKS), item(Items.OAK_PLANKS)));
            } else if (recipe instanceof CesuRecipe cesu) {
                out.add(shaped(cesu.getId(), cesu.resultStack(),
                        item(ModItems.BRONZE_PLATE), cable(CableKind.COPPER, 1), item(ModItems.BRONZE_PLATE),
                        item(ModItems.ADVANCED_RE_BATTERY), item(ModItems.ADVANCED_RE_BATTERY), item(ModItems.ADVANCED_RE_BATTERY),
                        item(ModItems.BRONZE_PLATE), item(ModItems.BRONZE_PLATE), item(ModItems.BRONZE_PLATE)));
            } else if (recipe instanceof MfeRecipe mfe) {
                out.add(shaped(mfe.getId(), mfe.resultStack(),
                        cable(CableKind.GOLD, 2), item(ModItems.ENERGY_CRYSTAL), cable(CableKind.GOLD, 2),
                        item(ModItems.ENERGY_CRYSTAL), item(ModBlocks.MACHINE_CASING), item(ModItems.ENERGY_CRYSTAL),
                        cable(CableKind.GOLD, 2), item(ModItems.ENERGY_CRYSTAL), cable(CableKind.GOLD, 2)));
            } else if (recipe instanceof CoilRecipe coil) {
                out.add(shaped(coil.getId(), coil.resultStack(),
                        cable(CableKind.COPPER, 0), cable(CableKind.COPPER, 0), cable(CableKind.COPPER, 0),
                        cable(CableKind.COPPER, 0), item(Items.IRON_INGOT), cable(CableKind.COPPER, 0),
                        cable(CableKind.COPPER, 0), cable(CableKind.COPPER, 0), cable(CableKind.COPPER, 0)));
            } else if (recipe instanceof ElectricMotorRecipe motor) {
                out.add(shaped(IlJeiUtil.suffix(motor.getId(), "_a"), motor.resultStack(),
                        e(), item(ModItems.TIN_CASING), e(),
                        item(ModItems.COIL), item(Items.IRON_INGOT), item(ModItems.COIL),
                        e(), item(ModItems.TIN_CASING), e()));
                out.add(shaped(IlJeiUtil.suffix(motor.getId(), "_b"), motor.resultStack(),
                        e(), item(ModItems.COIL), e(),
                        item(ModItems.TIN_CASING), item(Items.IRON_INGOT), item(ModItems.TIN_CASING),
                        e(), item(ModItems.COIL), e()));
            } else if (recipe instanceof ElectronicCircuitRecipe circuit) {
                out.add(shaped(IlJeiUtil.suffix(circuit.getId(), "_a"), circuit.resultStack(),
                        cable(CableKind.COPPER, 1), cable(CableKind.COPPER, 1), cable(CableKind.COPPER, 1),
                        item(Items.REDSTONE), item(ModItems.IRON_PLATE), item(Items.REDSTONE),
                        cable(CableKind.COPPER, 1), cable(CableKind.COPPER, 1), cable(CableKind.COPPER, 1)));
                out.add(shaped(IlJeiUtil.suffix(circuit.getId(), "_b"), circuit.resultStack(),
                        cable(CableKind.COPPER, 1), item(Items.REDSTONE), cable(CableKind.COPPER, 1),
                        cable(CableKind.COPPER, 1), item(ModItems.IRON_PLATE), cable(CableKind.COPPER, 1),
                        cable(CableKind.COPPER, 1), item(Items.REDSTONE), cable(CableKind.COPPER, 1)));
            } else if (recipe instanceof LuminatorRecipe luminator) {
                out.add(shaped(luminator.getId(), luminator.resultStack(),
                        item(ModItems.IRON_CASING), cable(CableKind.COPPER, 1), item(ModItems.IRON_CASING),
                        item(Items.GLASS), cable(CableKind.TIN, 0), item(Items.GLASS),
                        item(Items.GLASS), item(Items.GLASS), item(Items.GLASS)));
            }
        }
        return out;
    }

    private static void addInsulationRecipes(List<IlSpecialCraftingRecipe> out, InsulateCableRecipe recipe) {
        for (CableKind kind : CableKind.values()) {
            for (int insulation = 0; insulation < kind.maxInsulation; insulation++) {
                Identifier id = IlJeiUtil.suffix(recipe.getId(), "_" + kind.id() + "_" + insulation + "_to_" + (insulation + 1));
                List<ItemStack> output = cable(kind, insulation + 1);
                if (!output.isEmpty()) {
                    out.add(shapeless(id, output.get(0), cable(kind, insulation), IlJeiUtil.ingredient(recipe.material(), 1)));
                }
            }
        }
    }

    private static void addCableVariantRecipe(List<IlSpecialCraftingRecipe> out, CableVariantCraftingRecipe recipe) {
        if (recipe.resultKind() == CableKind.DETECTOR) {
            out.add(shaped(recipe.getId(), recipe.resultStack(),
                    e(), item(ModItems.ELECTRONIC_CIRCUIT), e(),
                    item(Items.REDSTONE), cable(CableKind.IRON, 3), item(Items.REDSTONE),
                    e(), item(Items.REDSTONE), e()));
        } else if (recipe.resultKind() == CableKind.SPLITTER) {
            out.add(shaped(recipe.getId(), recipe.resultStack(),
                    e(), item(Items.REDSTONE), e(),
                    cable(CableKind.IRON, 3), item(Items.LEVER), cable(CableKind.IRON, 3),
                    e(), item(Items.REDSTONE), e()));
        }
    }

    @SafeVarargs
    private static IlSpecialCraftingRecipe shaped(Identifier id, ItemStack output, List<ItemStack>... inputs) {
        return new IlSpecialCraftingRecipe(id, List.of(inputs), output, false);
    }

    @SafeVarargs
    private static IlSpecialCraftingRecipe shapeless(Identifier id, ItemStack output, List<ItemStack>... inputs) {
        List<List<ItemStack>> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(List.of());
        }
        for (int i = 0; i < inputs.length && i < 9; i++) {
            grid.set(i, inputs[i]);
        }
        return new IlSpecialCraftingRecipe(id, grid, output, true);
    }

    private static List<ItemStack> e() {
        return List.of();
    }

    private static List<ItemStack> item(net.minecraft.item.ItemConvertible item) {
        return IlJeiUtil.item(item);
    }

    private static List<ItemStack> cable(CableKind kind, int insulation) {
        return List.of(IlJeiUtil.cable(kind, insulation));
    }
}
