package rocamocha.lootsparkle.core;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Configuration manager for the Loot Sparkle mod
 *
 * Handles loading and saving configuration from config/loot-sparkle.properties
 */
public class LootSparkleConfig {
    private static final String CONFIG_FILE_NAME = "loot-sparkle.properties";
    private static final String SPARKLE_LIFETIME_KEY = "sparkle_lifetime_minutes";
    private static final String VERTICAL_RADIUS_KEY = "vertical_spawn_radius";
    private static final String TREASURE_COMPASS_DURABILITY_KEY = "treasure_compass_durability";
    private static final String FAIRY_DUST_TICK_INTERVAL_KEY = "fairy_dust_tick_interval";
    private static final String HOSTILE_SPARKLE_ACTIVATION_RANGE_KEY = "hostile_sparkle_activation_range";
    private static final String HOSTILE_SPARKLE_AUTO_ACTIVATION_ENABLED_KEY = "hostile_sparkle_auto_activation_enabled";
    private static final String HOSTILE_MOB_LEASH_RANGE_KEY = "hostile_mob_leash_range";
    private static final String HOSTILE_SPARKLE_MIN_DISTANCE_KEY = "hostile_sparkle_min_distance";
    private static final String HOSTILE_SPARKLE_TEXT_RENDER_RANGE_KEY = "hostile_sparkle_text_render_range";
    private static final int DEFAULT_SPARKLE_LIFETIME_MINUTES = 10;
    private static final int DEFAULT_VERTICAL_RADIUS = 16;
    private static final int DEFAULT_TREASURE_COMPASS_DURABILITY = 480;
    private static final int DEFAULT_FAIRY_DUST_TICK_INTERVAL = 60;
    private static final double DEFAULT_HOSTILE_SPARKLE_ACTIVATION_RANGE = 8.0;
    private static final double DEFAULT_HOSTILE_MOB_LEASH_RANGE = 32.0;
    private static final double DEFAULT_HOSTILE_SPARKLE_MIN_DISTANCE = 32.0;
    private static final double DEFAULT_HOSTILE_SPARKLE_TEXT_RENDER_RANGE = 24.0;

    private static int sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
    private static int verticalRadius = DEFAULT_VERTICAL_RADIUS;
    private static int treasureCompassDurability = DEFAULT_TREASURE_COMPASS_DURABILITY;
    private static int fairyDustTickInterval = DEFAULT_FAIRY_DUST_TICK_INTERVAL;
    private static double hostileSparkleActivationRange = DEFAULT_HOSTILE_SPARKLE_ACTIVATION_RANGE;
    private static boolean hostileSparkleAutoActivationEnabled = true;
    private static double hostileMobLeashRange = DEFAULT_HOSTILE_MOB_LEASH_RANGE;
    private static double hostileSparkleMinDistance = DEFAULT_HOSTILE_SPARKLE_MIN_DISTANCE;
    private static double hostileSparkleTextRenderRange = DEFAULT_HOSTILE_SPARKLE_TEXT_RENDER_RANGE;

