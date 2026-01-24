package com.shipovskijkorp.industriallegacy.config;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal IC2-style .ini config reader.
 * <p>
 * We keep the same key format IC2 uses in code, e.g.
 * {@code balance/energy/generator/generator}.
 */
public final class ILConfig {
    private ILConfig() {}

    private static final String DIR_NAME = IndustrialLegacy.MOD_ID; // config/industrial_legacy/
    private static final String FILE_NAME = "general.ini";

    private static volatile Map<String, String> values = Collections.emptyMap();
    private static volatile boolean loaded = false;

    public static synchronized void load() {
        if (loaded) return;
        reload();
    }

    public static synchronized void reload() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve(DIR_NAME);
        Path file = configDir.resolve(FILE_NAME);

        try {
            if (!Files.exists(file)) {
                Files.createDirectories(configDir);
                writeDefault(file);
            }

            values = Collections.unmodifiableMap(parseIni(file));
            loaded = true;
            IndustrialLegacy.LOGGER.info("Loaded config: {}", file);
        } catch (IOException e) {
            IndustrialLegacy.LOGGER.error("Failed to load config: {}", file, e);
            values = Collections.emptyMap();
            loaded = true;
        }
    }

    private static void writeDefault(Path file) throws IOException {
        // Minimal starter file. Users can replace it with a full IC2 general.ini.
        String content = """
                # Industrial Legacy config (IC2-compatible paths)
                #
                # You can paste your full IC2 1.12.2 general.ini here.
                # The mod reads keys in the same format as IC2, e.g.:
                #   balance/energy/generator/generator
                
                [balance/energy/generator]
                generator = 1.0
                
                [misc]
                allowBurningScrap = false
                
                [balance]
                energyRetainedInStorageBlockDrops = 0.8
                """;

        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseIni(Path file) throws IOException {
        Map<String, String> map = new HashMap<>();
        String section = "";

        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#") || line.startsWith(";")) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int eq = line.indexOf('=');
            if (eq < 0) continue;

            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();

            // Inline comment stripping ("foo = 1 # comment")
            int hash = value.indexOf('#');
            int semi = value.indexOf(';');
            int cut = -1;
            if (hash >= 0) cut = hash;
            if (semi >= 0) cut = (cut < 0) ? semi : Math.min(cut, semi);
            if (cut >= 0) value = value.substring(0, cut).trim();

            String fullKey = section.isEmpty() ? key : (section + "/" + key);
            map.put(fullKey, value);
        }

        return map;
    }

    public static String getString(String path, String def) {
        load();
        return values.getOrDefault(path, def);
    }

    public static boolean getBool(String path, boolean def) {
        String v = getString(path, null);
        if (v == null) return def;
        return v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes") || v.equals("1");
    }

    public static int getInt(String path, int def) {
        String v = getString(path, null);
        if (v == null) return def;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long getLong(String path, long def) {
        String v = getString(path, null);
        if (v == null) return def;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static float getFloat(String path, float def) {
        String v = getString(path, null);
        if (v == null) return def;
        try {
            return Float.parseFloat(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double getDouble(String path, double def) {
        String v = getString(path, null);
        if (v == null) return def;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
