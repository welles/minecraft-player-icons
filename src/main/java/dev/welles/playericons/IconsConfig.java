package dev.welles.playericons;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Settings stored in {@code config/playericons.properties}.
 * The file is re-read automatically whenever it changes on disk.
 */
public final class IconsConfig {
    /** The allowed values of each number setting; values outside are clamped. */
    public static final Map<String, Range> RANGES = Map.of(
        "iconSize", new Range(4, 64),
        "hideDistance", new Range(0, 1024),
        "fadeDistance", new Range(0, 256),
        "opacity", new Range(0.05, 1)
    );
    /** The allowed values of each setting with a fixed set of them, none so far. */
    public static final Map<String, List<String>> CHOICES = Map.of();

    public boolean visible = true;
    public int iconSize = 12;
    public double hideDistance = 8.0;
    public double fadeDistance = 8.0;
    public double opacity = 1.0;
    public boolean showNames = true;
    public boolean showDistance = true;
    public boolean sharePositions = true;

    private static final String HEADER = """
        Player Icons configuration
        Changes are picked up automatically, no restart required.
        sharePositions is decided by the server, all other settings only affect the client.
        The toggle key is changed in Options > Controls > Key Binds > Multiplayer.

        visible         true = show the icons when the game starts. The toggle key shows and hides them while playing.
                        Changing this value shows or hides them immediately.
        iconSize        Size of a face in GUI pixels (the size of a hotbar slot is 20). 4 to 64.
        hideDistance    Players closer than this many blocks get no icon, you can see them anyway. 0 to 1024.
        fadeDistance    Beyond hideDistance the icon fades in over this many blocks; 0 = it appears at once. 0 to 256.
        opacity         Opacity of fully visible icons, 1.0 = opaque. 0.05 to 1.
        showNames       true = show the player's name below the face
        showDistance    true = show the distance in blocks below the face
        sharePositions  true = the server sends every player with the mod the positions of all other players in the
                        same dimension, so they are shown at any distance. false = only players within the render
                        distance are shown. Only used on the server (and in singleplayer, i.e. LAN worlds).""";

    private static Path file;
    private static FileTime loadedAt;
    private static IconsConfig current = new IconsConfig();

    public static synchronized void init(Path configDir) {
        file = configDir.resolve(PlayerIcons.MOD_ID + ".properties");
        reload();
    }

    /** Returns the current settings, re-reading the file if it was modified. */
    public static synchronized IconsConfig get() {
        if (file != null) {
            try {
                FileTime modified = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
                if (modified == null || !modified.equals(loadedAt)) reload();
            } catch (IOException e) {
                PlayerIcons.LOGGER.warn("Could not check config file {}", file, e);
            }
        }
        return current;
    }

    private static void reload() {
        IconsConfig config = new IconsConfig();
        Properties props = new Properties();
        boolean complete = false;

        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                props.load(reader);
                complete = config.read(props);
            } catch (IOException | RuntimeException e) {
                PlayerIcons.LOGGER.error("Could not read config file {}, using defaults", file, e);
                current = config;
                return;
            }
        }

        if (!complete) {
            try {
                Files.createDirectories(file.getParent());
                try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                    config.write(writer);
                }
            } catch (IOException e) {
                PlayerIcons.LOGGER.error("Could not write config file {}", file, e);
            }
        }

        try {
            loadedAt = Files.exists(file) ? Files.getLastModifiedTime(file) : null;
        } catch (IOException e) {
            loadedAt = null;
        }
        current = config;
    }

    /** Applies all valid values present in {@code props}; returns whether every key was present. */
    private boolean read(Properties props) {
        visible = readBoolean(props, "visible", visible);
        iconSize = (int) Math.round(readNumber(props, "iconSize", iconSize, true));
        hideDistance = readNumber(props, "hideDistance", hideDistance, false);
        fadeDistance = readNumber(props, "fadeDistance", fadeDistance, false);
        opacity = readNumber(props, "opacity", opacity, false);
        showNames = readBoolean(props, "showNames", showNames);
        showDistance = readBoolean(props, "showDistance", showDistance);
        sharePositions = readBoolean(props, "sharePositions", sharePositions);
        return props.keySet().containsAll(toProperties().keySet());
    }

    private static boolean readBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    /** Reads a number clamped to its allowed values; keeps the fallback if it is missing or not a number. */
    private static double readNumber(Properties props, String key, double fallback, boolean integer) {
        String value = props.getProperty(key);
        if (value == null) return fallback;
        try {
            double number = integer ? Integer.parseInt(value.trim()) : Double.parseDouble(value.trim());
            if (Double.isFinite(number)) return RANGES.get(key).clamp(number);
        } catch (NumberFormatException ignored) {
            // logged below
        }
        PlayerIcons.LOGGER.warn("Invalid {} '{}' in {}, using {}", key, value.trim(), file, fallback);
        return fallback;
    }

    private void write(Writer writer) throws IOException {
        for (String line : HEADER.split("\n")) writer.write(("# " + line).stripTrailing() + "\n");
        writer.write("\n");
        for (var entry : toProperties().entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
        }
    }

    /** The default value of every setting, as written to the file. */
    public static Map<String, String> defaults() {
        return new IconsConfig().toProperties();
    }

    private Map<String, String> toProperties() {
        Map<String, String> props = new LinkedHashMap<>();
        props.put("visible", String.valueOf(visible));
        props.put("iconSize", String.valueOf(iconSize));
        props.put("hideDistance", String.valueOf(hideDistance));
        props.put("fadeDistance", String.valueOf(fadeDistance));
        props.put("opacity", String.valueOf(opacity));
        props.put("showNames", String.valueOf(showNames));
        props.put("showDistance", String.valueOf(showDistance));
        props.put("sharePositions", String.valueOf(sharePositions));
        return props;
    }

    public record Range(double min, double max) {
        public double clamp(double value) {
            return Math.clamp(value, min, max);
        }

        public boolean contains(double value) {
            return value >= min && value <= max;
        }
    }
}
