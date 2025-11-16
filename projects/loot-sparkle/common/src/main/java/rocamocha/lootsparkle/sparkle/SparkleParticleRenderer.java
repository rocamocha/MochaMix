package rocamocha.lootsparkle.sparkle;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import rocamocha.lootsparkle.core.LootSparkle;
import rocamocha.lootsparkle.core.LootSparkleConfig;
import rocamocha.lootsparkle.item.TreasureCompassItem;

/**
 * Handles client-side particle rendering for sparkles
 *
 * Manages:
 * - Particle effect spawning
 * - Particle animation and positioning
 * - Performance optimization for multiple sparkles
 */
public class SparkleParticleRenderer {
    // Compass location enum
    private enum CompassLocation {
        NONE,
        MAIN_HAND,
        OFF_HAND,
        HOTBAR
    }

    // Particle spawn rate (every N ticks) - reduced for shorter particle lifetimes
    // Particle spawn rate (every N ticks) - reduced for shorter particle lifetimes
    private static final int PARTICLE_SPAWN_RATE = 5;

    // Number of particles per sparkle - reduced since we spawn more frequently
    private static final int PARTICLES_PER_SPARKLE = 2;

    // Compass-guided particle settings
    private static final double COMPASS_FAIRY_DISTANCE = 4; // Distance in front of player (increased)
    private static final double COMPASS_FAIRY_HEIGHT = 1.7; // Height above player (increased)
    private static final double COMPASS_FAIRY_FIGURE_EIGHT_SIZE = 0.43; // Size of figure-eight pattern

    // Directional particle settings
    private static final double DIRECTIONAL_PARTICLE_SPACING = 0.3; // Distance between particles
    private static final double DIRECTIONAL_DRIFT_AMPLITUDE_BASE = 0.4; // Base drift amplitude (doubled)
    private static final double DIRECTIONAL_DRIFT_AMPLITUDE_MAX = 3; // Maximum drift amplitude (doubled)
    private static final double DIRECTIONAL_DRIFT_DISTANCE_SCALE = 0.4; // How much distance affects drift
    private static final double DIRECTIONAL_DRIFT_SPEED = 0.15; // Animation speed for drift

    // Player-surrounding particle settings
    private static final int PLAYER_SURROUND_PARTICLES_PER_LAYER = 8; // Particles per layer
    private static final int PLAYER_SURROUND_LAYERS = 3; // Number of concentric layers
    private static final double[] PLAYER_SURROUND_RADII = {2.5, 3.6, 4.4}; // Radii for each layer
    private static final double PLAYER_SURROUND_HEIGHT_VARIATION = 2.0; // Height variation
    private static final double[] PLAYER_SURROUND_SPEEDS = {0.3, 0.2, 0.5}; // Rotation speeds for each layer
    private static final double PLAYER_SURROUND_SPARKLE_PROXIMITY = 12.0; // Distance to show colored particles

    private static int tickCounter = 0;
    private static int compassTickCounter = 0;
    private static double fairyAnimationTime = 0.0; // For figure-eight animation

