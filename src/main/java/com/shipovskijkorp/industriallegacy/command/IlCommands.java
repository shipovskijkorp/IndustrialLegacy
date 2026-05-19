package com.shipovskijkorp.industriallegacy.command;

import com.mojang.brigadier.context.CommandContext;
import com.shipovskijkorp.industriallegacy.recipe.RecipeLoadTracker;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public final class IlCommands {
    private IlCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("il")
                        .then(CommandManager.literal("recipes_log")
                                .executes(IlCommands::sendRecipesLog))
        ));
    }

    private static int sendRecipesLog(CommandContext<ServerCommandSource> context) {
        RecipeLoadTracker.RecipeLoadSummary summary = RecipeLoadTracker.snapshot();
        ServerCommandSource source = context.getSource();

        source.sendFeedback(() -> Text.literal("Recipes discovered: " + summary.discovered()
                + " loaded: " + summary.loaded()
                + " failed: " + summary.failed()
                + " skipped: " + summary.skipped()), false);

        for (RecipeLoadTracker.CategoryStats category : summary.categories()) {
            source.sendFeedback(() -> Text.literal(category.category() + " "
                    + category.discovered() + "/" + category.loaded() + "/" + category.failed() + "/" + category.skipped()), false);
        }

        return Math.max(1, summary.loaded());
    }
}
