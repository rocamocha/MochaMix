package rocamocha.lootsparkle.sparkle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

import rocamocha.lootsparkle.core.LootSparkle;
import rocamocha.lootsparkle.core.LootSparkleConfig;
import rocamocha.lootsparkle.network.SparkleNetworking;
import rocamocha.lootsparkle.screen.SparkleScreenHandler;

/**
 * Manages sparkle entities/effects in the world
 *
 * Handles:
 * - Sparkle creation and spawning
 * - Sparkle lifecycle management
 * - Per-player sparkle instances
 */
public class SparkleManager {
    // Map of player UUID to their active sparkles
    private static final Map<UUID, List<Sparkle>> playerSparkles = new HashMap<>();

    // Shared hostile sparkles (not per-player)
    private static final List<Sparkle> hostileSparkles = new ArrayList<>();

    // Maximum sparkles per player
    private static final int MAX_SPARKLES_PER_PLAYER = 5;

    // Sparkle spawn radius
    private static final int SPAWN_RADIUS = 36;

    // Minecraft day length in ticks
    private static final long MINECRAFT_DAY_TICKS = 24000L;

    // Target minimum spawns per day
    private static final double MIN_SPAWNS_PER_DAY = 6.0;

    // Real time hours needed per chunk to reach minimum spawn rate
    private static final double HOURS_TO_MIN_RATE = 24.0;

    // Convert hours to ticks (20 TPS * 60 seconds * 60 minutes)
    private static final long TICKS_PER_HOUR = 20 * 60 * 60;

    // Maximum inhabited time per chunk for minimum spawn rate calculation
    private static final long MAX_INHABITED_TIME_PER_CHUNK = (long) HOURS_TO_MIN_RATE * TICKS_PER_HOUR;

    // Reference to server for networking
    private static net.minecraft.server.MinecraftServer server;

