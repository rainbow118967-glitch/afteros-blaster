package dev.afteros.crt.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Tiny stand-in for NeoForge's ModConfigSpec: same builder calls, stored in config/afteros_crt.properties.
 * Missing keys are added with defaults every launch; out-of-range values are clamped.
 */
public final class SimpleConfig {
    private final List<String> lines = new ArrayList<>();

    private SimpleConfig(List<String> lines) {
        this.lines.addAll(lines);
    }

    public static final class DoubleValue {
        private final double value;
        DoubleValue(double value) { this.value = value; }
        public Double get() { return value; }
    }

    public static final class IntValue {
        private final int value;
        IntValue(int value) { this.value = value; }
        public Integer get() { return value; }
    }

    public static final class BooleanValue {
        private final boolean value;
        BooleanValue(boolean value) { this.value = value; }
        public Boolean get() { return value; }
    }

    public static final class Builder {
        private final Properties loaded = new Properties();
        private final Path file = FabricLoader.getInstance().getConfigDir().resolve("afteros_crt.properties");
        private final Deque<String> path = new ArrayDeque<>();
        private final List<String> out = new ArrayList<>();
        private List<String> pendingComment = new ArrayList<>();

        public Builder() {
            if (Files.exists(file)) {
                try (Reader r = Files.newBufferedReader(file)) {
                    loaded.load(r);
                } catch (IOException ignored) {
                    // fall back to defaults
                }
            }
        }

        public Builder comment(String... text) {
            for (String t : text) {
                pendingComment.add(t);
            }
            return this;
        }

        public Builder push(String section) {
            out.add("");
            for (String c : pendingComment) {
                out.add("# " + c);
            }
            pendingComment = new ArrayList<>();
            out.add("# ---- " + section + " ----");
            path.addLast(section);
            return this;
        }

        public Builder pop() {
            path.removeLast();
            return this;
        }

        private String key(String name) {
            StringBuilder sb = new StringBuilder();
            for (String p : path) {
                sb.append(p).append('.');
            }
            return sb.append(name).toString();
        }

        private void note(String key, Object value, String range) {
            for (String c : pendingComment) {
                out.add("# " + c);
            }
            pendingComment = new ArrayList<>();
            if (range != null) {
                out.add("# range: " + range);
            }
            out.add(key + "=" + value);
        }

        public DoubleValue defineInRange(String name, double def, double min, double max) {
            String k = key(name);
            double v = def;
            try {
                v = Double.parseDouble(loaded.getProperty(k, Double.toString(def)).trim());
            } catch (NumberFormatException ignored) {
                // keep default
            }
            v = Math.max(min, Math.min(max, v));
            note(k, v, min + " ~ " + max);
            return new DoubleValue(v);
        }

        public IntValue defineInRange(String name, int def, int min, int max) {
            String k = key(name);
            int v = def;
            try {
                v = (int) Math.round(Double.parseDouble(loaded.getProperty(k, Integer.toString(def)).trim()));
            } catch (NumberFormatException ignored) {
                // keep default
            }
            v = Math.max(min, Math.min(max, v));
            note(k, v, min + " ~ " + max);
            return new IntValue(v);
        }

        public BooleanValue define(String name, boolean def) {
            String k = key(name);
            boolean v = Boolean.parseBoolean(loaded.getProperty(k, Boolean.toString(def)).trim());
            note(k, v, "true or false");
            return new BooleanValue(v);
        }

        public SimpleConfig build() {
            try {
                Files.createDirectories(file.getParent());
                try (Writer w = Files.newBufferedWriter(file)) {
                    w.write("# AfterOS // CRT Arsenal settings. Edit and restart the game/world.\n");
                    for (String l : out) {
                        w.write(l + "\n");
                    }
                }
            } catch (IOException ignored) {
                // read-only config dir: defaults still apply
            }
            return new SimpleConfig(out);
        }
    }
}
