package rocamocha.lootsparkle.core;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocamocha.lootsparkle.sparkle.SparkleParticleRenderer;
import rocamocha.lootsparkle.sparkle.ClientSparkleManager;
import rocamocha.lootsparkle.network.SparkleNetworking;
import rocamocha.lootsparkle.item.TreasureCompassItem;

/**
 * Client-side entry point for Loot Sparkle mod
 *
 * Handles client-specific functionality including:
 * - Particle effects rendering
 * - Key binding registration
 * - Client-side sparkle interaction
 */
public class LootSparkleClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(LootSparkle.MOD_ID);

    @Override
    public void onInitializeClient() {

        // Initialize particle effects
        SparkleParticleRenderer.initialize();

        // Initialize client-side sparkle manager for network sync
        ClientSparkleManager.initialize();

        // Register client-side packet codecs for packets the client receives
        SparkleNetworking.registerClientCodecs();

        // Register custom model predicates for treasure compass
        registerModelPredicates();

    }

    /**
     * Register custom model predicates for treasure compass behavior
     */
    private static void registerModelPredicates() {
        // Register custom angle predicate for Treasure Compass
        ModelPredicateProviderRegistry.register(
            LootSparkle.TREASURE_COMPASS,
            Identifier.ofVanilla("angle"),
            (stack, world, entity, seed) -> {
                if (entity == null) return 0.0f;

                // Check if player has Fairy Dust enchantment
                boolean hasFairyDust = TreasureCompassItem.hasFairyDust(stack);

                float angle;
                if (hasFairyDust) {
                    // Point to nearest sparkle
                    angle = calculateSparkleAngle(entity);
                } else {
                    // Haywire behavior - random spinning
                    angle = calculateHaywireAngle();
                }

                // Convert to 0.0-1.0 range and ensure it's valid
                float normalizedAngle = (angle / 360.0f) % 1.0f;
                if (normalizedAngle < 0) {
                    normalizedAngle += 1.0f;
                }
                
                return normalizedAngle;
            }
        );
    }

    /**
     * Calculates the angle to the nearest sparkle for compass pointing
     */
    private static float calculateSparkleAngle(net.minecraft.entity.Entity entity) {
        // Get nearest non-hostile sparkle (player sparkles)
        ClientSparkleManager.ClientSparkle nearestSparkle = ClientSparkleManager.getNearestSparkle(
            entity.getX(), entity.getY(), entity.getZ());

        if (nearestSparkle == null) {
            // No sparkles available, point north as fallback
            return 0.0f;
        }

        // Calculate direction vector from player to sparkle
        double dx = nearestSparkle.getPosition().getX() + 0.5 - entity.getX();
        double dz = nearestSparkle.getPosition().getZ() + 0.5 - entity.getZ();

        // Calculate angle to target using atan2 (angle from positive X axis/east)
        double angleToTarget = Math.toDegrees(Math.atan2(dz, dx));

        // Convert from atan2 coordinates (0° = east) to compass coordinates (0° = south)
        // Add 90° to convert: east->south, south->west, west->north, north->east
        double compassAngle = (angleToTarget + 90.0) % 360.0;

        // Get player's yaw and normalize to 0-360 range
        float playerYaw = entity.getYaw();
        double normalizedYaw = playerYaw;
        if (normalizedYaw < 0) {
            normalizedYaw += 360.0;
        }

        // Calculate relative angle (positive = clockwise from player's facing)
        double relativeAngle = compassAngle - normalizedYaw;

        // Normalize to 0-360 range
        while (relativeAngle < 0) {
            relativeAngle += 360.0;
        }
        while (relativeAngle >= 360.0) {
            relativeAngle -= 360.0;
        }

        return (float) relativeAngle;
    }

    /**
     * Calculates haywire (random spinning) angle for compass when Fairy Dust is missing
     */
    private static float calculateHaywireAngle() {
        // Use system time for consistent spinning across ticks
        long time = System.currentTimeMillis();
        // Spin at a moderate speed (full rotation every ~3 seconds)
        double spinProgress = (time % 3000L) / 3000.0;
        // Add some randomness to make it look more erratic
        double randomOffset = Math.sin(time * 0.001) * 30.0; // ±30 degree wobble

        double angle = (spinProgress * 360.0) + randomOffset;

        // Normalize to 0-360 range
        while (angle < 0) {
            angle += 360.0;
        }
        while (angle >= 360.0) {
            angle -= 360.0;
        }

        return (float) angle;
    }
}
