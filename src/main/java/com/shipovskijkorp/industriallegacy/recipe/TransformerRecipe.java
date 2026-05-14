package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * IC2 transformer crafting.
 *
 * <p>Vanilla shaped recipes can't express Industrial Legacy cable variants because cables are
 * one NBT-backed item. This recipe keeps the IC2 shaped patterns while matching the exact cable
 * kind/insulation required by the original IC2 recipes.</p>
 */
public final class TransformerRecipe extends SpecialCraftingRecipe {
    public enum Variant {
        LV("lv", CableKind.TIN, 1),
        MV("mv", CableKind.COPPER, 1),
        HV("hv", CableKind.GOLD, 2),
        EV("ev", CableKind.IRON, 3);

        private final String id;
        private final CableKind cableKind;
        private final int insulation;

        Variant(String id, CableKind cableKind, int insulation) {
            this.id = id;
            this.cableKind = cableKind;
            this.insulation = insulation;
        }

        public String id() {
            return id;
        }

        public CableKind cableKind() {
            return cableKind;
        }

        public int insulation() {
            return insulation;
        }

        public static Variant fromId(String id) {
            if (id != null) {
                for (Variant variant : values()) {
                    if (variant.id.equals(id)) return variant;
                }
            }
            return LV;
        }
    }

    private final Variant variant;
    private final ItemStack result;

    public TransformerRecipe(Identifier id, CraftingRecipeCategory category, Variant variant, ItemStack result) {
        super(id, category);
        this.variant = variant;
        this.result = result;
    }

    public Variant variant() {
        return variant;
    }

    public ItemStack resultStack() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory inv, World world) {
        if (inv.getWidth() != 3 || inv.getHeight() != 3) return false;
        return switch (variant) {
            case LV -> matchesLv(inv);
            case MV -> matchesMv(inv);
            case HV -> matchesHv(inv);
            case EV -> matchesEv(inv);
        };
    }

    /** IC2: "PCP|PcP|PCP", C = insulated tin cable, c = coil. */
    private static boolean matchesLv(RecipeInputInventory inv) {
        return isPlanks(inv.getStack(0)) && isCable(inv.getStack(1), CableKind.TIN, 1) && isPlanks(inv.getStack(2))
                && isPlanks(inv.getStack(3)) && inv.getStack(4).isOf(ModItems.COIL) && isPlanks(inv.getStack(5))
                && isPlanks(inv.getStack(6)) && isCable(inv.getStack(7), CableKind.TIN, 1) && isPlanks(inv.getStack(8));
    }

    /** IC2: "C|M|C"; allow any column in the 3x3 crafting grid, like vanilla shaped recipes. */
    private static boolean matchesMv(RecipeInputInventory inv) {
        for (int x = 0; x < 3; x++) {
            if (columnMatchesMv(inv, x)) return true;
        }
        return false;
    }

    private static boolean columnMatchesMv(RecipeInputInventory inv, int x) {
        for (int slot = 0; slot < 9; slot++) {
            int slotX = slot % 3;
            ItemStack stack = inv.getStack(slot);

            if (slotX != x) {
                if (!stack.isEmpty()) return false;
                continue;
            }

            int y = slot / 3;
            if (y == 0 || y == 2) {
                if (!isCable(stack, CableKind.COPPER, 1)) return false;
            } else {
                if (!stack.isOf(ModBlocks.MACHINE_CASING.asItem())) return false;
            }
        }
        return true;
    }

    /** IC2: " c |CEB| c ". */
    private static boolean matchesHv(RecipeInputInventory inv) {
        return inv.getStack(0).isEmpty() && isCable(inv.getStack(1), CableKind.GOLD, 2) && inv.getStack(2).isEmpty()
                && inv.getStack(3).isOf(ModItems.ELECTRONIC_CIRCUIT) && inv.getStack(4).isOf(ModBlocks.MV_TRANSFORMER.asItem()) && inv.getStack(5).isOf(ModItems.ADVANCED_RE_BATTERY)
                && inv.getStack(6).isEmpty() && isCable(inv.getStack(7), CableKind.GOLD, 2) && inv.getStack(8).isEmpty();
    }

    /** IC2: " c |CED| c ". */
    private static boolean matchesEv(RecipeInputInventory inv) {
        return inv.getStack(0).isEmpty() && isCable(inv.getStack(1), CableKind.IRON, 3) && inv.getStack(2).isEmpty()
                && inv.getStack(3).isOf(ModItems.ADVANCED_CIRCUIT) && inv.getStack(4).isOf(ModBlocks.HV_TRANSFORMER.asItem()) && inv.getStack(5).isOf(ModItems.LAPOTRON_CRYSTAL)
                && inv.getStack(6).isEmpty() && isCable(inv.getStack(7), CableKind.IRON, 3) && inv.getStack(8).isEmpty();
    }

    private static boolean isPlanks(ItemStack stack) {
        return !stack.isEmpty() && stack.isIn(ItemTags.PLANKS);
    }

    private static boolean isCable(ItemStack stack, CableKind kind, int insulation) {
        return !stack.isEmpty()
                && stack.getItem() instanceof CableItem
                && CableItem.getKind(stack) == kind
                && CableItem.getInsulation(stack) == insulation;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inv, net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result.copy();
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(9, Ingredient.EMPTY);
        Ingredient cable = Ingredient.ofStacks(CableItem.createStack(ModItems.CABLE, variant.cableKind(), variant.insulation()));

        switch (variant) {
            case LV -> {
                Ingredient planks = Ingredient.fromTag(ItemTags.PLANKS);
                ingredients.set(0, planks);
                ingredients.set(1, cable);
                ingredients.set(2, planks);
                ingredients.set(3, planks);
                ingredients.set(4, Ingredient.ofItems(ModItems.COIL));
                ingredients.set(5, planks);
                ingredients.set(6, planks);
                ingredients.set(7, cable);
                ingredients.set(8, planks);
            }
            case MV -> {
                ingredients.set(1, cable);
                ingredients.set(4, Ingredient.ofItems(ModBlocks.MACHINE_CASING.asItem()));
                ingredients.set(7, cable);
            }
            case HV -> {
                ingredients.set(1, cable);
                ingredients.set(3, Ingredient.ofItems(ModItems.ELECTRONIC_CIRCUIT));
                ingredients.set(4, Ingredient.ofItems(ModBlocks.MV_TRANSFORMER.asItem()));
                ingredients.set(5, Ingredient.ofItems(ModItems.ADVANCED_RE_BATTERY));
                ingredients.set(7, cable);
            }
            case EV -> {
                ingredients.set(1, cable);
                ingredients.set(3, Ingredient.ofItems(ModItems.ADVANCED_CIRCUIT));
                ingredients.set(4, Ingredient.ofItems(ModBlocks.HV_TRANSFORMER.asItem()));
                ingredients.set(5, Ingredient.ofItems(ModItems.LAPOTRON_CRYSTAL));
                ingredients.set(7, cable);
            }
        }

        return ingredients;
    }

    @Override
    public boolean fits(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getOutput(net.minecraft.registry.DynamicRegistryManager registryManager) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TRANSFORMER_SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}
