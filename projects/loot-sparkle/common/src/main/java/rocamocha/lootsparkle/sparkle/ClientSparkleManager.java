package rocamocha.lootsparkle.sparkle;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import rocamocha.lootsparkle.core.LootSparkle;
import rocamocha.lootsparkle.network.SparkleNetworking;

/**
 * Client-side sparkle manager that stores sparkle data received from server
 */
public class ClientSparkleManager {
    // Client-side storage of sparkles received from server
    private static final Map<UUID, Map<UUID, ClientSparkle>> playerSparkles = new ConcurrentHashMap<>();
    // Hostile sparkles (shared across all players)
    private static final Map<UUID, ClientSparkle> hostileSparkles = new ConcurrentHashMap<>();

    public static void initialize() {
        LootSparkle.LOGGER.info("Initializing client sparkle manager...");

        // Register packet receivers
        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.SYNC_SPARKLE,
            (packet, context) -> {
                context.client().execute(() -> {
                    syncSparkle(packet.sparkleId(), packet.playerId(), packet.position(), packet.tierLevel(), packet.phaseMessage());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.REMOVE_SPARKLE,
            (packet, context) -> {
                context.client().execute(() -> {
                    removeSparkle(packet.sparkleId(), packet.playerId());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.SYNC_SPAWNED_MOBS,
            (packet, context) -> {
                context.client().execute(() -> {
                    syncSpawnedMobs(packet.sparkleId(), packet.playerId(), packet.mobIds());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.SYNC_TIMER,
            (packet, context) -> {
                context.client().execute(() -> {
                    syncTimer(packet.sparkleId(), packet.playerId(), packet.showTimer(), packet.endTimeMs(), packet.isPreparation());
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(SparkleNetworking.PHASE_MESSAGE,
            (packet, context) -> {
                context.client().execute(() -> {
                    syncPhaseMessage(packet.sparkleId(), packet.playerId(), packet.message());
                });
            });

        // Register client disconnect handler to clear all sparkle data
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            clearAllSparkleData();
        });
    }

    /**
     * Called when server sends a sparkle sync packet
     */
    private static void syncSparkle(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel, String phaseMessage) {
        if (playerId == null) {
            // Hostile sparkle
            ClientSparkle sparkle = hostileSparkles.get(sparkleId);
            if (sparkle == null) {
                // Create new sparkle
                sparkle = new ClientSparkle(sparkleId, playerId, position, tierLevel);
                hostileSparkles.put(sparkleId, sparkle);
                LootSparkle.LOGGER.info("[DEBUG] Created new hostile sparkle {} at {} with tier {}", sparkleId, position, tierLevel);
            } else {
                // Update existing sparkle position and tier (in case they changed)
                // Note: We don't update position/tier as they shouldn't change, but we could if needed
                LootSparkle.LOGGER.debug("[DEBUG] Updating existing hostile sparkle {}", sparkleId);
            }
            // Always update the phase message
            sparkle.setPhaseMessage(phaseMessage);
            LootSparkle.LOGGER.info("[DEBUG] Synced hostile sparkle {} with phase message '{}'", sparkleId, phaseMessage);
        } else {
            // Player sparkle
            Map<UUID, ClientSparkle> sparkles = playerSparkles.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            ClientSparkle sparkle = sparkles.get(sparkleId);
            if (sparkle == null) {
                // Create new sparkle
                sparkle = new ClientSparkle(sparkleId, playerId, position, tierLevel);
                sparkles.put(sparkleId, sparkle);
                LootSparkle.LOGGER.debug("Created new sparkle {} for player {} at {} with tier {}", sparkleId, playerId, position, tierLevel);
            } else {
                // Update existing sparkle
                LootSparkle.LOGGER.debug("Updating existing sparkle {} for player {}", sparkleId, playerId);
            }
            // Always update the phase message
            sparkle.setPhaseMessage(phaseMessage);
            LootSparkle.LOGGER.debug("Synced sparkle {} for player {} with phase message '{}'", sparkleId, playerId, phaseMessage);
        }
    }

    /**
     * Called when server sends a sparkle removal packet
     */
    private static void removeSparkle(UUID sparkleId, UUID playerId) {
        if (playerId == null) {
            // Hostile sparkle
            hostileSparkles.remove(sparkleId);
            LootSparkle.LOGGER.debug("Removed hostile sparkle {}", sparkleId);
        } else {
            // Player sparkle
            Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
            if (sparkles != null) {
                sparkles.remove(sparkleId);
                LootSparkle.LOGGER.debug("Removed sparkle {} for player {}", sparkleId, playerId);
            }
        }
    }

    /**
     * Called when server sends an interaction failed packet
     */
    @SuppressWarnings("unused")
    private static void handleInteractionFailed(String reason) {
        LootSparkle.LOGGER.info("Sparkle interaction failed: {}", reason);
        // TODO: Could show a client-side message to the player
    }

    /**
     * Called when server sends a spawned mobs sync packet
     */
    private static void syncSpawnedMobs(UUID sparkleId, UUID playerId, List<UUID> mobIds) {
        if (playerId == null) {
            // Hostile sparkle
            ClientSparkle sparkle = hostileSparkles.get(sparkleId);
            if (sparkle != null) {
                sparkle.setSpawnedMobIds(new ArrayList<>(mobIds));
                LootSparkle.LOGGER.debug("Synced {} spawned mobs for hostile sparkle {}", mobIds.size(), sparkleId);
            }
        } else {
            // Player sparkle (if needed in future)
            Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
            if (sparkles != null) {
                ClientSparkle sparkle = sparkles.get(sparkleId);
                if (sparkle != null) {
                    sparkle.setSpawnedMobIds(new ArrayList<>(mobIds));
                    LootSparkle.LOGGER.debug("Synced {} spawned mobs for player sparkle {}", mobIds.size(), sparkleId);
                }
            }
        }
    }

    /**
     * Called when server sends a timer sync packet
     */
    private static void syncTimer(UUID sparkleId, UUID playerId, boolean showTimer, long endTimeMs, boolean isPreparation) {
        long currentTime = System.currentTimeMillis();
        LootSparkle.LOGGER.info("[DEBUG] Received timer packet: sparkleId={}, showTimer={}, endTimeMs={}, isPreparation={}, currentTime={}, timeDiff={}", 
            sparkleId, showTimer, endTimeMs, isPreparation, currentTime, endTimeMs - currentTime);
        
        if (playerId == null) {
            // Hostile sparkle
            ClientSparkle sparkle = hostileSparkles.get(sparkleId);
            if (sparkle != null) {
                sparkle.setShowTimer(showTimer);
                sparkle.setEndTimeMs(endTimeMs);
                sparkle.setIsPreparation(isPreparation);
                LootSparkle.LOGGER.debug("Synced timer for hostile sparkle {}: show={}, endTime={}, isPreparation={}", sparkleId, showTimer, endTimeMs, isPreparation);
            }
        } else {
            // Player sparkle (if needed in future)
            Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
            if (sparkles != null) {
                ClientSparkle sparkle = sparkles.get(sparkleId);
                if (sparkle != null) {
                    sparkle.setShowTimer(showTimer);
                    sparkle.setEndTimeMs(endTimeMs);
                    sparkle.setIsPreparation(isPreparation);
                    LootSparkle.LOGGER.debug("Synced timer for player sparkle {}: show={}, endTime={}, isPreparation={}", sparkleId, showTimer, endTimeMs, isPreparation);
                }
            }
        }
    }

    /**
     * Called when server sends a phase message packet
     */
    private static void syncPhaseMessage(UUID sparkleId, UUID playerId, String message) {
        LootSparkle.LOGGER.info("[DEBUG] Client received phase message packet: sparkleId={}, playerId={}, message='{}'", sparkleId, playerId, message);
        
        if (playerId == null) {
            // Hostile sparkle
            ClientSparkle sparkle = hostileSparkles.get(sparkleId);
            if (sparkle != null) {
                sparkle.setPhaseMessage(message);
                LootSparkle.LOGGER.info("[DEBUG] Client set phase message '{}' for hostile sparkle {} - sparkle exists: {}", message, sparkleId, sparkle != null);
                LootSparkle.LOGGER.info("[DEBUG] Client sparkle details: position={}, tierLevel={}, showTimer={}, phaseMessage='{}'", 
                    sparkle.getPosition(), sparkle.getTierLevel(), sparkle.isShowTimer(), sparkle.getPhaseMessage());
            } else {
                LootSparkle.LOGGER.warn("[DEBUG] Client received phase message for unknown hostile sparkle {}", sparkleId);
                LootSparkle.LOGGER.warn("[DEBUG] Available hostile sparkles: {}", hostileSparkles.keySet());
            }
        } else {
            // Player sparkle (if needed in future)
            Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
            if (sparkles != null) {
                ClientSparkle sparkle = sparkles.get(sparkleId);
                if (sparkle != null) {
                    sparkle.setPhaseMessage(message);
                    LootSparkle.LOGGER.debug("Synced phase message for player sparkle {}: {}", sparkleId, message);
                }
            }
        }
    }

    /**
     * Clear all client-side sparkle data (called on disconnect)
     */
    private static void clearAllSparkleData() {
        playerSparkles.clear();
        hostileSparkles.clear();
        currentTarget = null;
        currentTargetDistance = Double.MAX_VALUE;
        LootSparkle.LOGGER.info("Cleared all client sparkle data on disconnect");
    }

    /**
     * Get all sparkles for a player (client-side)
     */
    public static List<ClientSparkle> getPlayerSparkles(UUID playerId) {
        Map<UUID, ClientSparkle> sparkles = playerSparkles.get(playerId);
        return sparkles != null ? new ArrayList<>(sparkles.values()) : Collections.emptyList();
    }

    /**
     * Get all hostile sparkles (client-side)
     */
    public static List<ClientSparkle> getHostileSparkles() {
        return new ArrayList<>(hostileSparkles.values());
    }

    // Stable target tracking to prevent compass jumping between nearby sparkles
    private static ClientSparkle currentTarget = null;
    private static double currentTargetDistance = Double.MAX_VALUE;
    private static final double TARGET_SWITCH_THRESHOLD = 4.0; // Only switch if new target is 4 blocks closer

    /**
     * Get the nearest visible sparkle to a player position with stable target tracking
     * Uses hysteresis to prevent rapid switching between nearby sparkles
     * Returns null if no sparkles are available
     */
    public static ClientSparkle getNearestSparkle(double playerX, double playerY, double playerZ) {
        ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Find the absolutely closest sparkle
        for (Map<UUID, ClientSparkle> playerSparkleMap : playerSparkles.values()) {
            for (ClientSparkle sparkle : playerSparkleMap.values()) {
                double distance = sparkle.getPosition().getSquaredDistance(playerX, playerY, playerZ);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = sparkle;
                }
            }
        }

        // If no sparkles found, clear current target
        if (nearest == null) {
            currentTarget = null;
            currentTargetDistance = Double.MAX_VALUE;
            return null;
        }

        // Check if we should switch targets
        boolean shouldSwitchTarget = false;

        if (currentTarget == null) {
            // No current target, so adopt the nearest one
            shouldSwitchTarget = true;
        } else if (!currentTarget.getSparkleId().equals(nearest.getSparkleId())) {
            // Different sparkle is now closest
            // Only switch if it's significantly closer (hysteresis)
            double distanceImprovement = currentTargetDistance - nearestDistance;
            if (distanceImprovement > TARGET_SWITCH_THRESHOLD * TARGET_SWITCH_THRESHOLD) {
                // New target is at least 4 blocks closer, switch to it
                shouldSwitchTarget = true;
            }
        } else {
            // Same target, update distance
            currentTargetDistance = nearestDistance;
        }

        if (shouldSwitchTarget) {
            currentTarget = nearest;
            currentTargetDistance = nearestDistance;
        }

        // Verify current target still exists
        if (currentTarget != null) {
            boolean targetStillExists = false;
            for (Map<UUID, ClientSparkle> playerSparkleMap : playerSparkles.values()) {
                if (playerSparkleMap.containsKey(currentTarget.getSparkleId())) {
                    targetStillExists = true;
                    break;
                }
            }
            if (!targetStillExists) {
                // Target disappeared, clear it
                currentTarget = null;
                currentTargetDistance = Double.MAX_VALUE;
                // Recursively find new target
                return getNearestSparkle(playerX, playerY, playerZ);
            }
        }

        return currentTarget;
    }

    /**
     * Client-side sparkle representation (simplified)
     */
    public static class ClientSparkle {
        private final UUID sparkleId;
        private final UUID playerId;
        private final BlockPos position;
        private final int tierLevel;
        private List<UUID> spawnedMobIds = new ArrayList<>();
        private boolean showTimer = false;
        private long endTimeMs = 0;
        private String phaseMessage = null;
        private boolean isPreparation = false;

        public ClientSparkle(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel) {
            this.sparkleId = sparkleId;
            this.playerId = playerId;
            this.position = position;
            this.tierLevel = tierLevel;
        }

        public UUID getSparkleId() { return sparkleId; }
        public UUID getPlayerId() { return playerId; }
        public BlockPos getPosition() { return position; }
        public int getTierLevel() { return tierLevel; }
        public List<UUID> getSpawnedMobIds() { return spawnedMobIds; }
        public void setSpawnedMobIds(List<UUID> spawnedMobIds) { this.spawnedMobIds = spawnedMobIds; }
        public boolean isShowTimer() { return showTimer; }
        public void setShowTimer(boolean showTimer) { this.showTimer = showTimer; }
        public long getEndTimeMs() { return endTimeMs; }
        public void setEndTimeMs(long endTimeMs) { this.endTimeMs = endTimeMs; }
        public String getPhaseMessage() { return phaseMessage; }
        public void setPhaseMessage(String phaseMessage) { this.phaseMessage = phaseMessage; }
        public boolean isPreparation() { return isPreparation; }
        public void setIsPreparation(boolean isPreparation) { this.isPreparation = isPreparation; }
    }
}