package com.shipovskijkorp.industriallegacy.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.JsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class IlCraftingIngredient {
    private static final IlCraftingIngredient EMPTY = new IlCraftingIngredient(List.of());

    private final List<Alt> alternatives;

    private IlCraftingIngredient(List<Alt> alternatives) {
        this.alternatives = List.copyOf(alternatives);
    }

    public static IlCraftingIngredient empty() {
        return EMPTY;
    }

    public static IlCraftingIngredient of(Ingredient ingredient) {
        if (ingredient == Ingredient.EMPTY) return EMPTY;
        return new IlCraftingIngredient(List.of(Alt.ingredient(ingredient)));
    }

    public static IlCraftingIngredient ofAlternatives(List<IlCraftingIngredient> ingredients) {
        List<Alt> alternatives = new ArrayList<>();
        for (IlCraftingIngredient ingredient : ingredients) {
            if (ingredient != null && !ingredient.isEmpty()) {
                alternatives.addAll(ingredient.alternatives);
            }
        }
        return alternatives.isEmpty() ? EMPTY : new IlCraftingIngredient(alternatives);
    }

    public static IlCraftingIngredient cable(CableKind kind, int insulation) {
        return new IlCraftingIngredient(List.of(Alt.cable(kind, insulation)));
    }

    public static IlCraftingIngredient fluidCell(UniversalFluidCellItem.CellFluid fluid) {
        return new IlCraftingIngredient(List.of(Alt.fluidCell(fluid)));
    }

    public static IlCraftingIngredient fluidContainer(UniversalFluidCellItem.CellFluid fluid, Ingredient fallback) {
        List<Alt> alternatives = new ArrayList<>();
        alternatives.add(Alt.fluidCell(fluid));
        if (fallback != Ingredient.EMPTY) alternatives.add(Alt.ingredient(fallback));
        return new IlCraftingIngredient(alternatives);
    }

    public boolean isEmpty() {
        return alternatives.isEmpty();
    }

    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return isEmpty();
        for (Alt alternative : alternatives) {
            if (alternative.test(stack)) return true;
        }
        return false;
    }

    public boolean isVanillaOnly() {
        return alternatives.size() == 1 && alternatives.get(0).kind == AltKind.INGREDIENT;
    }

    public Ingredient asVanillaIngredient() {
        return isVanillaOnly() ? alternatives.get(0).ingredient : Ingredient.EMPTY;
    }

    public List<ItemStack> previewStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Alt alternative : alternatives) {
            stacks.addAll(alternative.previewStacks());
        }
        return stacks;
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(alternatives.size());
        for (Alt alternative : alternatives) {
            alternative.write(buf);
        }
    }

    public static IlCraftingIngredient read(PacketByteBuf buf) {
        int size = buf.readVarInt();
        if (size <= 0) return EMPTY;
        List<Alt> alternatives = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            alternatives.add(Alt.read(buf));
        }
        return new IlCraftingIngredient(alternatives);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();
        for (Alt alternative : alternatives) {
            array.add(alternative.toJson());
        }
        root.add("alternatives", array);
        return root;
    }

    public static IlCraftingIngredient fromJson(JsonElement element) {
        if (element == null || element.isJsonNull()) return EMPTY;
        JsonObject root = element.getAsJsonObject();
        if (root.has("alternatives")) {
            JsonArray array = JsonHelper.getArray(root, "alternatives");
            List<Alt> alternatives = new ArrayList<>();
            for (JsonElement alt : array) {
                alternatives.add(Alt.fromJson(alt.getAsJsonObject()));
            }
            return alternatives.isEmpty() ? EMPTY : new IlCraftingIngredient(alternatives);
        }
        return of(Ingredient.fromJson(element));
    }

    private enum AltKind {
        INGREDIENT,
        CABLE,
        FLUID_CELL
    }

    private static final class Alt {
        private final AltKind kind;
        private final Ingredient ingredient;
        private final CableKind cableKind;
        private final int cableInsulation;
        private final UniversalFluidCellItem.CellFluid fluid;

        private Alt(AltKind kind, Ingredient ingredient, CableKind cableKind, int cableInsulation, UniversalFluidCellItem.CellFluid fluid) {
            this.kind = kind;
            this.ingredient = ingredient;
            this.cableKind = cableKind;
            this.cableInsulation = cableInsulation;
            this.fluid = fluid;
        }

        static Alt ingredient(Ingredient ingredient) {
            return new Alt(AltKind.INGREDIENT, ingredient, CableKind.COPPER, 0, UniversalFluidCellItem.CellFluid.EMPTY);
        }

        static Alt cable(CableKind kind, int insulation) {
            return new Alt(AltKind.CABLE, Ingredient.EMPTY, kind, kind.clampInsulation(insulation), UniversalFluidCellItem.CellFluid.EMPTY);
        }

        static Alt fluidCell(UniversalFluidCellItem.CellFluid fluid) {
            return new Alt(AltKind.FLUID_CELL, Ingredient.EMPTY, CableKind.COPPER, 0, fluid);
        }

        boolean test(ItemStack stack) {
            return switch (kind) {
                case INGREDIENT -> ingredient.test(stack);
                case CABLE -> stack.getItem() instanceof CableItem
                        && CableItem.getKind(stack) == cableKind
                        && CableItem.getInsulation(stack) == cableInsulation;
                case FLUID_CELL -> stack.getItem() instanceof UniversalFluidCellItem
                        && UniversalFluidCellItem.getFluid(stack) == fluid;
            };
        }

        List<ItemStack> previewStacks() {
            return switch (kind) {
                case INGREDIENT -> List.of(ingredient.getMatchingStacks());
                case CABLE -> List.of(CableItem.createStack(ModItems.CABLE, cableKind, cableInsulation));
                case FLUID_CELL -> List.of(UniversalFluidCellItem.createStack(fluid));
            };
        }

        void write(PacketByteBuf buf) {
            buf.writeEnumConstant(kind);
            switch (kind) {
                case INGREDIENT -> ingredient.write(buf);
                case CABLE -> {
                    buf.writeEnumConstant(cableKind);
                    buf.writeVarInt(cableInsulation);
                }
                case FLUID_CELL -> buf.writeString(fluid.id);
            }
        }

        static Alt read(PacketByteBuf buf) {
            AltKind kind = buf.readEnumConstant(AltKind.class);
            return switch (kind) {
                case INGREDIENT -> ingredient(Ingredient.fromPacket(buf));
                case CABLE -> cable(buf.readEnumConstant(CableKind.class), buf.readVarInt());
                case FLUID_CELL -> fluidCell(UniversalFluidCellItem.CellFluid.byId(buf.readString()));
            };
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("kind", kind.name().toLowerCase(java.util.Locale.ROOT));
            switch (kind) {
                case INGREDIENT -> json.add("ingredient", ingredient.toJson());
                case CABLE -> {
                    json.addProperty("cable", cableKind.id());
                    json.addProperty("insulation", cableInsulation);
                }
                case FLUID_CELL -> json.addProperty("fluid", fluid.id);
            }
            return json;
        }

        static Alt fromJson(JsonObject json) {
            String kind = JsonHelper.getString(json, "kind", "ingredient");
            return switch (kind) {
                case "cable" -> cable(CableKind.fromId(JsonHelper.getString(json, "cable")), JsonHelper.getInt(json, "insulation", 0));
                case "fluid_cell" -> fluidCell(UniversalFluidCellItem.CellFluid.byId(JsonHelper.getString(json, "fluid")));
                default -> ingredient(Ingredient.fromJson(JsonHelper.getObject(json, "ingredient")));
            };
        }
    }
}
