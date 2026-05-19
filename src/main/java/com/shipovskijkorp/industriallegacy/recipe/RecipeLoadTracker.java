package com.shipovskijkorp.industriallegacy.recipe;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tracks IL .ini recipe loading attempts, successes and failures for logs and commands. */
public final class RecipeLoadTracker {
    private static final Map<String, MutableCategoryStats> CATEGORIES = new LinkedHashMap<>();
    private static String lastLoggedFailureFingerprint = "";

    private RecipeLoadTracker() {}

    public static String categoryName(String resourcePath) {
        int slash = Math.max(resourcePath.lastIndexOf('/'), resourcePath.lastIndexOf('\\'));
        return slash >= 0 ? resourcePath.substring(slash + 1) : resourcePath;
    }

    public static synchronized void beginCategory(String category) {
        CATEGORIES.put(category, new MutableCategoryStats(category));
    }

    public static synchronized void discovered(String category) {
        category(category).discovered++;
    }

    public static synchronized void loaded(String category) {
        category(category).loaded++;
    }

    public static synchronized void failed(String category, String recipeName, Throwable error) {
        failed(category, recipeName, error == null ? "unknown error" : error.getMessage());
    }

    public static synchronized void failed(String category, String recipeName, String reason) {
        MutableCategoryStats stats = category(category);
        stats.failed++;
        stats.failures.add(new Failure(category, recipeName, reason == null || reason.isBlank() ? "unknown error" : reason));
    }

    public static synchronized RecipeLoadSummary snapshot() {
        int discovered = 0;
        int loaded = 0;
        int failed = 0;
        List<CategoryStats> categories = new ArrayList<>();
        List<Failure> failures = new ArrayList<>();

        for (MutableCategoryStats stats : CATEGORIES.values()) {
            discovered += stats.discovered;
            loaded += stats.loaded;
            failed += stats.failed;
            categories.add(new CategoryStats(stats.category, stats.discovered, stats.loaded, stats.failed));
            failures.addAll(stats.failures);
        }

        return new RecipeLoadSummary(discovered, loaded, failed, List.copyOf(categories), List.copyOf(failures));
    }

    public static void logFailuresIfAny() {
        RecipeLoadSummary summary = snapshot();
        if (summary.failed() <= 0) return;

        StringBuilder log = new StringBuilder("IL recipes load failed:");
        for (Failure failure : summary.failures()) {
            log.append('\n')
                    .append(failure.recipeName())
                    .append(" — ")
                    .append(failure.reason());
        }

        String fingerprint = log.toString();
        synchronized (RecipeLoadTracker.class) {
            if (fingerprint.equals(lastLoggedFailureFingerprint)) return;
            lastLoggedFailureFingerprint = fingerprint;
        }

        IndustrialLegacy.LOGGER.warn("{}", fingerprint);
    }

    private static MutableCategoryStats category(String category) {
        return CATEGORIES.computeIfAbsent(category, MutableCategoryStats::new);
    }

    private static final class MutableCategoryStats {
        private final String category;
        private int discovered;
        private int loaded;
        private int failed;
        private final List<Failure> failures = new ArrayList<>();

        private MutableCategoryStats(String category) {
            this.category = category;
        }
    }

    public record CategoryStats(String category, int discovered, int loaded, int failed) {}
    public record Failure(String category, String recipeName, String reason) {}

    public record RecipeLoadSummary(int discovered, int loaded, int failed, List<CategoryStats> categories, List<Failure> failures) {
        public RecipeLoadSummary {
            categories = Collections.unmodifiableList(new ArrayList<>(categories));
            failures = Collections.unmodifiableList(new ArrayList<>(failures));
        }
    }
}