    /**
     * Loads the configuration from the config file
     */
    public static void loadConfig() {
        try {
            // Get the config directory (works for both client and server)
            Path configDir = Paths.get("config");
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            // Create config directory if it doesn't exist
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Properties properties = new Properties();

            // Load existing config if it exists
            if (Files.exists(configFile)) {
                try (FileInputStream fis = new FileInputStream(configFile.toFile())) {
                    properties.load(fis);
                }
            } else {
                // Create default config file
            }

            // Read sparkle lifetime setting
            String lifetimeStr = properties.getProperty(SPARKLE_LIFETIME_KEY,
                String.valueOf(DEFAULT_SPARKLE_LIFETIME_MINUTES));
            try {
                sparkleLifetimeMinutes = Integer.parseInt(lifetimeStr);
                if (sparkleLifetimeMinutes <= 0) {
                    sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
                }
            } catch (NumberFormatException e) {
                sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
            }

            // Read vertical radius setting
            String verticalRadiusStr = properties.getProperty(VERTICAL_RADIUS_KEY,
                String.valueOf(DEFAULT_VERTICAL_RADIUS));
            try {
                verticalRadius = Integer.parseInt(verticalRadiusStr);
                if (verticalRadius < 0) {
                    verticalRadius = DEFAULT_VERTICAL_RADIUS;
                }
            } catch (NumberFormatException e) {
                verticalRadius = DEFAULT_VERTICAL_RADIUS;
            }

            // Read treasure compass durability setting
            String durabilityStr = properties.getProperty(TREASURE_COMPASS_DURABILITY_KEY,
                String.valueOf(DEFAULT_TREASURE_COMPASS_DURABILITY));
            try {
                treasureCompassDurability = Integer.parseInt(durabilityStr);
                if (treasureCompassDurability < 0) {
                    treasureCompassDurability = DEFAULT_TREASURE_COMPASS_DURABILITY;
                }
            } catch (NumberFormatException e) {
                treasureCompassDurability = DEFAULT_TREASURE_COMPASS_DURABILITY;
            }

            // Read fairy dust tick interval setting
            String tickIntervalStr = properties.getProperty(FAIRY_DUST_TICK_INTERVAL_KEY,
                String.valueOf(DEFAULT_FAIRY_DUST_TICK_INTERVAL));
            try {
                fairyDustTickInterval = Integer.parseInt(tickIntervalStr);
                if (fairyDustTickInterval <= 0) {
                    fairyDustTickInterval = DEFAULT_FAIRY_DUST_TICK_INTERVAL;
                }
            } catch (NumberFormatException e) {
                fairyDustTickInterval = DEFAULT_FAIRY_DUST_TICK_INTERVAL;
            }

            // Read hostile sparkle activation range setting
            String activationRangeStr = properties.getProperty(HOSTILE_SPARKLE_ACTIVATION_RANGE_KEY,
                String.valueOf(DEFAULT_HOSTILE_SPARKLE_ACTIVATION_RANGE));
            try {
                hostileSparkleActivationRange = Double.parseDouble(activationRangeStr);
                if (hostileSparkleActivationRange <= 0) {
                    hostileSparkleActivationRange = DEFAULT_HOSTILE_SPARKLE_ACTIVATION_RANGE;
                }
            } catch (NumberFormatException e) {
                hostileSparkleActivationRange = DEFAULT_HOSTILE_SPARKLE_ACTIVATION_RANGE;
            }

            // Read hostile mob leash range setting
            String leashRangeStr = properties.getProperty(HOSTILE_MOB_LEASH_RANGE_KEY,
                String.valueOf(DEFAULT_HOSTILE_MOB_LEASH_RANGE));
            try {
                hostileMobLeashRange = Double.parseDouble(leashRangeStr);
                if (hostileMobLeashRange <= 0) {
                    hostileMobLeashRange = DEFAULT_HOSTILE_MOB_LEASH_RANGE;
                }
            } catch (NumberFormatException e) {
                hostileMobLeashRange = DEFAULT_HOSTILE_MOB_LEASH_RANGE;
            }

            // Read hostile sparkle minimum distance setting
            String minDistanceStr = properties.getProperty(HOSTILE_SPARKLE_MIN_DISTANCE_KEY,
                String.valueOf(DEFAULT_HOSTILE_SPARKLE_MIN_DISTANCE));
            try {
                hostileSparkleMinDistance = Double.parseDouble(minDistanceStr);
                if (hostileSparkleMinDistance < 0) {
                    hostileSparkleMinDistance = DEFAULT_HOSTILE_SPARKLE_MIN_DISTANCE;
                }
            } catch (NumberFormatException e) {
                hostileSparkleMinDistance = DEFAULT_HOSTILE_SPARKLE_MIN_DISTANCE;
            }

            // Read hostile sparkle text render range setting
            String textRenderRangeStr = properties.getProperty(HOSTILE_SPARKLE_TEXT_RENDER_RANGE_KEY,
                String.valueOf(DEFAULT_HOSTILE_SPARKLE_TEXT_RENDER_RANGE));
            try {
                hostileSparkleTextRenderRange = Double.parseDouble(textRenderRangeStr);
                if (hostileSparkleTextRenderRange < 0) {
                    hostileSparkleTextRenderRange = DEFAULT_HOSTILE_SPARKLE_TEXT_RENDER_RANGE;
                }
            } catch (NumberFormatException e) {
                hostileSparkleTextRenderRange = DEFAULT_HOSTILE_SPARKLE_TEXT_RENDER_RANGE;
            }

            // Read hostile sparkle auto-activation toggle (default true)
            String autoActivationStr = properties.getProperty(HOSTILE_SPARKLE_AUTO_ACTIVATION_ENABLED_KEY,
                String.valueOf(true));
            hostileSparkleAutoActivationEnabled = Boolean.parseBoolean(autoActivationStr);

            // Save the config (this will create the file with current values if it doesn't exist)
            saveConfig();


        } catch (Exception e) {
            sparkleLifetimeMinutes = DEFAULT_SPARKLE_LIFETIME_MINUTES;
        }
    }