    public static void initialize() {

        // Register world render event to spawn particles
        WorldRenderEvents.END.register(context -> {
            tickCounter++;
            compassTickCounter++;

            // Spawn sparkle particles
            if (tickCounter >= PARTICLE_SPAWN_RATE) {
                tickCounter = 0;
                spawnSparkleParticles(context.world());
            }

            // Spawn compass-guided particles
            if (compassTickCounter >= getCompassParticleRate(context.world())) {
                compassTickCounter = 0;
                spawnCompassParticles(context.world());
            }

            // Render timer displays above sparkles
            renderSparkleTimers(context);
        });

        // Register HUD rendering for 2D timer overlays
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            renderTimerOverlays(drawContext);
        });
    }

    /**
     * Spawns particles for all active sparkles
     */
    private static void spawnSparkleParticles(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        UUID playerId = client.player.getUuid();
        ParticleManager particleManager = client.particleManager;

        // Always spawn particles for hostile sparkles (visible to everyone)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            spawnParticlesForSparkle(world, particleManager, hostileSparkle);
        }

        // Spawn particles around hostile mobs
        spawnMobParticles(world, particleManager);

        // Only spawn particles for player sparkles if player has compass
        if (isPlayerHoldingCompass(client)) {
            List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
            for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
                spawnParticlesForSparkle(world, particleManager, sparkle);
            }
        }
    }

    /**
     * Spawns particles for a single sparkle
     */
    private static void spawnParticlesForSparkle(ClientWorld world, ParticleManager particleManager, ClientSparkleManager.ClientSparkle sparkle) {
        BlockPos pos = sparkle.getPosition();
        Vec3d center = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);

        // Get color based on tier
        Vector3f particleColor = getTierColor(sparkle.getTierLevel());

        // Spawn multiple particles around the sparkle position
        for (int i = 0; i < PARTICLES_PER_SPARKLE; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetY = world.random.nextDouble() * 0.2;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;

            Vec3d particlePos = center.add(offsetX, offsetY, offsetZ);

            // Underwater tiers get bubbles plus tinted dust
            if (isUnderwaterTier(sparkle.getTierLevel())) {
                // Visual separation:
                // - Lower ONLY the bubbles so they appear in water and don't pop instantly at the surface
                // - Keep sparkle dust at original height so core sparkle remains readable above bubbles
                Vec3d bubblePos = particlePos.add(0.0, -0.5, 0.0);
                particleManager.addParticle(
                    ParticleTypes.BUBBLE,
                    bubblePos.x, bubblePos.y, bubblePos.z,
                    0.0, 0.05, 0.0
                );

                // Keep sparkle dust at original height; adjust color slightly per-tier
                Vector3f dustColor;
                if (sparkle.getTierLevel() == 9) { // DRIFTWOOD: mix in a lighter, less saturated tone
                    // Base driftwood tone and a lighter/beige variant to avoid muddy look
                    Vector3f base = new Vector3f(0.78f, 0.65f, 0.46f);
                    Vector3f light = new Vector3f(0.88f, 0.80f, 0.65f);
                    float t = 0.35f + (float)world.random.nextDouble() * 0.25f; // 0.35 - 0.60
                    dustColor = new Vector3f(
                        base.x + (light.x - base.x) * t,
                        base.y + (light.y - base.y) * t,
                        base.z + (light.z - base.z) * t
                    );
                } else {
                    dustColor = new Vector3f(particleColor);
                    // Slight cool tint for non-driftwood underwater tiers to feel aquatic
                    dustColor.mul(0.9f, 0.95f, 0.95f);
                }

                DustParticleEffect dustEffect = new DustParticleEffect(dustColor, 0.9f);
                particleManager.addParticle(
                    dustEffect,
                    particlePos.x, particlePos.y, particlePos.z,
                    0.0, 0.01, 0.0
                );
            } else {
                // Spawn colored dust particles based on tier
                DustParticleEffect dustEffect = new DustParticleEffect(particleColor, 1.0f);
                particleManager.addParticle(
                    dustEffect,
                    particlePos.x, particlePos.y, particlePos.z,
                    0.0, 0.01, 0.0 // Slight upward motion
                );
            }
        }
    }

    /**
     * Checks if the player has a treasure compass and returns where it's located
     */
    private static CompassLocation getCompassLocation(MinecraftClient client) {
        if (client.player == null) return CompassLocation.NONE;

        // Check main hand
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() == LootSparkle.TREASURE_COMPASS) return CompassLocation.MAIN_HAND;

        // Check off hand
        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() == LootSparkle.TREASURE_COMPASS) return CompassLocation.OFF_HAND;

        // Check hotbar slots (0-8)
        for (int slot = 0; slot < 9; slot++) {
            ItemStack hotbarStack = client.player.getInventory().getStack(slot);
            if (hotbarStack.getItem() == LootSparkle.TREASURE_COMPASS) return CompassLocation.HOTBAR;
        }

        return CompassLocation.NONE;
    }

    /**
     * Checks if the player has a treasure compass (in hand or hotbar)
     */
    private static boolean isPlayerHoldingCompass(MinecraftClient client) {
        return getCompassLocation(client) != CompassLocation.NONE;
    }

    /**
     * Checks if the player is holding the compass in their hand (main or off)
     */
    private static boolean isPlayerHoldingCompassInHand(MinecraftClient client) {
        CompassLocation location = getCompassLocation(client);
        return location == CompassLocation.MAIN_HAND || location == CompassLocation.OFF_HAND;
    }

    /**
     * Gets the treasure compass item stack from the player's inventory
     */
    private static ItemStack getTreasureCompassStack(MinecraftClient client) {
        if (client.player == null) return ItemStack.EMPTY;

        // Check main hand
        ItemStack mainHand = client.player.getMainHandStack();
        if (mainHand.getItem() == LootSparkle.TREASURE_COMPASS) return mainHand;

        // Check off hand
        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() == LootSparkle.TREASURE_COMPASS) return offHand;

        // Check hotbar slots (0-8)
        for (int slot = 0; slot < 9; slot++) {
            ItemStack hotbarStack = client.player.getInventory().getStack(slot);
            if (hotbarStack.getItem() == LootSparkle.TREASURE_COMPASS) return hotbarStack;
        }

        return ItemStack.EMPTY;
    }

    /**
     * Checks if the player is wearing a helmet with Soul Sight enchantment
     */
    private static boolean hasSoulSightHelmet(MinecraftClient client) {
        return getSoulSightLevel(client) > 0;
    }

    /**
     * Gets the Soul Sight enchantment level from the player's helmet
     * Returns 0 if no Soul Sight enchantment is present
     */
    private static int getSoulSightLevel(MinecraftClient client) {
        if (client.player == null) return 0;

        ItemStack helmet = client.player.getInventory().getArmorStack(3); // Helmet slot
        if (helmet.isEmpty()) return 0;

        // Get the enchantments component
        var enchantments = helmet.getEnchantments();
        
        // Find the Soul Sight enchantment and get its level
        for (var entry : enchantments.getEnchantments()) {
            if (entry.getIdAsString().equals("loot-sparkle:soul_sight")) {
                return enchantments.getLevel(entry);
            }
        }
        
        return 0;
    }

    /**
     * Finds the nearest sparkle to the player (no Soul Sight filtering)
     * Used for fairy particle - shows nearest sparkle regardless of detection ability
     */
    private static ClientSparkleManager.ClientSparkle findNearestSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        Vec3d playerPos = client.player.getPos();

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Check player sparkles
        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        // Check hostile sparkles (always visible)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hostileSparkle;
            }
        }

        return nearest;
    }

    // Normal sparkles only (exclude underwater tiers)
    private static ClientSparkleManager.ClientSparkle findNearestNormalSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        Vec3d playerPos = client.player.getPos();

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            if (isUnderwaterTier(sparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            if (isUnderwaterTier(hostileSparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hostileSparkle;
            }
        }

        return nearest;
    }

    // Underwater sparkles only
    private static ClientSparkleManager.ClientSparkle findNearestUnderwaterSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        Vec3d playerPos = client.player.getPos();

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            if (!isUnderwaterTier(sparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        return nearest;
    }

    // Normal detectable (Soul Sight), excluding underwater tiers
    private static ClientSparkleManager.ClientSparkle findNearestNormalDetectableSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        Vec3d playerPos = client.player.getPos();

        int soulSightLevel = getSoulSightLevel(client);
        int maxDetectableTier = soulSightLevel > 0 ? soulSightLevel - 1 : -1;

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            if (isUnderwaterTier(sparkle.getTierLevel())) continue;
            if (soulSightLevel == 0 || sparkle.getTierLevel() > maxDetectableTier) continue;
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            if (isUnderwaterTier(hostileSparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hostileSparkle;
            }
        }

        return nearest;
    }

    /**
     * Finds the nearest sparkle to the player that can be detected by the current Soul Sight level
     * Used for directional particles - requires Soul Sight
     */
    private static ClientSparkleManager.ClientSparkle findNearestDetectableSparkle(ClientWorld world, MinecraftClient client) {
        if (client.player == null) return null;

        UUID playerId = client.player.getUuid();
        Vec3d playerPos = client.player.getPos();

        int soulSightLevel = getSoulSightLevel(client);
        // Determine max tier that can be detected: Soul Sight level 1 = Common (tier 0), level 2 = Uncommon (tier 1), etc.
        int maxDetectableTier = soulSightLevel > 0 ? soulSightLevel - 1 : -1;

        ClientSparkleManager.ClientSparkle nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        // Check player sparkles (filtered by Soul Sight)
        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            // Filter by Soul Sight level - can only detect sparkles up to maxDetectableTier
            if (soulSightLevel == 0 || sparkle.getTierLevel() > maxDetectableTier) {
                continue; // Skip this sparkle if we can't detect it
            }

            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = sparkle;
            }
        }

        // Check hostile sparkles (always detectable since they're visible to everyone)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            double distance = playerPos.distanceTo(sparklePos);

            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = hostileSparkle;
            }
        }

        return nearest;
    }

    /**
     * Calculates the compass particle spawn rate based on distance to nearest sparkle
     */
    private static int getCompassParticleRate(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!isPlayerHoldingCompass(client)) return Integer.MAX_VALUE; // Don't spawn if no compass

        return 10; // Fixed rate for ring particles
    }

    /**
     * Spawns a single compass fairy particle in front and to the left of the player
     */
    private static void spawnCompassParticles(ClientWorld world) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !isPlayerHoldingCompass(client)) return;

        ItemStack treasureCompass = getTreasureCompassStack(client);
        if (treasureCompass.isEmpty()) return;

        if (treasureCompass.getDamage() >= treasureCompass.getMaxDamage()) return;

        boolean hasFairyDust = TreasureCompassItem.hasFairyDust(treasureCompass);
        int diversLevel = getDiversCrystalLevel(client);
        int eldertideLevel = getEldertideLevel(client);

        ParticleManager particleManager = client.particleManager;
        Vec3d playerPos = client.player.getPos();

        // Update fairy animation time for figure-eight pattern
        fairyAnimationTime += 0.1;

        // Base vectors
        Vec3d playerFacing = client.player.getRotationVector();
        Vec3d frontOffset = new Vec3d(playerFacing.x, 0, playerFacing.z).normalize().multiply(COMPASS_FAIRY_DISTANCE);
        Vec3d leftOffset = frontOffset.crossProduct(new Vec3d(0, -1, 0)).normalize().multiply(0.7);
        Vec3d rightOffset = frontOffset.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(0.7);
        double figureEightX = Math.sin(fairyAnimationTime) * COMPASS_FAIRY_FIGURE_EIGHT_SIZE;
        double figureEightY = Math.sin(fairyAnimationTime * 2) * COMPASS_FAIRY_FIGURE_EIGHT_SIZE * 0.5;

        // 1) Normal guidance (Fairy Dust) - exclude underwater tiers
        if (hasFairyDust) {
            ClientSparkleManager.ClientSparkle nearestNormal = findNearestNormalSparkle(world, client);
            if (nearestNormal != null) {
                Vector3f fairyColor = getFairyColorNormal(client, nearestNormal);
                Vec3d fairyPos = playerPos.add(frontOffset).add(leftOffset)
                    .add(figureEightX, figureEightY + COMPASS_FAIRY_HEIGHT, 0);

                DustParticleEffect fairyParticle = new DustParticleEffect(fairyColor, 1.0f);
                particleManager.addParticle(
                    fairyParticle,
                    fairyPos.x, fairyPos.y, fairyPos.z,
                    0.0, 0.01, 0.0
                );

                if (isPlayerHoldingCompassInHand(client) && hasSoulSightHelmet(client)) {
                    ClientSparkleManager.ClientSparkle detectable = findNearestNormalDetectableSparkle(world, client);
                    if (detectable != null) {
                        Vector3f color = getTierColor(detectable.getTierLevel());
                        spawnDirectionalParticles(world, particleManager, detectable, fairyPos, color);
                        spawnPlayerSurroundParticles(world, particleManager, playerPos, client);
                    }
                }
            }
        }

        // 2) Underwater guidance (Eldertide Resonance + Diver's Crystal) - underwater tiers only
        if (eldertideLevel > 0 && diversLevel > 0) {
            ClientSparkleManager.ClientSparkle nearestUnderwater = findNearestUnderwaterSparkle(world, client);
            if (nearestUnderwater != null) {
                Vector3f uwFairyColor = getFairyColorUnderwater(client, nearestUnderwater);
                Vec3d uwFairyPos = playerPos.add(frontOffset).add(rightOffset)
                    .add(figureEightX, figureEightY + COMPASS_FAIRY_HEIGHT, 0);

                DustParticleEffect uwFairyParticle = new DustParticleEffect(uwFairyColor, 1.0f);
                particleManager.addParticle(
                    uwFairyParticle,
                    uwFairyPos.x, uwFairyPos.y, uwFairyPos.z,
                    0.0, 0.01, 0.0
                );

                if (isPlayerHoldingCompassInHand(client)) {
                    UUID playerId = client.player.getUuid();
                    List<ClientSparkleManager.ClientSparkle> all = ClientSparkleManager.getPlayerSparkles(playerId);
                    if (eldertideLevel >= 2) {
                        for (ClientSparkleManager.ClientSparkle s : all) {
                            if (!isUnderwaterTier(s.getTierLevel())) continue;
                            Vec3d pos = Vec3d.ofCenter(s.getPosition());
                            if (pos.distanceTo(playerPos) <= 64.0) {
                                Vector3f c = getTierColor(s.getTierLevel());
                                spawnDirectionalParticles(world, particleManager, s, uwFairyPos, c);
                            }
                        }
                        spawnPlayerSurroundParticles(world, particleManager, playerPos, client);
                    } else {
                        Vector3f c = getTierColor(nearestUnderwater.getTierLevel());
                        spawnDirectionalParticles(world, particleManager, nearestUnderwater, uwFairyPos, c);
                        spawnPlayerSurroundParticles(world, particleManager, playerPos, client);
                    }
                }
            }
        }
    }

    /**
     * Spawns directional particles along the path from sparkle to fairy with magical drift
     */
    private static void spawnDirectionalParticles(ClientWorld world, ParticleManager particleManager,
                                                 ClientSparkleManager.ClientSparkle sparkle, Vec3d fairyPos, Vector3f color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Always use pathfinding to prevent particles from clipping through walls
        spawnPathfindingParticles(world, particleManager, sparkle, fairyPos, color);
    }

    /**
     * Spawns particles along a direct line from sparkle to fairy (for outdoor sparkles)
     */
    private static void spawnDirectParticles(ClientWorld world, ParticleManager particleManager,
                                           ClientSparkleManager.ClientSparkle sparkle, Vec3d fairyPos, Vector3f color) {
        Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
        Vec3d playerPos = MinecraftClient.getInstance().player.getPos();

        // Calculate direction vector from sparkle to fairy
        Vec3d direction = fairyPos.subtract(sparklePos);
        double distance = direction.length();

        // Calculate distance from player to sparkle for drift scaling
        double playerToSparkleDistance = playerPos.distanceTo(sparklePos);

        // Scale drift amplitude based on distance from player to sparkle
        // Further away = more drift for adventurous exploration
        double driftAmplitude = DIRECTIONAL_DRIFT_AMPLITUDE_BASE +
            Math.min(DIRECTIONAL_DRIFT_AMPLITUDE_MAX - DIRECTIONAL_DRIFT_AMPLITUDE_BASE,
                    playerToSparkleDistance * DIRECTIONAL_DRIFT_DISTANCE_SCALE);

        // Don't spawn particles if too close (would be cluttered)
        if (distance < 3.0) return;

        // Calculate number of particles based on distance and spacing
        int numParticles = Math.max(5, (int)(distance / DIRECTIONAL_PARTICLE_SPACING));

        // Create particles along the path with magical drift
        for (int i = 0; i < numParticles; i++) {
            double t = (double) i / (numParticles - 1); // 0 to 1 along the path
            Vec3d basePos = sparklePos.add(direction.multiply(t));

            // Add magical drift animation for each particle
            // Each particle has its own animation phase based on its position and time
            double particlePhase = fairyAnimationTime * DIRECTIONAL_DRIFT_SPEED + (i * 0.5); // Unique phase per particle

            // Create unique random characteristics for each particle
            long particleSeed = (long)(sparkle.getSparkleId().hashCode() + i * 31); // Unique seed per particle
            double randomOffset1 = (new java.util.Random(particleSeed).nextDouble() - 0.5) * 2.0; // -1 to 1
            double randomOffset2 = (new java.util.Random(particleSeed + 1).nextDouble() - 0.5) * 2.0; // -1 to 1
            double randomRotation = new java.util.Random(particleSeed + 2).nextDouble() * Math.PI * 2; // 0 to 2π
            double randomSpeed = 0.8 + new java.util.Random(particleSeed + 3).nextDouble() * 0.4; // 0.8 to 1.2

            // Apply random rotation to the drift pattern for uniqueness
            double rotatedPhase1 = particlePhase * randomSpeed + randomOffset1;
            double rotatedPhase2 = (particlePhase * 1.3 + Math.PI * 0.5) * randomSpeed + randomOffset2;
            double rotatedPhase3 = (particlePhase * 0.8 + randomRotation) * randomSpeed;

            // Create whimsical drift using multiple sine waves, scaled by distance
            double driftX = Math.sin(rotatedPhase1) * driftAmplitude;
            double driftY = Math.sin(rotatedPhase2) * driftAmplitude * 0.6;
            double driftZ = Math.cos(rotatedPhase3) * driftAmplitude;

            // Apply drift to base position
            Vec3d particlePos = basePos.add(driftX, driftY, driftZ);

            // Add some additional randomness for extra magic
            double randomOffsetX = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetY = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetZ = (world.random.nextDouble() - 0.5) * 0.1;
            particlePos = particlePos.add(randomOffsetX, randomOffsetY, randomOffsetZ);

            // Create directional particle with magical color variation
            Vector3f particleColor = new Vector3f(color);
            // Add slight color variation based on particle position
            float colorVariation = 0.1f + (float)(Math.sin(particlePhase * 2) * 0.05f);
            particleColor.add(colorVariation, colorVariation * 0.5f, colorVariation * 0.8f);
            // Clamp color components to valid range [0.0, 1.0]
            particleColor.set(Math.max(0.0f, Math.min(1.0f, particleColor.x)),
                             Math.max(0.0f, Math.min(1.0f, particleColor.y)),
                             Math.max(0.0f, Math.min(1.0f, particleColor.z)));

            DustParticleEffect directionalParticle = new DustParticleEffect(particleColor, 0.6f);

            particleManager.addParticle(
                directionalParticle,
                particlePos.x, particlePos.y, particlePos.z,
                0.0, 0.002, 0.0 // Very gentle upward motion
            );
        }
    }

    /**
     * Spawns particles along a pathfinding-generated path from sparkle to fairy (for indoor sparkles)
     */
    private static void spawnPathfindingParticles(ClientWorld world, ParticleManager particleManager,
                                                ClientSparkleManager.ClientSparkle sparkle, Vec3d fairyPos, Vector3f color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        BlockPos sparkleBlockPos = sparkle.getPosition();
        BlockPos fairyBlockPos = new BlockPos((int)fairyPos.x, (int)fairyPos.y, (int)fairyPos.z);

        // Use the same pathfinding logic as sparkle spawning
        List<BlockPos> path = findPath(world, sparkleBlockPos, fairyBlockPos, 36);

        if (path.isEmpty()) {
            // Fallback to direct particles if no path found
            spawnDirectParticles(world, particleManager, sparkle, fairyPos, color);
            return;
        }

        // Spawn particles along the path
        for (int i = 0; i < path.size(); i++) {
            BlockPos pathPos = path.get(i);

            // Find valid positions for particle spawning (original + up to 2 blocks away non-solid neighbors)
            List<BlockPos> validPositions = findValidParticlePositions(world, pathPos);

            // Randomly choose one of the valid positions
            BlockPos chosenPos = validPositions.get(world.random.nextInt(validPositions.size()));
            Vec3d particlePos = new Vec3d(chosenPos.getX() + 0.5, chosenPos.getY() + 0.5, chosenPos.getZ() + 0.5);

            // Add some magical drift animation
            double particlePhase = fairyAnimationTime * DIRECTIONAL_DRIFT_SPEED + (i * 0.3);
            double driftX = Math.sin(particlePhase) * 0.3;
            double driftY = Math.sin(particlePhase * 1.3) * 0.2;
            double driftZ = Math.cos(particlePhase * 0.8) * 0.3;

            particlePos = particlePos.add(driftX, driftY, driftZ);

            // Add randomness
            double randomOffsetX = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetY = (world.random.nextDouble() - 0.5) * 0.1;
            double randomOffsetZ = (world.random.nextDouble() - 0.5) * 0.1;
            particlePos = particlePos.add(randomOffsetX, randomOffsetY, randomOffsetZ);

            // Create particle with color variation
            Vector3f particleColor = new Vector3f(color);
            float colorVariation = 0.1f + (float)(Math.sin(particlePhase * 2) * 0.05f);
            particleColor.add(colorVariation, colorVariation * 0.5f, colorVariation * 0.8f);
            particleColor.set(Math.max(0.0f, Math.min(1.0f, particleColor.x)),
                             Math.max(0.0f, Math.min(1.0f, particleColor.y)),
                             Math.max(0.0f, Math.min(1.0f, particleColor.z)));

            DustParticleEffect pathParticle = new DustParticleEffect(particleColor, 0.6f);

            particleManager.addParticle(
                pathParticle,
                particlePos.x, particlePos.y, particlePos.z,
                0.0, 0.002, 0.0
            );
        }
    }

    /**
     * Simplified pathfinding for particle placement (client-side version of server pathfinding)
     */
    private static List<BlockPos> findPath(ClientWorld world, BlockPos start, BlockPos end, int maxRadius) {
        List<BlockPos> path = new java.util.ArrayList<>();
        if (start.getSquaredDistance(end) > maxRadius * maxRadius) {
            return path; // Too far
        }

        // Simple BFS for pathfinding
        Set<BlockPos> visited = new java.util.HashSet<>();
        Map<BlockPos, BlockPos> cameFrom = new java.util.HashMap<>();
        Queue<BlockPos> queue = new java.util.LinkedList<>();

        queue.add(start);
        visited.add(start);
        cameFrom.put(start, null);

        int[][] directions = {
            {0, 0, 1}, {0, 0, -1}, {1, 0, 0}, {-1, 0, 0},  // Horizontal movement
            {0, 1, 0}, {0, -1, 0}  // Vertical movement for caves
        };

        boolean found = false;
        while (!queue.isEmpty() && !found) {
            BlockPos current = queue.poll();
            if (current.equals(end)) {
                found = true;
                break;
            }

            for (int[] dir : directions) {
                BlockPos next = current.add(dir[0], dir[1], dir[2]);
                if (!visited.contains(next) && next.getSquaredDistance(start) <= maxRadius * maxRadius) {
                    if (isPassableAndGrounded(world, next)) {
                        visited.add(next);
                        queue.add(next);
                        cameFrom.put(next, current);
                    }
                }
            }
        }

        // Reconstruct path if found
        if (found) {
            BlockPos current = end;
            while (current != null) {
                path.add(0, current);
                current = cameFrom.get(current);
            }
        }

        return path;
    }

    /**
     * Client-side version of the grounded pathfinding check
     */
    private static boolean isPassableAndGrounded(ClientWorld world, BlockPos pos) {
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
     * Finds valid positions for particle spawning around a path position
     * Includes the original position and up to 2 blocks away non-solid neighbors
     */
    private static List<BlockPos> findValidParticlePositions(ClientWorld world, BlockPos centerPos) {
        List<BlockPos> validPositions = new java.util.ArrayList<>();

        // Always include the original position if it's valid
        if (isPassableAndGrounded(world, centerPos)) {
            validPositions.add(centerPos);
        }

        // Check all positions within 2 blocks in each direction (but not including the center)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    // Skip the center position (already added above)
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    // Check if within 2 blocks distance (Manhattan distance)
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 2) {
                        BlockPos checkPos = centerPos.add(dx, dy, dz);
                        if (isPassableAndGrounded(world, checkPos)) {
                            validPositions.add(checkPos);
                        }
                    }
                }
            }
        }

        // If no valid positions found (shouldn't happen), return the center
        if (validPositions.isEmpty()) {
            validPositions.add(centerPos);
        }

        return validPositions;
    }

    /**
     * Spawns whimsical particles that circle around the player in multiple layers
     */
    private static void spawnPlayerSurroundParticles(ClientWorld world, ParticleManager particleManager, Vec3d playerPos, MinecraftClient client) {
        // Check if there's a sparkle within proximity distance
        boolean hasNearbySparkle = hasSparkleWithinDistance(client, playerPos, PLAYER_SURROUND_SPARKLE_PROXIMITY);

        // Create multiple layers of particles circling around the player
        for (int layer = 0; layer < PLAYER_SURROUND_LAYERS; layer++) {
            double radius = PLAYER_SURROUND_RADII[layer];
            double speed = PLAYER_SURROUND_SPEEDS[layer];

            // Create particles for this layer
            for (int i = 0; i < PLAYER_SURROUND_PARTICLES_PER_LAYER; i++) {
                // Calculate angle for this particle in the circle
                double angle = (fairyAnimationTime * speed) + (i * (Math.PI * 2 / PLAYER_SURROUND_PARTICLES_PER_LAYER));

                // Add some randomness to make it less uniform
                long seedBase = layer * 100 + i * 7; // Unique seed per layer and particle
                double randomAngleOffset = (new java.util.Random(seedBase).nextDouble() - 0.5) * 0.5;
                double randomRadiusOffset = (new java.util.Random(seedBase + 1).nextDouble() - 0.5) * 0.3;

                // Dynamic height variation that changes over time
                long heightSeed = (long)(fairyAnimationTime * 1000) + layer * 1000 + i * 100;
                double randomHeightOffset = (new java.util.Random(heightSeed).nextDouble() - 0.5) * PLAYER_SURROUND_HEIGHT_VARIATION;

                double finalAngle = angle + randomAngleOffset;
                double finalRadius = radius + randomRadiusOffset;
                double height = playerPos.y + 1.0 + randomHeightOffset; // Eye level + variation

                // Calculate position on the circle
                double x = playerPos.x + Math.cos(finalAngle) * finalRadius;
                double z = playerPos.z + Math.sin(finalAngle) * finalRadius;

                Vector3f particleColor;
                if (hasNearbySparkle) {
                    // Generate random color for each particle (changes over time)
                    long timeBasedSeed = (long)(fairyAnimationTime * 1000) + layer * 1000 + i * 100;
                    java.util.Random colorRandom = new java.util.Random(timeBasedSeed);
                    float red = colorRandom.nextFloat();
                    float green = colorRandom.nextFloat();
                    float blue = colorRandom.nextFloat();

                    // Ensure colors are bright enough (avoid too dark)
                    red = Math.max(red, 0.6f);
                    green = Math.max(green, 0.2f);
                    blue = Math.max(blue, 0.2f);

                    // Add layer-based color tinting for visual distinction
                    switch (layer) {
                        case 0: // Inner layer - slightly more saturated
                            red *= 1.1f;
                            green *= 1.1f;
                            blue *= 1.1f;
                            break;
                        case 1: // Middle layer - normal
                            break;
                        case 2: // Outer layer - slightly more pastel
                            red = (red + 0.5f) / 1.5f;
                            green = (green + 0.5f) / 1.5f;
                            blue = (blue + 0.5f) / 1.5f;
                            break;
                    }
                    particleColor = new Vector3f(red, green, blue);
                } else {
                    // No nearby sparkle - use gray particles
                    particleColor = new Vector3f(0.5f, 0.5f, 0.5f); // Medium gray
                }

                // Create the particle
                DustParticleEffect surroundParticle = new DustParticleEffect(particleColor, 0.8f);
                particleManager.addParticle(
                    surroundParticle,
                    x, height, z,
                    0.0, 0.005, 0.0 // Gentle upward drift
                );
            }
        }
    }

    /**
     * Checks if there's a sparkle within the specified distance from the player
     */
    private static boolean hasSparkleWithinDistance(MinecraftClient client, Vec3d playerPos, double distance) {
        if (client.player == null) return false;

        UUID playerId = client.player.getUuid();

        // Check player sparkles
        List<ClientSparkleManager.ClientSparkle> playerSparkles = ClientSparkleManager.getPlayerSparkles(playerId);
        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            double sparkleDistance = playerPos.distanceTo(sparklePos);

            if (sparkleDistance <= distance) {
                return true;
            }
        }

        // Check hostile sparkles (always visible)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            double sparkleDistance = playerPos.distanceTo(sparklePos);

            if (sparkleDistance <= distance) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the color for a sparkle based on its tier level
     */
    private static Vector3f getTierColor(int tierLevel) {
        switch (tierLevel) {
            case 0: // COMMON - White
                return new Vector3f(1.0f, 1.0f, 1.0f);
            case 1: // UNCOMMON - Green
                return new Vector3f(0.0f, 1.0f, 0.0f);
            case 2: // RARE - Blue
                return new Vector3f(0.0f, 0.5f, 1.0f);
            case 3: // EPIC - Purple
                return new Vector3f(0.8f, 0.0f, 1.0f);
            case 4: // LEGENDARY - Gold
                return new Vector3f(1.0f, 0.8f, 0.0f);
            case 5: // DIVINE - Rainbow cycling
                return getRainbowColor();
            case 6: // CURSED - Red
                return new Vector3f(1.0f, 0.0f, 0.0f);
            case 7: // BLIGHTED - Red and Purple
                return getBlightedColor();
            case 8: // DOOMED - Red and Black
                return getDoomedColor();
            case 9: // DRIFTWOOD - Light, desaturated wood tone
                return new Vector3f(0.78f, 0.58f, 0.27f);
            case 10: // KELP - Sea green
                return new Vector3f(0.2f, 0.8f, 0.6f);
            case 11: // CORAL - Coral pink
                return new Vector3f(1.0f, 0.4f, 0.5f);
            case 12: // CAVERN - Deep teal
                return new Vector3f(0.0f, 0.6f, 0.6f);
            case 13: // SEABED - Sandy gold
                return new Vector3f(0.9f, 0.8f, 0.4f);
            default:
                return new Vector3f(1.0f, 1.0f, 1.0f); // White fallback
        }
    }

    /**
     * Gets the fairy color based on the highest tier sparkle within the player's viewing angle
     * No Soul Sight filtering - fairy shows colors for all sparkles regardless of detection ability
     */
    private static Vector3f getFairyColor(MinecraftClient client, ClientSparkleManager.ClientSparkle nearestSparkle) {
        if (client.player == null) return getTierColor(nearestSparkle.getTierLevel());

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> sparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        Vec3d playerPos = client.player.getPos();
        Vec3d playerLookVec = client.player.getRotationVector();

        int highestTier = -1;

        // Check player sparkles
        for (ClientSparkleManager.ClientSparkle sparkle : sparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();

            // Check if sparkle is in front of player (dot product > 0)
            double dotProduct = playerLookVec.dotProduct(toSparkle);
            if (dotProduct > 0) {
                // Track the highest tier we're looking at
                if (sparkle.getTierLevel() > highestTier) {
                    highestTier = sparkle.getTierLevel();
                }
            }
        }

        // Check hostile sparkles (always visible)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();

            // Check if sparkle is in front of player (dot product > 0)
            double dotProduct = playerLookVec.dotProduct(toSparkle);
            if (dotProduct > 0) {
                // Track the highest tier we're looking at
                if (hostileSparkle.getTierLevel() > highestTier) {
                    highestTier = hostileSparkle.getTierLevel();
                }
            }
        }

        // Use the highest tier found, or fall back to the nearest sparkle's tier
        if (highestTier >= 0) {
            return getTierColor(highestTier);
        }
        return getTierColor(nearestSparkle.getTierLevel());
    }

    // Fairy color for normal guidance: ignore underwater tiers when scanning
    private static Vector3f getFairyColorNormal(MinecraftClient client, ClientSparkleManager.ClientSparkle nearestSparkle) {
        if (client.player == null) return getTierColor(nearestSparkle.getTierLevel());

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> sparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        Vec3d playerPos = client.player.getPos();
        Vec3d playerLookVec = client.player.getRotationVector();

        int highestTier = -1;

        for (ClientSparkleManager.ClientSparkle sparkle : sparkles) {
            if (isUnderwaterTier(sparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();
            double dotProduct = playerLookVec.dotProduct(toSparkle);
            if (dotProduct > 0) {
                if (sparkle.getTierLevel() > highestTier) {
                    highestTier = sparkle.getTierLevel();
                }
            }
        }

        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();
        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            if (isUnderwaterTier(hostileSparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(hostileSparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();
            double dotProduct = playerLookVec.dotProduct(toSparkle);
            if (dotProduct > 0) {
                if (hostileSparkle.getTierLevel() > highestTier) {
                    highestTier = hostileSparkle.getTierLevel();
                }
            }
        }

        if (highestTier >= 0) return getTierColor(highestTier);
        return getTierColor(nearestSparkle.getTierLevel());
    }

    // Fairy color for underwater guidance: only consider underwater tiers
    private static Vector3f getFairyColorUnderwater(MinecraftClient client, ClientSparkleManager.ClientSparkle nearestSparkle) {
        if (client.player == null) return getTierColor(nearestSparkle.getTierLevel());

        UUID playerId = client.player.getUuid();
        List<ClientSparkleManager.ClientSparkle> sparkles = ClientSparkleManager.getPlayerSparkles(playerId);

        Vec3d playerPos = client.player.getPos();
        Vec3d playerLookVec = client.player.getRotationVector();

        int highestTier = -1;

        for (ClientSparkleManager.ClientSparkle sparkle : sparkles) {
            if (!isUnderwaterTier(sparkle.getTierLevel())) continue;
            Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition());
            Vec3d toSparkle = sparklePos.subtract(playerPos).normalize();
            double dotProduct = playerLookVec.dotProduct(toSparkle);
            if (dotProduct > 0) {
                if (sparkle.getTierLevel() > highestTier) {
                    highestTier = sparkle.getTierLevel();
                }
            }
        }

        if (highestTier >= 0) return getTierColor(highestTier);
        return getTierColor(nearestSparkle.getTierLevel());
    }

    private static boolean isUnderwaterTier(int tierLevel) {
        return tierLevel >= 9; // Underwater tiers: 9-13
    }

    // Detect Diver's Crystal on treasure compass
    private static int getDiversCrystalLevel(MinecraftClient client) {
        ItemStack compass = getTreasureCompassStack(client);
        if (compass.isEmpty()) return 0;
        var enchantments = compass.getEnchantments();
        for (var entry : enchantments.getEnchantments()) {
            if (entry.getIdAsString().equals("loot-sparkle:divers_crystal")) {
                return enchantments.getLevel(entry);
            }
        }
        return 0;
    }

    // Detect Eldertide Resonance on held items (trident)
    private static int getEldertideLevel(MinecraftClient client) {
        if (client.player == null) return 0;
        int level = 0;
        ItemStack main = client.player.getMainHandStack();
        if (!main.isEmpty()) {
            var ench = main.getEnchantments();
            for (var entry : ench.getEnchantments()) {
                if (entry.getIdAsString().equals("loot-sparkle:eldertide_resonance")) {
                    level = Math.max(level, ench.getLevel(entry));
                }
            }
        }
        ItemStack off = client.player.getOffHandStack();
        if (!off.isEmpty()) {
            var ench = off.getEnchantments();
            for (var entry : ench.getEnchantments()) {
                if (entry.getIdAsString().equals("loot-sparkle:eldertide_resonance")) {
                    level = Math.max(level, ench.getLevel(entry));
                }
            }
        }
        return level;
    }

    /**
     * Gets a rainbow color that cycles over time for divine tier sparkles
     */
    private static Vector3f getRainbowColor() {
        // Use system time for smooth color cycling
        long time = System.currentTimeMillis();
        float hue = (time % 3000) / 3000.0f; // Full cycle every 3 seconds

        // Convert HSV to RGB (hue, full saturation, full brightness)
        float r, g, b;

        if (hue < 1.0f/6.0f) {
            r = 1.0f;
            g = hue * 6.0f;
            b = 0.0f;
        } else if (hue < 2.0f/6.0f) {
            r = (2.0f/6.0f - hue) * 6.0f;
            g = 1.0f;
            b = 0.0f;
        } else if (hue < 3.0f/6.0f) {
            r = 0.0f;
            g = 1.0f;
            b = (hue - 2.0f/6.0f) * 6.0f;
        } else if (hue < 4.0f/6.0f) {
            r = 0.0f;
            g = (4.0f/6.0f - hue) * 6.0f;
            b = 1.0f;
        } else if (hue < 5.0f/6.0f) {
            r = (hue - 4.0f/6.0f) * 6.0f;
            g = 0.0f;
            b = 1.0f;
        } else {
            r = 1.0f;
            g = 0.0f;
            b = (1.0f - hue) * 6.0f;
        }

        return new Vector3f(r, g, b);
    }

    /**
     * Gets a color that cycles between red and purple for blighted tier sparkles
     */
    private static Vector3f getBlightedColor() {
        // Use system time for smooth color cycling
        long time = System.currentTimeMillis();
        float cycle = (time % 2000) / 2000.0f; // Full cycle every 2 seconds

        // Cycle between red (1,0,0) and purple (0.8,0,1)
        float r = 1.0f - (0.2f * cycle);
        float g = 0.0f;
        float b = cycle;

        return new Vector3f(r, g, b);
    }

    /**
     * Gets a color that cycles between red and black for doomed tier sparkles
     */
    private static Vector3f getDoomedColor() {
        // Use system time for smooth color cycling
        long time = System.currentTimeMillis();
        float cycle = (time % 1500) / 1500.0f; // Full cycle every 1.5 seconds

        // Cycle between red (1,0,0) and black (0,0,0)
        float r = 1.0f - cycle;
        float g = 0.0f;
        float b = 0.0f;

        return new Vector3f(r, g, b);
    }

    /**
     * Spawns particles around hostile mobs to make them more distinguishable
     */
    private static void spawnMobParticles(ClientWorld world, ParticleManager particleManager) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Get all hostile sparkles
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();

        for (ClientSparkleManager.ClientSparkle hostileSparkle : hostileSparkles) {
            // Get the color for this sparkle's tier
            Vector3f sparkleColor = getTierColor(hostileSparkle.getTierLevel());

            // Get the spawned mob IDs for this sparkle
            List<UUID> mobIds = hostileSparkle.getSpawnedMobIds();

            for (UUID mobId : mobIds) {
                // Find the mob entity in the world by iterating through all entities
                net.minecraft.entity.Entity mob = null;
                for (net.minecraft.entity.Entity entity : world.getEntities()) {
                    if (entity.getUuid().equals(mobId)) {
                        mob = entity;
                        break;
                    }
                }

                if (mob != null && mob.isAlive()) {
                    // Spawn particles around this mob
                    spawnParticlesAroundMob(world, particleManager, mob, sparkleColor);
                }
            }
        }
    }

    /**
     * Spawns a ring of particles around a mob using the sparkle's color
     */
    private static void spawnParticlesAroundMob(ClientWorld world, ParticleManager particleManager, net.minecraft.entity.Entity mob, Vector3f color) {
        Vec3d mobPos = mob.getPos();
        double mobHeight = mob.getHeight();

        // Spawn particles in a ring around the mob
        int numParticles = 8; // Number of particles in the ring
        double ringRadius = 1.5; // Distance from mob center
        double ringHeight = mobHeight * 0.7; // Height above mob feet

        for (int i = 0; i < numParticles; i++) {
            double angle = (2 * Math.PI * i) / numParticles;

            // Calculate particle position in a circle around the mob
            double offsetX = Math.cos(angle) * ringRadius;
            double offsetZ = Math.sin(angle) * ringRadius;
            double particleY = mobPos.y + ringHeight;

            // Add some random variation
            double randomOffsetX = (world.random.nextDouble() - 0.5) * 0.2;
            double randomOffsetY = (world.random.nextDouble() - 0.5) * 0.2;
            double randomOffsetZ = (world.random.nextDouble() - 0.5) * 0.2;

            Vec3d particlePos = new Vec3d(
                mobPos.x + offsetX + randomOffsetX,
                particleY + randomOffsetY,
                mobPos.z + offsetZ + randomOffsetZ
            );

            // Create colored dust particle
            DustParticleEffect mobParticle = new DustParticleEffect(color, 0.8f);
            particleManager.addParticle(
                mobParticle,
                particlePos.x, particlePos.y, particlePos.z,
                0.0, 0.01, 0.0 // Slight upward motion
            );
        }
    }

    /**
     * Renders countdown timers above sparkles that have active timers
     */
    private static void renderSparkleTimers(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) return;

        // Get all hostile sparkles (only hostile sparkles have timers)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();


        for (ClientSparkleManager.ClientSparkle sparkle : hostileSparkles) {
            if (sparkle.isShowTimer()) {
                renderTimerAboveSparkle(context, sparkle);
            }
        }
    }

    /**
     * Renders a countdown timer above a specific sparkle
     */
    private static void renderTimerAboveSparkle(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context, ClientSparkleManager.ClientSparkle sparkle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) return;

        long currentTime = System.currentTimeMillis();
        long endTime = sparkle.getEndTimeMs();
        long remainingMs = endTime - currentTime;


        // Don't render if timer has expired
        if (remainingMs <= 0) {
            return;
        }

        // Calculate remaining seconds (round up)
        int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

        // Format the timer text
        String timerText = String.valueOf(remainingSeconds);


        // Get sparkle position
        Vec3d sparklePos = Vec3d.ofCenter(sparkle.getPosition()).add(0, 2.0, 0); // Above the sparkle

        // Convert world position to screen coordinates
        net.minecraft.client.render.Camera camera = context.camera();
        if (camera == null) {
            return;
        }

        Vec3d cameraPos = camera.getPos();
        double distance = sparklePos.distanceTo(cameraPos);

        // Only render if within reasonable distance
        if (distance > 32.0) {
            return;
        }

        // Get screen position
        net.minecraft.client.util.math.MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        // Render the timer text with high visibility
        var vertexConsumers = context.consumers();
        if (vertexConsumers != null) {

            // Try a much simpler approach - render directly on screen at a fixed position for testing
            // This will help us see if the issue is with 3D positioning or text rendering itself
            matrices.push();

            // Simple test: render at screen center first to see if text rendering works at all
            matrices.translate(0, 0, 0); // Screen center
            matrices.scale(2.0f, 2.0f, 2.0f); // Make it big so we can see it

            // Use bright red text for maximum visibility
            client.textRenderer.draw(
                timerText,
                0, 0, 0xFFFF0000,
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0x00F000F0, // Full brightness
                15728880
            );
            matrices.pop();
        } else {
        }
    }

    /**
     * Renders timer overlays on the HUD (2D rendering)
     */
    private static void renderTimerOverlays(net.minecraft.client.gui.DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) return;

        // Get all hostile sparkles (only hostile sparkles have timers)
        List<ClientSparkleManager.ClientSparkle> hostileSparkles = ClientSparkleManager.getHostileSparkles();

        for (ClientSparkleManager.ClientSparkle clientSparkle : hostileSparkles) {
            
            // Check if player is within render range
            double distance = client.player.getPos().distanceTo(Vec3d.ofCenter(clientSparkle.getPosition()));
            double renderRange = LootSparkleConfig.getHostileSparkleTextRenderRange();

            if (distance <= renderRange) {
                // Render overlay if there's a timer OR a phase message
                if (clientSparkle.isShowTimer() || clientSparkle.getPhaseMessage() != null) {
                    renderTimerOverlay(drawContext, clientSparkle);
                }
            }
        }
    }

    /**
     * Renders a countdown timer as a 2D overlay on the screen
     */
    private static void renderTimerOverlay(net.minecraft.client.gui.DrawContext drawContext, ClientSparkleManager.ClientSparkle sparkle) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.textRenderer == null) return;

        // Get screen dimensions
        int screenWidth = client.getWindow().getScaledWidth();

        // Render phase message if present
        String phaseMessage = sparkle.getPhaseMessage();
        int baseY = 30; // Base Y position for text

        if (phaseMessage != null && !phaseMessage.isEmpty()) {
            int messageX = screenWidth / 2 - client.textRenderer.getWidth(phaseMessage) / 2;
            int messageY = baseY;
            int messageWidth = client.textRenderer.getWidth(phaseMessage);
            int messageHeight = client.textRenderer.fontHeight;

            // Draw dark overlay behind phase message
            int padding = 4;
            drawContext.fill(
                messageX - padding, 
                messageY - padding, 
                messageX + messageWidth + padding, 
                messageY + messageHeight + padding, 
                0x80000000 // Semi-transparent black
            );

            // Draw phase message with native Minecraft shadow for visibility
            drawContext.drawText(client.textRenderer, phaseMessage, messageX, messageY, 0xFFFF0000, true); // Bright red text with shadow
            baseY += 20; // Move timer down if message is present
        }

        // Render timer if enabled
        if (sparkle.isShowTimer()) {
            long currentTime = System.currentTimeMillis();
            long endTime = sparkle.getEndTimeMs();
            long remainingMs = endTime - currentTime;

            // Don't render if timer has expired
            if (remainingMs <= 0) {
                return;
            }

            // Calculate remaining seconds (round up)
            int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

            // Format the timer text - show blank during preparation phase
            String timerText;
            if (sparkle.isPreparation()) {
                timerText = ""; // Blank text during preparation
            } else {
                timerText = String.valueOf(remainingSeconds);
            }

            // Render timer below the message
            int timerX = screenWidth / 2 - client.textRenderer.getWidth(timerText) / 2;
            int timerY = baseY;
            int timerWidth = client.textRenderer.getWidth(timerText);
            int timerHeight = client.textRenderer.fontHeight;

            // Draw dark overlay behind timer text (only if there's actual text to display)
            if (!timerText.isEmpty()) {
                int padding = 4;
                drawContext.fill(
                    timerX - padding, 
                    timerY - padding, 
                    timerX + timerWidth + padding, 
                    timerY + timerHeight + padding, 
                    0x80000000 // Semi-transparent black
                );
            }

            // Draw bright red text with native Minecraft shadow for maximum visibility
            drawContext.drawText(client.textRenderer, timerText, timerX, timerY, 0xFFFF0000, true); // Bright red text with shadow
        }
    }
}