    /**
     * Calculates the aggregate inhabited time of all chunks within the spawn radius around a player.
     * This represents how "explored" or "lived-in" the area around the player is.
     */
    private static long calculateAggregateInhabitedTime(ServerWorld world, BlockPos playerPos) {
        long totalInhabitedTime = 0;
        int chunkRadius = (SPAWN_RADIUS + 15) / 16; // Convert block radius to chunk radius

        // Get the chunk coordinates of the player
        int playerChunkX = playerPos.getX() >> 4;
        int playerChunkZ = playerPos.getZ() >> 4;

        // Iterate through all chunks within the spawn radius
        for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                // Check if this chunk is within the actual block distance (not just chunk distance)
                ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
                BlockPos chunkCenter = new BlockPos(chunkPos.getCenterX(), playerPos.getY(), chunkPos.getCenterZ());
                double distance = Math.sqrt(chunkCenter.getSquaredDistance(playerPos));

                if (distance <= SPAWN_RADIUS) {
                    // Get the world chunk and add its inhabited time
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                    if (chunk != null) {
                        totalInhabitedTime += chunk.getInhabitedTime();
                    }
                }
            }
        }

        return totalInhabitedTime;
    }

    /**
     * Calculates the spawn probability based on aggregate inhabited time.
     * Higher inhabited time = lower spawn rate (slower spawns in explored areas).
     * The rate decreases slowly until reaching minimum when chunks have ~24 hours of inhabited time.
     */
    private static float calculateSpawnProbability(long aggregateInhabitedTime) {
        // Maximum spawn probability (when aggregate time is 0) - current default
        final float MAX_SPAWN_PROBABILITY = 0.02f; // 2% chance per tick

        // Minimum spawn probability - target 6-8 spawns per Minecraft day
        final float MIN_SPAWN_PROBABILITY = 7.0f / MINECRAFT_DAY_TICKS; // ~0.0002917

        // Threshold for reaching minimum probability
        // 24 hours * 20 TPS * 60 seconds * 60 minutes * estimated chunks in radius
        // Assuming ~25 chunks in spawn radius, each with 24 hours = 25 * 24 * 3600 * 20
        final long THRESHOLD_INHABITED_TIME = 25L * (long) HOURS_TO_MIN_RATE * TICKS_PER_HOUR;

        if (aggregateInhabitedTime >= THRESHOLD_INHABITED_TIME) {
            return MIN_SPAWN_PROBABILITY;
        }

        // Exponential decay: slowly decrease from max to min probability
        double progress = (double) aggregateInhabitedTime / THRESHOLD_INHABITED_TIME;
        double decayFactor = Math.pow(progress, 0.5); // Square root for slower initial decrease

        return (float) (MAX_SPAWN_PROBABILITY - (MAX_SPAWN_PROBABILITY - MIN_SPAWN_PROBABILITY) * decayFactor);
    }

    /**
     * Calculates the trial sparkle spawn probability based on aggregate inhabited time.
     * Higher inhabited time = higher trial spawn rate (harder challenges in explored areas).
     * Starts at 0 when aggregate time is low, increases as areas become more explored.
     */
    private static float calculateTrialSpawnProbability(long aggregateInhabitedTime) {
        // Maximum trial spawn probability (when aggregate time is high)
        final float MAX_TRIAL_SPAWN_PROBABILITY = 0.01f; // 1% chance per tick (higher than current 0.5%)

        // Minimum trial spawn probability (when aggregate time is 0)
        final float MIN_TRIAL_SPAWN_PROBABILITY = 0.0f; // 0% chance when area is unexplored

        // Threshold for reaching maximum probability (same as normal sparkles)
        final long THRESHOLD_INHABITED_TIME = 25L * (long) HOURS_TO_MIN_RATE * TICKS_PER_HOUR;

        if (aggregateInhabitedTime <= 0) {
            return MIN_TRIAL_SPAWN_PROBABILITY;
        }

        if (aggregateInhabitedTime >= THRESHOLD_INHABITED_TIME) {
            return MAX_TRIAL_SPAWN_PROBABILITY;
        }

        // Linear increase from min to max probability
        double progress = (double) aggregateInhabitedTime / THRESHOLD_INHABITED_TIME;

        return (float) (MIN_TRIAL_SPAWN_PROBABILITY + (MAX_TRIAL_SPAWN_PROBABILITY - MIN_TRIAL_SPAWN_PROBABILITY) * progress);
    }

    public static void initialize() {
        // Store server reference for networking
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);

        // Clean up all entities when server stops (handles single player save/quit)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            cleanupAllEntitiesOnServerStop(s);
        });

        // Clean up orphaned entities when worlds load (handles world reloads)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register((server, world) -> {
            cleanupOrphanedEntitiesOnWorldLoad(world);
        });

        // Register server tick event to update sparkles
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            for (ServerWorld world : s.getWorlds()) {
                updateSparkles(world);
            }
        });
    }

    /**
     * Spawns a new sparkle for a player at a random location
     */
    public static void spawnSparkleForPlayer(UUID playerId, World world, BlockPos center) {
        List<Sparkle> sparkles = playerSparkles.computeIfAbsent(playerId, k -> new ArrayList<>());

        // Remove expired sparkles
        sparkles.removeIf(Sparkle::isExpired);

        // Check if we can spawn more sparkles
        if (sparkles.size() >= MAX_SPARKLES_PER_PLAYER) {
            return;
        }

        // Check if sky is visible to the player at their current position
        boolean skyVisibleToPlayer = world.isSkyVisible(center);

        // Find a random valid position with sky visibility constraints
        BlockPos spawnPos = findValidSpawnPosition(world, center, SPAWN_RADIUS, skyVisibleToPlayer);
        if (spawnPos != null) {
            Sparkle sparkle = new Sparkle(playerId, spawnPos, world);
            sparkles.add(sparkle);

            // Send sync packet to the player
            sendSparkleSyncPacket(playerId, sparkle);

        }
    }

    /**
     * Spawns a new hostile sparkle at a random location (shared among all players)
     */
    public static void spawnHostileSparkle(World world, BlockPos center) {
        // Check if we can spawn more hostile sparkles (limit to prevent spam)
        if (hostileSparkles.size() >= 3) { // Max 3 hostile sparkles at once
            return;
        }

        // Check if sky is visible to the player at their current position
        boolean skyVisibleToPlayer = world.isSkyVisible(center);

        // Find a random valid position with sky visibility constraints and minimum distance from other hostile sparkles
        BlockPos spawnPos = findValidHostileSpawnPosition(world, center, SPAWN_RADIUS, skyVisibleToPlayer);
        if (spawnPos != null) {
            // Force a hostile tier (select randomly from hostile tiers)
            SparkleTier hostileTier = selectRandomHostileTier(world, spawnPos);
            Sparkle sparkle = new Sparkle(null, spawnPos, world, hostileTier); // null playerId for shared sparkles
            hostileSparkles.add(sparkle);

            // Send sync packet to all players
            sendHostileSparkleSyncPacket(sparkle);
        }
    }

    /**
     * Spawns a sparkle of a specific tier for a player at a given location
     */
    public static void spawnSparkleOfTierForPlayer(UUID playerId, World world, BlockPos position, SparkleTier tier) {
        List<Sparkle> sparkles = playerSparkles.computeIfAbsent(playerId, k -> new ArrayList<>());

        Sparkle sparkle = new Sparkle(playerId, position, world, tier);
        sparkles.add(sparkle);

        // Send sync packet to the player
        sendSparkleSyncPacket(playerId, sparkle);

    }

    /**
     * Selects a random hostile tier based on weights
     */
    private static SparkleTier selectRandomHostileTier(World world, BlockPos position) {
        // Get all hostile tiers and their weights
        SparkleTier[] allTiers = SparkleTier.values();
        int totalWeight = 0;
        
        // Calculate total weight for hostile tiers only
        for (SparkleTier tier : allTiers) {
            if (tier.getCategory() == SparkleCategory.TRIAL) {
                totalWeight += tier.getWeight();
            }
        }
        
        if (totalWeight == 0) {
            // Fallback to CURSED if no hostile tiers have weight
            return SparkleTier.CURSED;
        }
        
        // Select tier based on weights
        int roll = world.getRandom().nextInt(totalWeight);
        int currentWeight = 0;
        
        for (SparkleTier tier : allTiers) {
            if (tier.getCategory() == SparkleCategory.TRIAL) {
                currentWeight += tier.getWeight();
                if (roll < currentWeight) {
                    return tier;
                }
            }
        }
        
        // Fallback to CURSED
        return SparkleTier.CURSED;
    }

    /**
     * Gets all active sparkles for a player
     */
    public static List<Sparkle> getPlayerSparkles(UUID playerId) {
        return playerSparkles.getOrDefault(playerId, Collections.emptyList());
    }

    /**
     * Forces all sparkles to expire immediately (debug command)
     * @return The number of sparkles that were expired
     */
    public static int expireAllSparkles() {
        int expiredCount = 0;
        for (Map.Entry<UUID, List<Sparkle>> entry : playerSparkles.entrySet()) {
            UUID playerId = entry.getKey();
            List<Sparkle> playerSparkleList = entry.getValue();
            
            for (Sparkle sparkle : playerSparkleList) {
                // Clean up all associated entities before removing
                if (server != null) {
                    ServerWorld world = server.getWorld(net.minecraft.world.World.OVERWORLD);
                    if (world != null) {
                        sparkle.cleanupAllEntities(world);
                    }
                }
                // Send remove packet to the player for each sparkle
                sendSparkleRemovePacket(playerId, sparkle.getSparkleId());
                expiredCount++;
            }
            
            playerSparkleList.clear();
        }
        return expiredCount;
    }

    /**
     * Forces all hostile sparkles to expire immediately (debug command)
     * @return The number of hostile sparkles that were expired
     */
    public static int expireAllHostileSparkles() {
        int expiredCount = 0;
        for (Sparkle sparkle : hostileSparkles) {
            // Clean up all associated entities before removing
            if (server != null) {
                ServerWorld world = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (world != null) {
                    sparkle.cleanupAllEntities(world);
                }
            }
            // Send remove packet to all players for each hostile sparkle
            sendHostileSparkleRemovePacket(sparkle.getSparkleId());
            expiredCount++;
        }
        hostileSparkles.clear();
        return expiredCount;
    }

    /**
     * Removes a sparkle
     */
    public static void removeSparkle(UUID playerId, Sparkle sparkle) {
        List<Sparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null && sparkles.remove(sparkle)) {
            // Clean up all associated entities before removing
            if (server != null) {
                ServerWorld world = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (world != null) {
                    sparkle.cleanupAllEntities(world);
                }
            }
            // Send remove packet to the player
            sendSparkleRemovePacket(playerId, sparkle.getSparkleId());
        }
    }

    private static void updateSparkles(ServerWorld world) {
        // Collect underwater respawn requests to avoid modifying lists during iteration
        java.util.List<UUID> underwaterRespawnOwners = new java.util.ArrayList<>();

        // Update all player sparkles and remove expired ones
        playerSparkles.values().forEach(sparkles ->
            sparkles.removeIf(sparkle -> {
                sparkle.update(world);
                boolean expired = sparkle.isExpired() || isSparkleExpiredDueToDistance(sparkle, world);
                if (expired) {
                    // Capture data before cleanup
                    UUID ownerId = sparkle.getPlayerId();
                    SparkleTier tier = sparkle.getTier();
                    // Clean up all associated entities before removing
                    sparkle.cleanupAllEntities(world);
                    // Send remove packet to the player
                    sendSparkleRemovePacket(ownerId, sparkle.getSparkleId());

                    // Eldertide Resonance L3: queue underwater respawn when an underwater sparkle expires
                    if (tier.getCategory() == SparkleCategory.UNDERWATER) {
                        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerId);
                        if (owner != null && owner.isAlive() && !owner.isSpectator()) {
                            int eldertide = getEldertideLevel(owner);
                            if (eldertide >= 3 && owner.isSubmergedInWater() && getDiversCrystalLevel(owner) > 0 && hasTreasureCompass(owner)) {
                                // Defer respawn until after iteration to avoid ConcurrentModificationException
                                underwaterRespawnOwners.add(ownerId);
                            }
                        }
                    }

                    return true;
                }
                return false;
            })
        );

        // Process deferred underwater respawns now that iteration is complete
        if (!underwaterRespawnOwners.isEmpty()) {
            for (UUID ownerId : underwaterRespawnOwners) {
                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerId);
                if (owner != null && owner.isAlive() && !owner.isSpectator()) {
                    spawnUnderwaterSparkleForPlayer(owner, world);
                }
            }
        }

        // Update hostile sparkles and check for activation
        hostileSparkles.removeIf(sparkle -> {
            sparkle.update(world);
            if (sparkle.isExpired()) {
                // Clean up all associated entities before removing
                sparkle.cleanupAllEntities(world);
                // Send remove packet to all players
                sendHostileSparkleRemovePacket(sparkle.getSparkleId());
                return true;
            }

            // Check activation for all players
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !player.isSpectator()) {
                    sparkle.checkActivation(player);
                }
            }

            return false;
        });

        // Spawn sparkles for active players
        spawnSparklesForActivePlayers(world);

        // Spawn underwater sparkles for eligible players
        spawnUnderwaterSparklesForActivePlayers(world);

        // Spawn hostile sparkles occasionally
        spawnHostileSparkles(world);
    }

    private static void spawnSparklesForActivePlayers(ServerWorld world) {
        // Get all players in the world
        for (ServerPlayerEntity player : world.getPlayers()) {
            // Only spawn for players who are not in spectator mode and are alive
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }

            // Check if this player already has too many sparkles
            List<Sparkle> playerSparklesList = playerSparkles.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
            if (playerSparklesList.size() >= MAX_SPARKLES_PER_PLAYER) {
                continue;
            }

            // Calculate aggregate inhabited time for chunks around this player
            long aggregateInhabitedTime = calculateAggregateInhabitedTime(world, player.getBlockPos());

            // Calculate spawn probability based on aggregate inhabited time
            // Higher inhabited time = lower spawn rate (slower spawns in explored areas)
            float spawnProbability = calculateSpawnProbability(aggregateInhabitedTime);

            // Random chance to spawn a sparkle
            if (world.getRandom().nextFloat() < spawnProbability) {
                spawnSparkleForPlayer(player.getUuid(), world, player.getBlockPos());
            }
        }
    }

    private static void spawnHostileSparkles(ServerWorld world) {
        // Get all players in the world
        List<ServerPlayerEntity> players = world.getPlayers();

        if (players.isEmpty()) return;

        // Pick a random player as the "center" for hostile sparkle spawning
        ServerPlayerEntity randomPlayer = players.get(world.getRandom().nextInt(players.size()));

        // Only spawn for alive, non-spectator players
        if (randomPlayer.isSpectator() || !randomPlayer.isAlive()) {
            return;
        }

        // Calculate aggregate inhabited time for chunks around this player
        long aggregateInhabitedTime = calculateAggregateInhabitedTime(world, randomPlayer.getBlockPos());

        // Require at least one nearby player with Curse of Treasure within 48 blocks
        if (!hasCursedPlayerWithinRadius(world, randomPlayer.getBlockPos(), 48.0)) {
            return;
        }

        // Calculate trial sparkle spawn probability based on aggregate inhabited time
        // Higher inhabited time = higher trial spawn rate (harder in explored areas)
        float trialSpawnProbability = calculateTrialSpawnProbability(aggregateInhabitedTime);

        // Random chance to spawn hostile sparkles
        if (world.getRandom().nextFloat() < trialSpawnProbability) {
            spawnHostileSparkle(world, randomPlayer.getBlockPos());
        }
    }

    private static boolean hasCursedPlayerWithinRadius(ServerWorld world, BlockPos center, double radius) {
        double r2 = radius * radius;
        for (ServerPlayerEntity p : world.getPlayers()) {
            if (!p.isAlive() || p.isSpectator()) continue;
            if (p.getBlockPos().getSquaredDistance(center) > r2) continue;
            if (hasTreasureCurse(p)) return true;
        }
        return false;
    }

    private static boolean hasTreasureCurse(ServerPlayerEntity player) {
        java.util.function.Function<net.minecraft.item.ItemStack, Integer> levelFn = (stack) -> {
            var ench = stack.getEnchantments();
            for (var entry : ench.getEnchantments()) {
                if (entry.getIdAsString().equals("loot-sparkle:curse_of_treasure")) {
                    return ench.getLevel(entry);
                }
            }
            return 0;
        };

        // Check compass in main hand, offhand, then hotbar
        var main = player.getMainHandStack();
        if (main.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
            if (levelFn.apply(main) > 0) return true;
        }
        var off = player.getOffHandStack();
        if (off.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
            if (levelFn.apply(off) > 0) return true;
        }
        for (int slot = 0; slot < 9; slot++) {
            var st = player.getInventory().getStack(slot);
            if (st.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
                if (levelFn.apply(st) > 0) return true;
            }
        }
        return false;
    }

    private static boolean hasTreasureCompass(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) return true;
        if (player.getOffHandStack().getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) return true;
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) return true;
        }
        return false;
    }

    private static int getDiversCrystalLevel(ServerPlayerEntity player) {
        // Check main hand, off hand, then hotbar for the treasure compass
        java.util.function.Function<net.minecraft.item.ItemStack, Integer> levelFn = (stack) -> {
            var ench = stack.getEnchantments();
            for (var entry : ench.getEnchantments()) {
                if (entry.getIdAsString().equals("loot-sparkle:divers_crystal")) {
                    return ench.getLevel(entry);
                }
            }
            return 0;
        };

        var main = player.getMainHandStack();
        if (main.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
            int lvl = levelFn.apply(main);
            if (lvl > 0) return lvl;
        }
        var off = player.getOffHandStack();
        if (off.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
            int lvl = levelFn.apply(off);
            if (lvl > 0) return lvl;
        }
        for (int slot = 0; slot < 9; slot++) {
            var st = player.getInventory().getStack(slot);
            if (st.getItem() == rocamocha.lootsparkle.core.LootSparkle.TREASURE_COMPASS) {
                int lvl = levelFn.apply(st);
                if (lvl > 0) return lvl;
            }
        }
        return 0;
    }

    private static int getEldertideLevel(ServerPlayerEntity player) {
        java.util.function.Function<net.minecraft.item.ItemStack, Integer> levelFn = (stack) -> {
            var ench = stack.getEnchantments();
            for (var entry : ench.getEnchantments()) {
                if (entry.getIdAsString().equals("loot-sparkle:eldertide_resonance")) {
                    return ench.getLevel(entry);
                }
            }
            return 0;
        };

        int level = 0;
        var main = player.getMainHandStack();
        if (main.isOf(net.minecraft.item.Items.TRIDENT)) level = Math.max(level, levelFn.apply(main));
        var off = player.getOffHandStack();
        if (off.isOf(net.minecraft.item.Items.TRIDENT)) level = Math.max(level, levelFn.apply(off));
        return level;
    }

    /**
     * Spawns underwater sparkles for eligible players.
     * Rules:
     * - Requires an active Treasure Compass for any underwater tier consideration.
     * - When submerged: all underwater tiers are eligible, gated by Diver's Crystal levels.
     *   - L0: driftwood only
     *   - L1: + kelp, coral
     *   - L2: + cavern
     *   - L3: + seabed
     * - When not submerged: only the driftwood exception is considered (may spawn near water surface).
     * - Spawns share the same per-player cap as normal sparkles and use the same probability curve.
     * - Eldertide Resonance L3: expired underwater sparkles will queue an immediate respawn (same tick) after iteration to avoid CME.
     */
    private static void spawnUnderwaterSparklesForActivePlayers(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || !player.isAlive()) continue;

            // Require an active Treasure Compass for all underwater spawns (driftwood does not require the enchantment)
            if (!hasTreasureCompass(player)) continue;

            // Respect per-player sparkle limit (shared with normal sparkles)
            List<Sparkle> list = playerSparkles.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
            if (list.size() >= MAX_SPARKLES_PER_PLAYER) continue;

            boolean submerged = player.isSubmergedInWater();
            int divers = getDiversCrystalLevel(player); // May be 0; driftwood is allowed at 0

            long aggregateInhabitedTime = calculateAggregateInhabitedTime(world, player.getBlockPos());
            float spawnProbability = calculateSpawnProbability(aggregateInhabitedTime);
            if (world.getRandom().nextFloat() >= spawnProbability) continue;

            if (submerged) {
                // When submerged: allow normal underwater spawning; with Divers>0 includes higher tiers; with 0 it will attempt driftwood only
                spawnUnderwaterSparkleForPlayer(player, world);
            } else {
                // On land: only allow the driftwood exception
                BlockPos spawnPos = findUnderwaterSpawnPosition(world, player.getBlockPos(), SPAWN_RADIUS, SparkleTier.DRIFTWOOD);
                if (spawnPos != null) {
                    spawnSparkleOfTierForPlayer(player.getUuid(), world, spawnPos, SparkleTier.DRIFTWOOD);
                }
            }
        }
    }

    private static void spawnUnderwaterSparkleForPlayer(ServerPlayerEntity player, World world) {
        int diversLevel = getDiversCrystalLevel(player);

        // Determine candidate tiers based on level
        java.util.List<SparkleTier> candidates = new java.util.ArrayList<>();
        // Driftwood always allowed (even without Divers Crystal per spec), but here we already have diversLevel>0 for normal path
        candidates.add(SparkleTier.DRIFTWOOD);
        if (diversLevel >= 1) {
            candidates.add(SparkleTier.KELP);
            candidates.add(SparkleTier.CORAL);
        }
        if (diversLevel >= 2) {
            candidates.add(SparkleTier.CAVERN);
        }
        if (diversLevel >= 3) {
            candidates.add(SparkleTier.SEABED);
        }

        java.util.Collections.shuffle(candidates);

        BlockPos center = player.getBlockPos();
        BlockPos spawnPos = null;
        SparkleTier chosenTier = null;
        for (SparkleTier t : candidates) {
            spawnPos = findUnderwaterSpawnPosition(world, center, SPAWN_RADIUS, t);
            if (spawnPos != null) {
                chosenTier = t;
                break;
            }
        }

        // As a last resort, try driftwood near any water surface even if player on land
        if (spawnPos == null) {
            spawnPos = findUnderwaterSpawnPosition(world, center, SPAWN_RADIUS, SparkleTier.DRIFTWOOD);
            if (spawnPos != null) chosenTier = SparkleTier.DRIFTWOOD;
        }

        if (spawnPos != null && chosenTier != null) {
            spawnSparkleOfTierForPlayer(player.getUuid(), world, spawnPos, chosenTier);
        }
    }

    private static BlockPos findUnderwaterSpawnPosition(World world, BlockPos center, int radius, SparkleTier tier) {
        java.util.Random random = new java.util.Random();
        for (int attempts = 0; attempts < 64; attempts++) {
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;

            switch (tier) {
                case DRIFTWOOD -> {
                    // Find water surface: a water block with air above
                    for (int y = Math.min(center.getY() + 16, world.getTopY() - 2); y >= world.getBottomY() + 1; y--) {
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = world.getBlockState(pos);
                        if (state.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                            BlockPos above = pos.up();
                            if (world.getBlockState(above).isAir()) {
                                // Place sparkle just above the surface for visibility
                                return above;
                            }
                        }
                    }
                }
                case KELP -> {
                    // Search small vertical range for kelp blocks
                    for (int y = center.getY() + 16; y >= center.getY() - 16; y--) {
                        if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) continue;
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = world.getBlockState(pos);
                        if (state.isOf(net.minecraft.block.Blocks.KELP) || state.isOf(net.minecraft.block.Blocks.KELP_PLANT)) {
                            // Spawn inside water near kelp
                            return pos.up();
                        }
                    }
                }
                case CORAL -> {
                    // Look for coral blocks via tag if available, else check common coral blocks
                    for (int y = center.getY() + 16; y >= center.getY() - 16; y--) {
                        if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) continue;
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = world.getBlockState(pos);
                        boolean isCoral = state.isIn(net.minecraft.registry.tag.BlockTags.CORAL_BLOCKS)
                            || state.isOf(net.minecraft.block.Blocks.TUBE_CORAL_BLOCK)
                            || state.isOf(net.minecraft.block.Blocks.BRAIN_CORAL_BLOCK)
                            || state.isOf(net.minecraft.block.Blocks.BUBBLE_CORAL_BLOCK)
                            || state.isOf(net.minecraft.block.Blocks.FIRE_CORAL_BLOCK)
                            || state.isOf(net.minecraft.block.Blocks.HORN_CORAL_BLOCK);
                        if (isCoral) {
                            // Choose adjacent water block if coral block itself is solid
                            BlockPos waterPos = findAdjacentWater(world, pos);
                            if (waterPos != null) return waterPos;
                        }
                    }
                }
                case CAVERN -> {
                    // Find water-filled enclosed spaces (no sky visibility)
                    for (int y = center.getY() + 16; y >= center.getY() - 32; y--) {
                        if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) continue;
                        BlockPos pos = new BlockPos(x, y, z);
                        var state = world.getBlockState(pos);
                        if (state.getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER) && !world.isSkyVisible(pos)) {
                            return pos;
                        }
                    }
                }
                case SEABED -> {
                    // Find ocean floor: solid block with water above and minimum depth
                    for (int y = center.getY(); y >= world.getBottomY() + 1; y--) {
                        BlockPos floor = new BlockPos(x, y, z);
                        var floorState = world.getBlockState(floor);
                        if (floorState.isSolidBlock(world, floor) && world.getBlockState(floor.up()).getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                            int depth = 0;
                            BlockPos check = floor.up();
                            while (world.getBlockState(check).getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER) && depth < 16) {
                                depth++;
                                check = check.up();
                            }
                            if (depth >= 5) {
                                return floor.up(); // place sparkle one block above floor in water
                            }
                        }
                    }
                }
                default -> {
                    // Not an underwater tier
                }
            }
        }
        return null;
    }

    private static BlockPos findAdjacentWater(World world, BlockPos pos) {
        BlockPos[] neighbors = new BlockPos[]{pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west()};
        for (BlockPos n : neighbors) {
            if (world.getBlockState(n).getFluidState().isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                return n;
            }
        }
        return null;
    }

    private static BlockPos findValidSpawnPosition(World world, BlockPos center, int radius, boolean skyVisibleToPlayer) {
        Random random = new Random();

        for (int attempts = 0; attempts < 50; attempts++) {
            // Generate random position within radius
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;

            // For cave spawning, search within vertical radius around player
            int playerY = center.getY();
            int verticalRadius = LootSparkleConfig.getVerticalSpawnRadius();

            // Search for valid positions within vertical range
            for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                int y = playerY + yOffset;

                // Stay within world bounds
                if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);

                // Check if position is valid
                if (isValidSpawnPosition(world, pos)) {
                    // Check sky visibility constraint
                    boolean skyVisibleAtPos = world.isSkyVisible(pos);
                    if (skyVisibleToPlayer == skyVisibleAtPos) {  // Match sky visibility
                        // If spawning in non-sky-visible location, check reachability with simple pathfinding
                        if (!skyVisibleAtPos && !isReachable(world, center, pos, 36)) {
                            continue;  // Skip if not reachable
                        }
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Sends a sparkle sync packet to the player
     */
    private static void sendSparkleSyncPacket(UUID playerId, Sparkle sparkle) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                ServerPlayNetworking.send(player, new SparkleNetworking.SyncSparklePacket(
                    sparkle.getSparkleId(),
                    playerId,
                    sparkle.getPosition(),
                    sparkle.getTier().getLevel(),
                    sparkle.getCurrentPhaseMessage()
                ));
            }
        }
    }

    /**
     * Sends a hostile sparkle sync packet to all players
     */
    private static void sendHostileSparkleSyncPacket(Sparkle sparkle) {
        if (server != null) {
            // Send to all players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, new SparkleNetworking.SyncSparklePacket(
                    sparkle.getSparkleId(),
                    null, // null playerId for hostile sparkles
                    sparkle.getPosition(),
                    sparkle.getTier().getLevel(),
                    sparkle.getCurrentPhaseMessage()
                ));
            }
        }
    }

    /**
     * Sends a sparkle remove packet to the player
     */
    private static void sendSparkleRemovePacket(UUID playerId, UUID sparkleId) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                ServerPlayNetworking.send(player, new SparkleNetworking.RemoveSparklePacket(sparkleId, playerId));
                // Clear any phase message for this sparkle
                ServerPlayNetworking.send(player, new SparkleNetworking.PhaseMessagePacket(sparkleId, playerId, null));
            }
        }
    }

    /**
     * Sends a hostile sparkle remove packet to all players
     */
    private static void sendHostileSparkleRemovePacket(UUID sparkleId) {
        if (server != null) {
            // Send to all players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, new SparkleNetworking.RemoveSparklePacket(sparkleId, null));
                // Clear any phase message for this sparkle
                ServerPlayNetworking.send(player, new SparkleNetworking.PhaseMessagePacket(sparkleId, null, null));
            }
        }
    }

    /**
     * Sends an interaction failed packet to the player
     */
    private static void sendInteractionFailedPacket(ServerPlayerEntity player, String reason) {
        ServerPlayNetworking.send(player, new SparkleNetworking.InteractionFailedPacket(reason));
    }

    /**
     * Remove a sparkle if its inventory is empty
     */
    public static void removeSparkleIfEmpty(UUID playerId, SimpleInventory inventory) {
        List<Sparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null) {
            sparkles.removeIf(sparkle -> {
                if (sparkle.getInventory() == inventory && sparkle.isInventoryEmpty()) {
                    // Clean up all associated entities before removing
                    if (server != null) {
                        ServerWorld world = server.getWorld(net.minecraft.world.World.OVERWORLD);
                        if (world != null) {
                            sparkle.cleanupAllEntities(world);
                        }
                    }
                    // Send remove packet to the player
                    sendSparkleRemovePacket(playerId, sparkle.getSparkleId());
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * Trigger sparkle interaction for a player
     */
    public static void triggerSparkleInteraction(ServerPlayerEntity player, UUID sparkleId) {
        // First check player sparkles
        List<Sparkle> sparkles = playerSparkles.get(player.getUuid());
        if (sparkles != null) {
            for (Sparkle sparkle : sparkles) {
                if (sparkle.getSparkleId().equals(sparkleId)) {
                    handleSparkleInteraction(player, sparkle);
                    return;
                }
            }
        }

        // Then check hostile sparkles
        for (Sparkle sparkle : hostileSparkles) {
            if (sparkle.getSparkleId().equals(sparkleId)) {
                handleSparkleInteraction(player, sparkle);
                return;
            }
        }

        sendInteractionFailedPacket(player, "This sparkle no longer exists");
    }

    /**
     * Handle interaction with a specific sparkle
     */
    private static void handleSparkleInteraction(ServerPlayerEntity player, Sparkle sparkle) {
        // Verify player is close enough to the sparkle (same distance check as client)
        double distance = player.getPos().distanceTo(Vec3d.ofCenter(sparkle.getPosition()));
        if (distance > 3.0) {
            sendInteractionFailedPacket(player, "You are too far away from the sparkle");
            return;
        }

        // Force activate trial sparkle if not already activated
        if (sparkle.isTrial() && !sparkle.isActivated()) {
            sparkle.forceActivate(player);
            // Send updated sync packet to ensure clients have the sparkle with the current phase message
            sendHostileSparkleSyncPacket(sparkle);
        }

        // Check if hostile sparkle phases are complete
        if (sparkle.isTrial() && (!sparkle.areAllPhasesCompleted() || sparkle.isExpired())) {
            sendInteractionFailedPacket(player, sparkle.isExpired() ? "This hostile sparkle has expired!" : "This hostile sparkle is not yet safe to approach!");
            return;
        }

        // Only spawn experience orbs if they haven't been granted yet
        if (!sparkle.isExperienceGranted()) {
            // Spawn experience orbs based on sparkle tier
            int experienceAmount = getExperienceForTier(sparkle.getTier());
            spawnExperienceOrbs(player.getWorld(), sparkle.getPosition(), experienceAmount);
            sparkle.setExperienceGranted(true);
        }

        openSparkleInventory(player.getUuid(), sparkle);
    }

    /**
     * Open sparkle inventory for a player
     */
    private static void openSparkleInventory(UUID playerId, Sparkle sparkle) {
        if (server != null) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                // Open the sparkle inventory screen
                player.openHandledScreen(new SparkleScreenHandler.Factory(sparkle));
            }
        }
    }

    /**
     * Gets the experience amount to award for interacting with a sparkle of the given tier
     */
    private static int getExperienceForTier(SparkleTier tier) {
        return switch (tier) {
            case COMMON -> 1;
            case UNCOMMON -> 2;
            case RARE -> 3;
            case EPIC -> 5;
            case LEGENDARY -> 8;
            case DIVINE -> 12;
            case CURSED -> 3;
            case BLIGHTED -> 6;
            case DOOMED -> 10;
            case DRIFTWOOD -> 1;
            case KELP -> 2;
            case CORAL -> 3;
            case CAVERN -> 4;
            case SEABED -> 5;
        };
    }

    /**
     * Spawns experience orbs at the specified position
     */
    private static void spawnExperienceOrbs(World world, BlockPos position, int experienceAmount) {
        if (world instanceof ServerWorld serverWorld) {
            // Create experience orb at the sparkle's position with a small random offset
            double x = position.getX() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 0.5;
            double y = position.getY() + 0.5;
            double z = position.getZ() + 0.5 + (world.getRandom().nextDouble() - 0.5) * 0.5;
            
            ExperienceOrbEntity orb = new ExperienceOrbEntity(serverWorld, x, y, z, experienceAmount);
            serverWorld.spawnEntity(orb);
        }
    }

    /**
     * Checks if a sparkle should expire due to player distance
     */
    private static boolean isSparkleExpiredDueToDistance(Sparkle sparkle, ServerWorld world) {
        // Get the player associated with this sparkle
        ServerPlayerEntity player = (ServerPlayerEntity) world.getPlayerByUuid(sparkle.getPlayerId());
        if (player == null) {
            // Player is not online, don't expire due to distance
            return false;
        }

        // Calculate distance between player and sparkle
        double distance = player.getPos().distanceTo(Vec3d.ofCenter(sparkle.getPosition()));
        
        // Base lifetime in milliseconds
        long baseLifetime = LootSparkleConfig.getSparkleLifetimeMs();
        
        // Distance-based lifetime multiplier
        // Closer = longer lifetime, further = shorter lifetime
        // At 0 blocks: 1.0x lifetime (normal)
        // At 32 blocks: 0.5x lifetime (half)
        // At 64 blocks: 0.25x lifetime (quarter)
        // At 128+ blocks: 0.1x lifetime (tenth)
        double distanceMultiplier = Math.max(0.1, 1.0 - (distance / 128.0));
        
        // Calculate effective lifetime
        long effectiveLifetime = (long) (baseLifetime * distanceMultiplier);
        
        // Check if sparkle has exceeded its effective lifetime
        long age = System.currentTimeMillis() - sparkle.getCreationTime();
        return age > effectiveLifetime;
    }

    /**
     * Checks if a position is valid for sparkle spawning
     */
    private static boolean isValidSpawnPosition(World world, BlockPos pos) {
        // Must be air block
        if (!world.getBlockState(pos).isAir()) {
            return false;
        }

        // Must have solid block below
        BlockPos below = pos.down();
        return world.getBlockState(below).isSolidBlock(world, below);
    }

    /**
     * Simple pathfinding check to ensure the sparkle is reachable within the given radius
     * Uses a basic breadth-first search to check if there's a clear path
     */
    private static boolean isReachable(World world, BlockPos start, BlockPos end, int maxRadius) {
        // Check distance first
        if (start.getSquaredDistance(end) > maxRadius * maxRadius) {
            return false;
        }

        // Simple BFS for reachability (checks for solid blocks blocking the path)
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        int[][] directions = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0},  // Horizontal movement
            {0, 1, 0}, {0, -1, 0}  // Vertical movement for caves
        };

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (current.equals(end)) {
                return true;
            }

            for (int[] dir : directions) {
                BlockPos next = current.add(dir[0], dir[1], dir[2]);
                if (!visited.contains(next) && next.getSquaredDistance(start) <= maxRadius * maxRadius) {
                    // Check if the block is passable and grounded
                    if (isPassableAndGrounded(world, next)) {
                        visited.add(next);
                        queue.add(next);
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks if a block position is passable and has solid ground within 4 blocks below
     * This prevents pathfinding through large open spaces (flying)
     */
    private static boolean isPassableAndGrounded(World world, BlockPos pos) {
        // First check if the block itself is passable
        if (!world.getBlockState(pos).isAir() && world.getBlockState(pos).isSolidBlock(world, pos)) {
            return false; // Solid blocks are not passable
        }

        // Check for solid ground within 4 blocks below
        for (int yOffset = 1; yOffset <= 4; yOffset++) {
            BlockPos checkPos = pos.down(yOffset);
            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos)) {
                return true; // Found solid ground within 4 blocks
            }
        }

        return false; // No solid ground found within 4 blocks below
    }

    /**
     * Finds a valid spawn position for hostile sparkles with minimum distance constraints
     */
    private static BlockPos findValidHostileSpawnPosition(World world, BlockPos center, int radius, boolean skyVisibleToPlayer) {
        Random random = new Random();
        double minDistance = LootSparkleConfig.getHostileSparkleMinDistance();

        for (int attempts = 0; attempts < 50; attempts++) {
            // Generate random position within radius
            int x = center.getX() + random.nextInt(radius * 2) - radius;
            int z = center.getZ() + random.nextInt(radius * 2) - radius;

            // For cave spawning, search within vertical radius around player
            int playerY = center.getY();
            int verticalRadius = LootSparkleConfig.getVerticalSpawnRadius();

            // Search for valid positions within vertical range
            for (int yOffset = -verticalRadius; yOffset <= verticalRadius; yOffset++) {
                int y = playerY + yOffset;

                // Stay within world bounds
                if (y <= world.getBottomY() + 1 || y >= world.getTopY() - 1) {
                    continue;
                }

                BlockPos pos = new BlockPos(x, y, z);

                // Check if position is valid
                if (isValidSpawnPosition(world, pos)) {
                    // Check sky visibility constraint
                    boolean skyVisibleAtPos = world.isSkyVisible(pos);
                    if (skyVisibleToPlayer == skyVisibleAtPos) {  // Match sky visibility
                        // Check minimum distance from the player who triggered the spawn
                        double distanceFromPlayer = Math.sqrt(pos.getSquaredDistance(center));
                        if (distanceFromPlayer < 4.0) {  // At least 4 blocks from player
                            continue;
                        }

                        // Check minimum distance from existing hostile sparkles
                        boolean tooClose = false;
                        for (Sparkle existingSparkle : hostileSparkles) {
                            double distance = Math.sqrt(existingSparkle.getPosition().getSquaredDistance(pos));
                            if (distance < minDistance) {
                                tooClose = true;
                                break;
                            }
                        }

                        if (!tooClose) {
                            // If spawning in non-sky-visible location, check reachability with simple pathfinding
                            if (!skyVisibleAtPos && !isReachable(world, center, pos, 36)) {
                                continue;  // Skip if not reachable
                            }
                            return pos;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Handle player disconnect by cleaning up all entities from their sparkles
     */
    public static void onPlayerDisconnect(UUID playerId) {
        List<Sparkle> sparkles = playerSparkles.get(playerId);
        if (sparkles != null) {
            // Clean up all entities from this player's sparkles
            if (server != null) {
                ServerWorld world = server.getWorld(net.minecraft.world.World.OVERWORLD);
                if (world != null) {
                    for (Sparkle sparkle : sparkles) {
                        sparkle.cleanupAllEntities(world);
                    }
                }
            }
        }
    }

    /**
     * Clean up all entities from active sparkles when server stops
     */
    private static void cleanupAllEntitiesOnServerStop(MinecraftServer server) {
        // Clean up entities from all player sparkles and expire them
        for (List<Sparkle> sparkles : playerSparkles.values()) {
            for (Sparkle sparkle : sparkles) {
                for (ServerWorld world : server.getWorlds()) {
                    sparkle.cleanupAllEntities(world);
                }
            }
        }

        // Clean up entities from hostile sparkles
        for (Sparkle sparkle : hostileSparkles) {
            for (ServerWorld world : server.getWorlds()) {
                sparkle.cleanupAllEntities(world);
            }
        }

        // Expire all sparkles since server is stopping
        playerSparkles.clear();
        hostileSparkles.clear();

        LootSparkle.LOGGER.info("Expired all sparkles on server stop");
    }

    /**
     * Clean up orphaned entities when a world loads (handles world reloads)
     */
    private static void cleanupOrphanedEntitiesOnWorldLoad(ServerWorld world) {
        // Since sparkle state is not persisted, any custom sparkle entities in the world
        // when it loads are orphaned and should be cleaned up. Our target entities are
        // instances of a subclass of FallingBlockEntity (not a custom registered type),
        // so detect them by class rather than registry id.

        int removed = 0;
        for (net.minecraft.entity.Entity entity : world.iterateEntities()) {
            if (entity instanceof rocamocha.lootsparkle.trial.TargetFallingBlockEntity target) {
                target.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
                removed++;
            }
        }

        if (removed > 0) {
            LootSparkle.LOGGER.info("Cleaned up {} orphaned sparkle target entities on world load", removed);
        }
    }
}