    /**
     * Saves the current configuration to the config file
     */
    private static void saveConfig() {
        try {
            Path configDir = Paths.get("config");
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);

            Properties properties = new Properties();
            properties.setProperty(SPARKLE_LIFETIME_KEY, String.valueOf(sparkleLifetimeMinutes));
            properties.setProperty(VERTICAL_RADIUS_KEY, String.valueOf(verticalRadius));
            properties.setProperty(TREASURE_COMPASS_DURABILITY_KEY, String.valueOf(treasureCompassDurability));
            properties.setProperty(FAIRY_DUST_TICK_INTERVAL_KEY, String.valueOf(fairyDustTickInterval));
            properties.setProperty(HOSTILE_SPARKLE_ACTIVATION_RANGE_KEY, String.valueOf(hostileSparkleActivationRange));
            properties.setProperty(HOSTILE_SPARKLE_AUTO_ACTIVATION_ENABLED_KEY, String.valueOf(hostileSparkleAutoActivationEnabled));
            properties.setProperty(HOSTILE_MOB_LEASH_RANGE_KEY, String.valueOf(hostileMobLeashRange));
            properties.setProperty(HOSTILE_SPARKLE_MIN_DISTANCE_KEY, String.valueOf(hostileSparkleMinDistance));
            properties.setProperty(HOSTILE_SPARKLE_TEXT_RENDER_RANGE_KEY, String.valueOf(hostileSparkleTextRenderRange));

            // Add comments
            String comments = "Loot Sparkle Mod Configuration\n" +
                "sparkle_lifetime_minutes: How long sparkles last before disappearing (in minutes)\n" +
                "vertical_spawn_radius: Maximum vertical distance sparkles can spawn from player (in blocks)\n" +
                "treasure_compass_durability: Durability of the treasure compass item (0 = unbreakable)\n" +
                "fairy_dust_tick_interval: How often (in ticks) the compass loses 1 durability when Fairy Dust is active (20 ticks = 1 second)\n" +
                "hostile_sparkle_activation_range: Distance (in blocks) within which players activate hostile sparkles\n" +
                "hostile_sparkle_auto_activation_enabled: If true, hostile sparkles auto-activate when a player is in range (default true)\n" +
                "hostile_mob_leash_range: Maximum distance (in blocks) hostile mobs can wander from their sparkle before being teleported back\n" +
                "hostile_sparkle_min_distance: Minimum distance (in blocks) that must be maintained between hostile sparkles\n" +
                "hostile_sparkle_text_render_range: Maximum distance (in blocks) from player within which hostile sparkle HUD text is rendered";

            try (FileOutputStream fos = new FileOutputStream(configFile.toFile())) {
                properties.store(fos, comments);
            }

        } catch (Exception e) {
        }
    }

    /**
     * Gets the sparkle lifetime in milliseconds
     */
    public static long getSparkleLifetimeMs() {
        return sparkleLifetimeMinutes * 60 * 1000L;
    }

    /**
     * Gets the sparkle lifetime in minutes
     */
    public static int getSparkleLifetimeMinutes() {
        return sparkleLifetimeMinutes;
    }

    /**
     * Gets the vertical spawn radius in blocks
     */
    public static int getVerticalSpawnRadius() {
        return verticalRadius;
    }

    /**
     * Gets the treasure compass durability
     */
    public static int getTreasureCompassDurability() {
        return treasureCompassDurability;
    }

    /**
     * Gets the fairy dust tick interval (how often compass loses durability)
     */
    public static int getFairyDustTickInterval() {
        return fairyDustTickInterval;
    }

    /**
     * Gets the hostile sparkle activation range in blocks
     */
    public static double getHostileSparkleActivationRange() {
        return hostileSparkleActivationRange;
    }

    /**
     * Returns whether hostile sparkles auto-activate when in range
     */
    public static boolean isHostileSparkleAutoActivationEnabled() {
        return hostileSparkleAutoActivationEnabled;
    }

    /**
     * Gets the hostile mob leash range in blocks
     */
    public static double getHostileMobLeashRange() {
        return hostileMobLeashRange;
    }

    /**
     * Gets the hostile sparkle minimum distance in blocks
     */
    public static double getHostileSparkleMinDistance() {
        return hostileSparkleMinDistance;
    }

    /**
     * Gets the hostile sparkle text render range in blocks
     */
    public static double getHostileSparkleTextRenderRange() {
        return hostileSparkleTextRenderRange;
    }
}
