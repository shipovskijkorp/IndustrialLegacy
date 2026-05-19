package com.shipovskijkorp.industriallegacy.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.shipovskijkorp.industriallegacy.recipe.CraftingRecipeIniLoader;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Shadow private Map<RecipeType<?>, Map<Identifier, Recipe<?>>> recipes;
    @Shadow private Map<Identifier, Recipe<?>> recipesById;

    @Inject(method = "apply", at = @At("TAIL"))
    private void industriallegacy$loadIniCraftingRecipes(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci) {
        Map<RecipeType<?>, Map<Identifier, Recipe<?>>> byType = new HashMap<>();
        for (Map.Entry<RecipeType<?>, Map<Identifier, Recipe<?>>> entry : this.recipes.entrySet()) {
            byType.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        Map<Identifier, Recipe<?>> byId = new LinkedHashMap<>(this.recipesById);

        for (Recipe<?> recipe : CraftingRecipeIniLoader.loadBuiltinRecipes()) {
            byType.computeIfAbsent(recipe.getType(), ignored -> new LinkedHashMap<>()).put(recipe.getId(), recipe);
            byId.put(recipe.getId(), recipe);
        }

        ImmutableMap.Builder<RecipeType<?>, Map<Identifier, Recipe<?>>> recipesBuilder = ImmutableMap.builder();
        for (Map.Entry<RecipeType<?>, Map<Identifier, Recipe<?>>> entry : byType.entrySet()) {
            recipesBuilder.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
        }
        this.recipes = recipesBuilder.build();
        this.recipesById = ImmutableMap.copyOf(byId);
    }
}
