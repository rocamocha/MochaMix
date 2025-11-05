package rocamocha.lootsparkle.sparkle;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import rocamocha.lootsparkle.core.LootSparkleConfig;
import rocamocha.lootsparkle.loot.LootTableIntegration;
import rocamocha.lootsparkle.trial.TrialPhase;
import rocamocha.lootsparkle.trial.TrialPhaseLoader;
import rocamocha.lootsparkle.trial.SpawnEntry;
import rocamocha.lootsparkle.trial.Count;
import rocamocha.lootsparkle.trial.Rolls;
import rocamocha.lootsparkle.trial.Challenge;
import rocamocha.lootsparkle.trial.TargetFallingBlockEntity;
import rocamocha.lootsparkle.trial.Duration;
import rocamocha.lootsparkle.network.SparkleNetworking;

/**
 * Represents a single sparkle entity/effect
 *
 * Each sparkle has:
 * - A position in the world
 * - An inventory generated from loot tables
 * - A lifetime
 * - Particle effects
 * - A tier that determines loot quality
 * - A category (normal or hostile) that determines behavior
 */
public class Sparkle {
    /**
     * Represents a particle emitter for challenge phases
     */
    private static class ParticleEmitter {
        private Vec3d position;
        private Vec3d velocity;
        private long spawnTime;
        private long lastUpdateTime;
        private boolean spawned = false;
        private double targetSpawnTime; // The calculated random spawn time for this emitter

        public ParticleEmitter(Vec3d position, Vec3d velocity, long spawnTime, double targetSpawnTime) {
            this.position = position;
            this.velocity = velocity;
            this.spawnTime = spawnTime;
            this.lastUpdateTime = spawnTime;
            this.targetSpawnTime = targetSpawnTime;
        }

        public Vec3d getPosition() { return position; }
        public long getSpawnTime() { return spawnTime; }
        public boolean isSpawned() { return spawned; }
        public void setSpawned(boolean spawned) { this.spawned = spawned; }
        public double getTargetSpawnTime() { return targetSpawnTime; }

        public void updatePosition(long currentTime) {
            if (spawned) return;
            
            // Calculate time delta since last update
            long deltaMs = currentTime - lastUpdateTime;
            double deltaSeconds = deltaMs / 1000.0;
            
            // Update position based on time delta
            position = position.add(
                velocity.x * deltaSeconds,
                velocity.y * deltaSeconds + 0.1 * deltaSeconds, // upward drift
                velocity.z * deltaSeconds
            );
            
            // Update last update time
            lastUpdateTime = currentTime;
            
            // Debug: Log position every few seconds
            long totalElapsedMs = currentTime - spawnTime;
            if (totalElapsedMs % 5000 < 50) { // Log roughly every 5 seconds instead of 2
            }
        }
    }
    private final UUID sparkleId;
    private final UUID playerId;
    private final BlockPos position;
    private final SimpleInventory inventory;
    private final long creationTime;
    private final long lifetime; // milliseconds
    private final SparkleTier tier;
    private final SparkleCategory category;
    private boolean experienceGranted = false;

    // Hostile sparkle specific fields
    private boolean activated = false;
    private net.minecraft.server.network.ServerPlayerEntity activatingPlayer = null;
    private int currentPhase = 0;
    private List<TrialPhase> phases = new ArrayList<>();
    private long lastPhaseCompletionTime = 0;
    private long phaseStartTime = 0;
    private long phaseDurationMs = 0;
    private long preparationEndTime = 0;
    private Set<net.minecraft.entity.Entity> spawnedMobs = new java.util.HashSet<>();
    private boolean forceExpired = false;
    private boolean showTimer = false;
    private String currentPhaseMessage = null;

    // Emitter phase specific fields
    private List<SpawnEntry> pendingEmitterSpawns = new ArrayList<>();
    private long nextEmitterSpawnTime = 0;
    private int remainingSpawnRolls = 0;

    // Challenge phase specific fields
    private List<ParticleEmitter> particleEmitters = new ArrayList<>();
    private Set<net.minecraft.entity.Entity> targetEntities = new java.util.HashSet<>();
    private Set<net.minecraft.entity.Entity> hitTargets = new java.util.HashSet<>();

    public Sparkle(UUID playerId, BlockPos position, World world) {
        this.sparkleId = UUID.randomUUID();
        this.playerId = playerId;
        this.position = position;
        this.inventory = new SimpleInventory(27); // 27 slots like a chest
        this.creationTime = System.currentTimeMillis();
        this.lifetime = LootSparkleConfig.getSparkleLifetimeMs();


        // Get the player entity to check for enchantments
        net.minecraft.server.network.ServerPlayerEntity player = null;
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            player = serverWorld.getServer().getPlayerManager().getPlayer(playerId);
        }

        // Determine sparkle tier based on world context and player enchantments
        this.tier = SparkleTier.selectRandomTier(world, position, player);
        this.category = this.tier.getCategory();


        // Generate loot for this sparkle based on tier
        LootTableIntegration.generateLootForSparkle(this.inventory, this.tier, world, position);

        // Initialize trial phases if this is a trial sparkle
        if (this.category == SparkleCategory.TRIAL) {
            initializeTrialPhases(world);
            // Strike lightning when trial sparkle spawns
            strikeLightning(world);
        }
    }

    /**
     * Creates a sparkle with a specific tier (for debug/testing purposes)
     */
    public Sparkle(UUID playerId, BlockPos position, World world, SparkleTier tier) {
        this.sparkleId = UUID.randomUUID();
        this.playerId = playerId;
        this.position = position;
        this.inventory = new SimpleInventory(27); // 27 slots like a chest
        this.creationTime = System.currentTimeMillis();
        this.lifetime = LootSparkleConfig.getSparkleLifetimeMs();
        this.tier = tier;
        this.category = this.tier.getCategory();


        // Generate loot for this sparkle based on the specified tier
        LootTableIntegration.generateLootForSparkle(this.inventory, tier, world, position);

        // Initialize trial phases if this is a trial sparkle
        if (this.category == SparkleCategory.TRIAL) {
            initializeTrialPhases(world);
            // Strike lightning when trial sparkle spawns
            strikeLightning(world);
        }
    }

    public UUID getSparkleId() {
        return sparkleId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public BlockPos getPosition() {
        return position;
    }

    public SparkleTier getTier() {
        return tier;
    }

    public SimpleInventory getInventory() {
        return inventory;
    }

    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Checks if the sparkle should be removed (empty inventory or expired)
     */
    public boolean isExpired() {
        boolean expired = forceExpired || isInventoryEmpty() || isLifetimeExpired();
        if (expired && isTrial()) {
        }
        return expired;
    }

    /**
     * Checks if the inventory is empty
     */
    public boolean isInventoryEmpty() {
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.getStack(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the sparkle has exceeded its lifetime
     */
    public boolean isLifetimeExpired() {
        return System.currentTimeMillis() - creationTime > lifetime;
    }

    /**
     * Updates the sparkle (called each server tick)
     */
    public void update(World world) {
        // Check if inventory became empty
        if (isInventoryEmpty()) {
        }

        // Handle trial sparkle mob leashing and phase completion
        if (isTrial() && activated) {
            leashSpawnedMobs(world);
            
            // Update sequential spawning phases (emitter, burst, boss)
            updateSequentialSpawning(world);
            
            // Check timer FIRST - this determines if limit duration should expire the sparkle
            checkPhaseTimer(world);
            
            // Then check phase completion - but for limit duration, don't complete early
            checkPhaseCompletion(world);

            // Update challenge phase if active
            updateChallengePhase(world);
        }
    }

    public long getRemainingLifetime() {
        return Math.max(0, lifetime - (System.currentTimeMillis() - creationTime));
    }

    /**
     * Checks if experience has already been granted for this sparkle
     */
    public boolean isExperienceGranted() {
        return experienceGranted;
    }

    /**
     * Marks that experience has been granted for this sparkle
     */
    public void setExperienceGranted(boolean experienceGranted) {
        this.experienceGranted = experienceGranted;
    }

    /**
     * Gets the category of this sparkle
     */
    public SparkleCategory getCategory() {
        return category;
    }

    /**
     * Checks if this sparkle is trial
     */
    public boolean isTrial() {
        return category == SparkleCategory.TRIAL;
    }

    /**
     * Checks if this trial sparkle has been activated
     */
    public boolean isActivated() {
        return activated;
    }

    /**
     * Gets the current phase of this trial sparkle
     */
    public int getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Gets the total number of phases for this trial sparkle
     */
    public int getTotalPhases() {
        return phases.size();
    }

    /**
     * Checks if all phases of this trial sparkle are completed
     */
    public boolean areAllPhasesCompleted() {
        return currentPhase >= phases.size();
    }

    /**
     * Gets the current phase message for this trial sparkle
     */
    public String getCurrentPhaseMessage() {
        return currentPhaseMessage;
    }

    /**
     * Initializes trial phases by loading from datapack
     */
    private void initializeTrialPhases(World world) {
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            // Fallback for client-side
            List<SpawnEntry> fallbackSpawns = List.of(
                new SpawnEntry("minecraft:zombie", null, null, null, false, 10, new Count(1)),
                new SpawnEntry("minecraft:skeleton", null, null, null, false, 8, new Count(1)),
                new SpawnEntry("minecraft:spider", null, null, null, false, 6, new Count(1))
            );
            phases.add(new TrialPhase("emitter", fallbackSpawns, new Rolls(1, 5), new Duration(30, "advance")));
            return;
        }

        // Load phases from datapack
        phases.addAll(TrialPhaseLoader.loadTrialPhases(tier, serverWorld.getServer().getResourceManager()));

        if (phases.isEmpty()) {
            // Fallback phases
            List<SpawnEntry> fallbackSpawns = List.of(
                new SpawnEntry("minecraft:zombie", null, null, null, false, 10, new Count(1)),
                new SpawnEntry("minecraft:skeleton", null, null, null, false, 8, new Count(1)),
                new SpawnEntry("minecraft:spider", null, null, null, false, 6, new Count(1))
            );
            phases.add(new TrialPhase("emitter", fallbackSpawns, new Rolls(1, 5), new Duration(30, "advance")));
        }
    }

    /**
     * Strikes lightning at the sparkle's position
     */
    private void strikeLightning(World world) {
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            net.minecraft.entity.LightningEntity lightning = new net.minecraft.entity.LightningEntity(
                net.minecraft.entity.EntityType.LIGHTNING_BOLT,
                serverWorld
            );
            lightning.setPosition(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
            lightning.setCosmetic(true); // Prevent fires from lightning
            lightning.setSilent(true); // Make lightning silent
            serverWorld.spawnEntity(lightning);
        }
    }

    /**
     * Launches fireworks at the sparkle's position
     */
    private void launchFireworks(World world) {
        if (world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            // Launch multiple spectacular fireworks in a circle
            launchSpectacularFireworks(serverWorld);
        }
    }

    /**
     * Launches a spectacular firework show with multiple colorful fireworks
     */
    private void launchSpectacularFireworks(net.minecraft.server.world.ServerWorld serverWorld) {
        // Launch 3-5 fireworks in a circle around the sparkle
        int numFireworks = 3 + serverWorld.getRandom().nextInt(3); // 3-5 fireworks

        for (int i = 0; i < numFireworks; i++) {
            // Position fireworks in a circle around the sparkle
            double angle = (2 * Math.PI * i) / numFireworks;
            double radius = 1.0 + serverWorld.getRandom().nextDouble() * 2.0; // 1-3 blocks away
            double x = position.getX() + 0.5 + Math.cos(angle) * radius;
            double z = position.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = position.getY() + 1.0;

            // Create firework entity with colorful explosions
            net.minecraft.entity.projectile.FireworkRocketEntity firework = new net.minecraft.entity.projectile.FireworkRocketEntity(
                serverWorld, x, y, z, createColorfulFirework(serverWorld)
            );

            serverWorld.spawnEntity(firework);
        }
    }

    /**
     * Creates a firework rocket item with colorful explosions
     */
    private net.minecraft.item.ItemStack createColorfulFirework(net.minecraft.server.world.ServerWorld serverWorld) {
        net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.FIREWORK_ROCKET);
        
        // Pick a primary color scheme for this firework
        int[] primaryColors = getRandomColorScheme(serverWorld.getRandom());
        
        // Create firework explosions using Minecraft's component system
        java.util.List<net.minecraft.component.type.FireworkExplosionComponent> explosions = new java.util.ArrayList<>();
        
        // Add 1-3 explosions, all using the same color scheme
        int numExplosions = 1 + serverWorld.getRandom().nextInt(3);
        for (int i = 0; i < numExplosions; i++) {
            // Create explosion with random shape but consistent colors
            net.minecraft.component.type.FireworkExplosionComponent.Type[] types = 
                net.minecraft.component.type.FireworkExplosionComponent.Type.values();
            net.minecraft.component.type.FireworkExplosionComponent.Type type = 
                types[serverWorld.getRandom().nextInt(types.length)];
            
            explosions.add(new net.minecraft.component.type.FireworkExplosionComponent(
                type,
                it.unimi.dsi.fastutil.ints.IntList.of(primaryColors),
                it.unimi.dsi.fastutil.ints.IntList.of(), // No fade colors
                serverWorld.getRandom().nextBoolean(), // Trail
                serverWorld.getRandom().nextBoolean()  // Twinkle
            ));
        }
        
        // Set the fireworks component
        stack.set(
            net.minecraft.component.DataComponentTypes.FIREWORKS,
            new net.minecraft.component.type.FireworksComponent(
                1 + serverWorld.getRandom().nextInt(1), // Flight duration 1-3
                explosions
            )
        );
        
        return stack;
    }

    /**
     * Gets a random color scheme for a firework (2-3 related colors)
     */
    private int[] getRandomColorScheme(net.minecraft.util.math.random.Random random) {
        // Predefined color schemes for cohesive fireworks
        int[][] colorSchemes = {
            {0xFF0000, 0xFF4444, 0xFF8888}, // Red variations
            {0x0000FF, 0x4444FF, 0x8888FF}, // Blue variations  
            {0x00FF00, 0x44FF44, 0x88FF88}, // Green variations
            {0xFFFF00, 0xFFFF44, 0xFFFF88}, // Yellow variations
            {0xFF00FF, 0xFF44FF, 0xFF88FF}, // Magenta variations
            {0x00FFFF, 0x44FFFF, 0x88FFFF}, // Cyan variations
            {0xFFFFFF, 0xEEEEEE, 0xDDDDDD}, // White/gray variations
            {0xFFA500, 0xFFAA22, 0xFFBB44}  // Orange variations
        };
        
        int[] scheme = colorSchemes[random.nextInt(colorSchemes.length)];
        // Return 2-3 colors from the scheme
        int numColors = 2 + random.nextInt(2); // 2-3 colors
        int[] result = new int[numColors];
        for (int i = 0; i < numColors; i++) {
            result[i] = scheme[i];
        }
        return result;
    }

    /**
     * Activates this trial sparkle if a player is within range
     */
    public void checkActivation(net.minecraft.server.network.ServerPlayerEntity player) {
        if (!isTrial() || activated) return;

        double distance = player.getPos().distanceTo(Vec3d.ofCenter(position));
        double activationRange = LootSparkleConfig.getHostileSparkleActivationRange();

        if (distance <= activationRange) {
            activated = true;
            activatingPlayer = player;
            startCurrentPhase(player.getWorld());
        }
    }

    /**
     * Forces activation of this trial sparkle (used when player attempts to interact)
     */
    public void forceActivate(net.minecraft.server.network.ServerPlayerEntity player) {
        if (!isTrial() || activated) return;
        activated = true;
        activatingPlayer = player;
        startCurrentPhase(player.getWorld());
    }

    /**
     * Starts the current phase of mob spawning
     */
    private void startCurrentPhase(World world) {
        if (currentPhase >= phases.size()) return;

        TrialPhase phase = phases.get(currentPhase);

        // Send chat message to nearby players
        sendPhaseStartMessage(world, phase);

        if (phase.isEmitter()) {
            startEmitterPhase(phase, world);
        } else if (phase.isBurst() || phase.isBoss()) {
            startEmitterPhase(phase, world); // Burst phases use same logic as emitter for now
        } else if (phase.isChallenge()) {
            startChallengePhase(phase, world);
        }
    }

    /**
     * Sends a phase start message to nearby players via HUD overlay
     */
    private void sendPhaseStartMessage(World world, TrialPhase phase) {
        Duration duration = phase.getDuration();
        String message;

        if (phase.isChallenge()) {
            message = "§cShoot the targets!";
        } else if (duration.isAdvance()) {
            message = "§cEnemies have appeared!";
        } else if (duration.isLimit()) {
            message = "§cDefeat all enemies!";
        } else if (duration.isSurvive()) {
            message = "§cSurvive the ambush!";
        } else {
            message = "§cHostile phase started!";
        }

        // Set the current phase message
        currentPhaseMessage = message;

        // Clear any existing phase message first, then send the new one
        sendPhaseMessagePacket(world, null); // Clear message
        sendPhaseMessagePacket(world, message); // Send new message
    }

    /**
     * Sends a phase message packet to nearby players
     */
    private void sendPhaseMessagePacket(World world, String message) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Send HUD message packet to all players within 32 blocks
        Vec3d sparklePos = Vec3d.ofCenter(position);
        for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
            double distance = player.getPos().distanceTo(sparklePos);
            if (distance <= 32.0) {
                SparkleNetworking.PhaseMessagePacket packet = new SparkleNetworking.PhaseMessagePacket(
                    sparkleId, playerId, message
                );
                ServerPlayNetworking.send(player, packet);
            }
        }
    }

    /**
     * Sends a networking packet to sync this sparkle to all clients
     */
    private void sendSyncPacket(World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Send sync packet to all players
        for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
            ServerPlayNetworking.send(player, new SparkleNetworking.SyncSparklePacket(
                sparkleId, playerId, position, tier.getLevel(), currentPhaseMessage
            ));
        }
    }

    /**
     * Starts an emitter phase (sequential spawning)
     */
    private void startEmitterPhase(TrialPhase phase, World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Clear previous spawned mobs for this phase
        spawnedMobs.clear();

        // Clear any previous emitter state
        pendingEmitterSpawns.clear();
        nextEmitterSpawnTime = 0;

        // Set phase timer only for timed phases (limit and survive)
        Duration duration = phase.getDuration();
        if (duration.isLimit() || duration.isSurvive()) {
            phaseStartTime = System.currentTimeMillis();
            phaseDurationMs = phase.getDuration().getValue() * 1000L;
        } else {
            // For advance phases, no timer - complete immediately when all mobs defeated
            phaseStartTime = 0;
            phaseDurationMs = 0;
        }

        // Set timer display for limit and survive phases
        showTimer = duration.isLimit() || duration.isSurvive();
        long endTimeMs = showTimer ? phaseStartTime + phaseDurationMs : 0;

        // Send timer sync packet to clients
        if (showTimer) {
            sendTimerPacket(world, endTimeMs, false); // false for emitter phase (no preparation)
        }

        // Store number of spawns to roll instead of pre-rolling them
        remainingSpawnRolls = phase.getRolls().getValue(serverWorld.getRandom());

        // Set initial spawn time (slight delay before first spawn)
        nextEmitterSpawnTime = System.currentTimeMillis() + 1000L; // 1 second delay

        // Send networking packet to sync spawned mobs to clients (initially empty)
        sendSpawnedMobsPacket(serverWorld);

        // Phase will complete when all spawned mobs are killed or timer expires
    }

    /**
     * Starts a challenge phase
     */
    private void startChallengePhase(TrialPhase phase, World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;


        // Clear any previous challenge state
        particleEmitters.clear();
        targetEntities.clear();
        hitTargets.clear();

        // Get the target challenge (for now, assume first challenge is target)
        List<Challenge> challenges = phase.getChallenges();
        if (challenges.isEmpty()) {
            return;
        }

        Challenge targetChallenge = challenges.get(0); // For now, only handle first challenge
        if (!targetChallenge.isTarget()) {
            return;
        }

        int count = targetChallenge.getCount();

        // Set phase duration for challenge (but don't start timer yet - wait for all targets to spawn)
        phaseDurationMs = phase.getDuration().getValue() * 1000L;
        // phaseStartTime remains 0 until all targets are spawned
        showTimer = true; // Show timer immediately for preparation phase
        preparationEndTime = System.currentTimeMillis() + 4000L; // 4 seconds preparation time
        sendTimerPacket(world, preparationEndTime, true);

        // Find a suitable starting position above ground level
        Vec3d sparklePos = Vec3d.ofCenter(position);
        Vec3d emitterStartPos = findEmitterStartPosition(serverWorld, sparklePos);

        // Create particle emitters
        for (int i = 0; i < count; i++) {
            // Random direction with upward drift
            double angle = serverWorld.getRandom().nextDouble() * 2 * Math.PI;
            double speed = 2.0 + serverWorld.getRandom().nextDouble() * 2.0; // 2-4 blocks per second
            double upwardDrift = 0.5 + serverWorld.getRandom().nextDouble() * 1.5; // 0.5-2 blocks per second upward
            
            Vec3d velocity = new Vec3d(
                Math.cos(angle) * speed,
                upwardDrift, // Add upward velocity for overhead spawning
                Math.sin(angle) * speed
            );

            // Calculate random spawn time for this emitter (2-4 seconds from now)
            // Ensure at least one emitter takes the maximum time (4 seconds)
            double targetSpawnTime;
            if (i == 0) {
                // First emitter always takes max time to ensure there's a delay
                targetSpawnTime = 4.0;
            } else {
                targetSpawnTime = 2.0 + serverWorld.getRandom().nextDouble() * 2.0;
            }

            ParticleEmitter emitter = new ParticleEmitter(emitterStartPos, velocity, System.currentTimeMillis(), targetSpawnTime);
            particleEmitters.add(emitter);
        }

    }

    /**
     * Finds a suitable starting position for particle emitters above ground level
     */
    private Vec3d findEmitterStartPosition(ServerWorld world, Vec3d sparklePos) {
        // Start from the sparkle position and scan upward to find air
        net.minecraft.util.math.BlockPos.Mutable mutablePos = new net.minecraft.util.math.BlockPos.Mutable(
            (int) Math.floor(sparklePos.x),
            (int) Math.floor(sparklePos.y),
            (int) Math.floor(sparklePos.z)
        );

        // Scan upward from the sparkle position to find the first air block above ground
        int maxHeight = world.getTopY();
        for (int y = mutablePos.getY(); y < maxHeight; y++) {
            mutablePos.setY(y);
            net.minecraft.block.BlockState state = world.getBlockState(mutablePos);
            
            // If we find air, check if there's solid ground below
            if (state.isAir() || state.getFluidState().isEmpty()) {
                // Check if there's solid ground 1-2 blocks below
                boolean hasGround = false;
                for (int groundCheck = 1; groundCheck <= 3; groundCheck++) {
                    net.minecraft.util.math.BlockPos groundPos = mutablePos.down(groundCheck);
                    if (world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                        hasGround = true;
                        break;
                    }
                }
                
                if (hasGround) {
                    // Found a good position - add some height to ensure targets spawn above ground
                    return new Vec3d(
                        sparklePos.x,
                        y + 2.0, // 2 blocks above the ground level we found
                        sparklePos.z
                    );
                }
            }
        }

        // Fallback: if we can't find a good position, use the original sparkle position + 10 blocks up
        return sparklePos.add(0, 10, 0);
    }

    /**
     * Selects a single spawn from a phase by rolling on the weighted list
     */
    private SpawnEntry selectSingleSpawnFromPhase(TrialPhase phase, net.minecraft.util.math.random.Random random) {
        // Get weighted spawns
        List<SpawnEntry> weightedSpawns = phase.getSpawns().stream()
            .filter(SpawnEntry::isWeighted)
            .collect(java.util.stream.Collectors.toList());

        if (weightedSpawns.isEmpty()) {
            return null;
        }

        // Roll on weighted list
        int totalWeight = weightedSpawns.stream().mapToInt(SpawnEntry::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (SpawnEntry spawn : weightedSpawns) {
            currentWeight += spawn.getWeight();
            if (roll < currentWeight) {
                return spawn;
            }
        }

        return null;
    }

    /**
     * Spawns a mob from a SpawnEntry at the given position
     */
    private void spawnMobFromEntry(ServerWorld world, SpawnEntry spawn, BlockPos position) {
        try {
            EntityType<?> entityType = EntityType.get(spawn.getMobId()).orElse(null);
            if (entityType == null) {
                return;
            }

            // Find a valid spawn position near the sparkle
            BlockPos spawnPos = findValidMobSpawnPosition(world, position);
            if (spawnPos == null) {
                spawnPos = position; // Fallback to sparkle position
            }

            // Spawn the mob directly
            net.minecraft.entity.Entity mob = entityType.spawn(world, spawnPos, SpawnReason.NATURAL);
            if (mob != null && mob.isAlive() && !mob.isRemoved()) {
                // Apply customizations
                applySpawnEntryToMob(spawn, mob, world);

                // Set the mob to target the activating player immediately
                if (activatingPlayer != null && mob instanceof net.minecraft.entity.mob.MobEntity mobEntity) {
                    mobEntity.setTarget(activatingPlayer);
                }

                spawnedMobs.add(mob);
            } else {
            }
        } catch (Exception e) {
        }
    }

    /**
     * Applies SpawnEntry customizations to a spawned mob
     */
    private void applySpawnEntryToMob(SpawnEntry spawn, net.minecraft.entity.Entity mob, ServerWorld world) {
        if (!(mob instanceof net.minecraft.entity.LivingEntity living)) return;

        // Apply armor
        if (spawn.getArmor() != null && !spawn.getArmor().isEmpty()) {
            applyArmorToMob(spawn.getArmor(), living);
        }

        // Apply attributes - TODO: Implement for 1.21.1
        if (spawn.getAttributes() != null && !spawn.getAttributes().isEmpty()) {
        }

        // Apply name
        if (spawn.getName() != null && !spawn.getName().isEmpty()) {
            living.setCustomName(net.minecraft.text.Text.of(spawn.getName()));
            living.setCustomNameVisible(true);
        }

        // Apply boss bar - TODO: Implement
        if (spawn.isBoss()) {
        }
    }

    /**
     * Applies armor to a mob
     */
    private void applyArmorToMob(Map<String, String> armor, net.minecraft.entity.LivingEntity mob) {
        for (Map.Entry<String, String> entry : armor.entrySet()) {
            String slot = entry.getKey();
            String itemId = entry.getValue();

            try {
                net.minecraft.item.Item item = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(itemId));
                if (item != null) {
                    net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item);

                    switch (slot.toLowerCase()) {
                        case "head", "helmet" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, stack);
                        case "chest", "chestplate" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, stack);
                        case "legs", "leggings" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.LEGS, stack);
                        case "feet", "boots" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.FEET, stack);
                        case "mainhand" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, stack);
                        case "offhand" -> mob.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND, stack);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Finds a valid position to spawn a mob near the sparkle
     */
    private BlockPos findValidMobSpawnPosition(ServerWorld world, BlockPos center) {
        // Try positions within 3 blocks of the sparkle (spherical radius)
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    // Check if within 3 block radius (Euclidean distance)
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance <= 3.0) {
                        BlockPos checkPos = center.add(x, y, z);
                        if (world.getBlockState(checkPos).isAir() &&
                            world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                            return checkPos;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the current phase timer has expired
     */
    private void checkPhaseTimer(World world) {
        if (currentPhase >= phases.size() || phaseStartTime == 0) return;

        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - phaseStartTime;
        
        if (elapsedMs >= phaseDurationMs) {
            TrialPhase phase = phases.get(currentPhase);
            Duration duration = phase.getDuration();

            // Clear the phase message when timer expires
            currentPhaseMessage = null;
            sendPhaseMessagePacket(world, null);

            // Send updated sync packet to ensure clients clear the phase message
            sendSyncPacket(world);

            if (phase.isChallenge()) {
                // Check if all targets have been hit before timing out
                boolean allTargetsHit = !targetEntities.isEmpty() && hitTargets.size() >= targetEntities.size();
                if (!allTargetsHit) {
                    // Challenge phase timed out - clean up targets and fail the sparkle
                    cleanupChallengePhase(world);
                    forceExpired = true;
                }
                // If all targets were hit, don't expire - the phase will complete normally
            } else if (duration.isLimit()) {
                // For limit duration, if timer expires with enemies still alive, expire the sparkle
                // (Phase completion when enemies defeated is now handled in checkPhaseCompletion)
                if (!spawnedMobs.isEmpty()) {
                    // Time limit expired with enemies still alive - expire sparkle
                    forceExpired = true;
                }
                // If no enemies left, the phase was already completed in checkPhaseCompletion
            } else if (duration.isSurvive()) {
                // Poof enemies and advance
                poofSpawnedMobs(world);
                completeCurrentPhase(world);
            }
            // Note: Advance phases don't have timers, so they won't reach this code
        }
    }

    /**
     * Poofs (removes) all spawned mobs without dropping loot
     */
    private void poofSpawnedMobs(World world) {
        for (net.minecraft.entity.Entity mob : spawnedMobs) {
            if (mob.isAlive()) {
                mob.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        spawnedMobs.clear();
    }

    /**
     * Checks if the current phase should be completed (all spawned mobs are dead)
     */
    private void checkPhaseCompletion(World world) {
        // Remove dead mobs from tracking (both dead and removed entities)
        int beforeCount = spawnedMobs.size();
        
        java.util.Iterator<net.minecraft.entity.Entity> iterator = spawnedMobs.iterator();
        while (iterator.hasNext()) {
            net.minecraft.entity.Entity entity = iterator.next();
            if (!entity.isAlive() || entity.isRemoved()) {
                iterator.remove();
                
                // Add bonus time for combat phases when a mob is killed
                if (currentPhase < phases.size()) {
                    TrialPhase phase = phases.get(currentPhase);
                    if (phase.isEmitter() || phase.isBurst() || phase.isBoss()) {
                        Integer bonus = phase.getBonus();
                        if (bonus > 0 && phaseDurationMs > 0) {
                            // Add bonus time to the phase duration
                            phaseDurationMs += bonus * 1000L;
                            
                            // Update the timer display for clients
                            if (showTimer) {
                                long endTimeMs = phaseStartTime + phaseDurationMs;
                                sendTimerPacket(world, endTimeMs, false);
                            }
                        }
                    }
                }
            }
        }
        
        // If no mobs are left alive, check if we can complete the phase
        if (spawnedMobs.isEmpty() && beforeCount > 0) {
            
            TrialPhase phase = phases.get(currentPhase);
            Duration duration = phase.getDuration();
            
            // For emitter/burst/boss phases, wait until all intended spawns have been spawned
            boolean allSpawnsComplete = true;
            if (phase.isEmitter() || phase.isBurst() || phase.isBoss()) {
                allSpawnsComplete = remainingSpawnRolls <= 0;
            }
            
            if (allSpawnsComplete) {
                if (duration.isLimit()) {
                    // For limit duration, complete when all enemies are defeated
                    completeCurrentPhase(world);
                } else if (duration.isAdvance()) {
                    // For advance duration, complete when all enemies are defeated
                    completeCurrentPhase(world);
                } else if (duration.isSurvive()) {
                    // For survive duration, complete when enemies defeated
                    completeCurrentPhase(world);
                }
            }
        }
    }

    /**
     * Updates sequential spawning phases (emitter, burst, boss)
     */
    private void updateSequentialSpawning(World world) {
        if (currentPhase >= phases.size()) return;
        
        TrialPhase phase = phases.get(currentPhase);
        if (!phase.isEmitter() && !phase.isBurst() && !phase.isBoss()) return;

        if (!(world instanceof ServerWorld serverWorld)) return;

        long currentTime = System.currentTimeMillis();

        // Check if it's time to spawn the next mob
        if (remainingSpawnRolls > 0 && currentTime >= nextEmitterSpawnTime) {
            // Roll for a spawn from the weighted list
            SpawnEntry spawn = selectSingleSpawnFromPhase(phase, serverWorld.getRandom());
            
            if (spawn != null) {
                int count = spawn.getCount().getValue(serverWorld.getRandom());
                for (int i = 0; i < count; i++) {
                    spawnMobFromEntry(serverWorld, spawn, position);
                }
            }
            
            remainingSpawnRolls--;

            // Set next spawn time based on the phase's rate
            Integer rate = phase.getRate();
            long delayMs = rate * 1000L; // Rate is guaranteed to be non-null (defaults to 5)
            nextEmitterSpawnTime = currentTime + delayMs;

            // Send updated networking packet to sync spawned mobs to clients
            sendSpawnedMobsPacket(serverWorld);
        }
    }

    /**
     * Updates the challenge phase (particle emitters, target spawning, completion check)
     */
    private void updateChallengePhase(World world) {
        if (currentPhase >= phases.size()) return;
        
        TrialPhase phase = phases.get(currentPhase);
        if (!phase.isChallenge()) return;

        if (!(world instanceof ServerWorld serverWorld)) return;

        long currentTime = System.currentTimeMillis();

        // Check if preparation phase has ended and start the actual challenge timer
        if (phaseStartTime == 0 && currentTime >= preparationEndTime) {
            // Preparation phase ended - start the actual challenge timer
            phaseStartTime = System.currentTimeMillis();
            sendTimerPacket(world, phaseStartTime + phaseDurationMs, false); // false for challenge phase
        }

        // Update particle emitters
        for (ParticleEmitter emitter : particleEmitters) {
            if (!emitter.isSpawned()) {
                emitter.updatePosition(currentTime);
                
                // Spawn particles at emitter position
                spawnEmitterParticles(serverWorld, emitter);
                
                // Check if it's time to spawn (using the pre-calculated target spawn time)
                long elapsedMs = currentTime - emitter.getSpawnTime();
                double elapsedSeconds = elapsedMs / 1000.0;
                
                if (elapsedSeconds >= emitter.getTargetSpawnTime()) {
                    spawnTargetEntity(world, emitter);
                }
            }
        }

        // Check for arrow hits on target blocks
        checkTargetHits(world);

        // Check if all targets have been hit
        if (!targetEntities.isEmpty() && hitTargets.size() >= targetEntities.size()) {
            completeCurrentPhase(world);
        }
    }

    /**
     * Spawns particles for a particle emitter
     */
    private void spawnEmitterParticles(ServerWorld world, ParticleEmitter emitter) {
        Vec3d pos = emitter.getPosition();
        
        // Spawn a few particles around the emitter position
        for (int i = 0; i < 3; i++) {
            double offsetX = (world.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = world.getRandom().nextDouble() * 0.3;
            double offsetZ = (world.getRandom().nextDouble() - 0.5) * 0.5;
            
            // Use ENCHANT particles like the treasure compass
            world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT,
                pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                1, 0.0, 0.1, 0.0, 0.05);
        }
    }
    private void spawnTargetEntity(World world, ParticleEmitter emitter) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d pos = emitter.getPosition();


        // Validate position - make sure it's not inside a block
        net.minecraft.util.math.BlockPos blockPos = net.minecraft.util.math.BlockPos.ofFloored(pos.x, pos.y, pos.z);
        net.minecraft.block.BlockState blockState = serverWorld.getBlockState(blockPos);
        if (!blockState.isAir() && !blockState.getFluidState().isEmpty()) {
            return;
        }

        // Get the target challenge to check camo
        if (currentPhase >= phases.size()) {
            return;
        }

        TrialPhase phase = phases.get(currentPhase);
        if (phase.getChallenges().isEmpty()) {
            return;
        }

        Challenge targetChallenge = phase.getChallenges().get(0);

        // Create a falling block entity with the target block (like when a piston pushes a block)
        net.minecraft.block.Block targetBlock = net.minecraft.block.Blocks.TARGET;
        if (targetChallenge.isCamo()) {
            // Use random nearby block texture - but for now, just use target block
            // Camo would require more complex logic for falling block entities
        }

        net.minecraft.block.BlockState targetBlockState = targetBlock.getDefaultState();

        // Create the target entity using our custom class that can be damaged by projectiles
        TargetFallingBlockEntity fallingBlock = new TargetFallingBlockEntity(this, serverWorld, pos.x, pos.y, pos.z, targetBlockState);

        // Make the falling block stationary and persistent (like a piston-pushed block)
        fallingBlock.setNoGravity(true);
        fallingBlock.setVelocity(0, 0, 0);
        fallingBlock.setHurtEntities(0.0f, 0); // Don't hurt entities when it "falls"
        fallingBlock.timeFalling = Integer.MAX_VALUE; // Prevent natural falling behavior

        // Spawn the entity
        boolean spawnResult = serverWorld.spawnEntity(fallingBlock);
        if (spawnResult) {
            targetEntities.add(fallingBlock);
            emitter.setSpawned(true);
        }
    }

    /**
     * Checks for arrow hits on target entities
     * Note: Hit detection is now primarily handled by TargetFallingBlockEntity.damage()
     */
    private void checkTargetHits(World world) {
        // Hit detection is now handled by the entity's damage() method
        // This method is kept for potential fallback or additional logic if needed
    }

    /**
     * Cleans up challenge phase state (remove remaining target entities, etc.)
     */
    private void cleanupChallengePhase(World world) {

        // Remove any remaining target entities
        for (net.minecraft.entity.Entity targetEntity : targetEntities) {
            if (targetEntity.isAlive() && !targetEntity.isRemoved()) {
                targetEntity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
        }

        // Clear challenge state
        particleEmitters.clear();
        targetEntities.clear();
        hitTargets.clear();
    }

    /**
     * Leashes spawned mobs to the sparkle position
     */
    private void leashSpawnedMobs(World world) {
        double leashRange = LootSparkleConfig.getHostileMobLeashRange();
        Vec3d sparklePos = Vec3d.ofCenter(position);

        for (net.minecraft.entity.Entity mob : spawnedMobs) {
            if (mob.isAlive()) {
                double distance = mob.getPos().distanceTo(sparklePos);
                if (distance > leashRange) {
                    // Find a valid position near the sparkle to teleport to
                    BlockPos teleportPos = findValidMobSpawnPosition((ServerWorld) world, position);
                    if (teleportPos == null) {
                        teleportPos = position;
                    }

                    // Teleport the mob back
                    mob.setPosition(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
                }
            }
        }
    }

    /**
     * Sends a networking packet to sync spawned mobs to all clients
     */
    private void sendSpawnedMobsPacket(ServerWorld world) {
        List<UUID> mobIds = spawnedMobs.stream()
            .map(net.minecraft.entity.Entity::getUuid)
            .collect(java.util.stream.Collectors.toList());

        SparkleNetworking.SyncSpawnedMobsPacket packet = new SparkleNetworking.SyncSpawnedMobsPacket(
            sparkleId, playerId, mobIds
        );

        // Send to all players in the world
        for (net.minecraft.server.network.ServerPlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /**
     * Called when a target entity is hit by a projectile
     */
    public void onTargetHit(TargetFallingBlockEntity targetEntity, net.minecraft.entity.damage.DamageSource source, World world) {
        
        // Launch a firework at the target's location for visual flair
        launchTargetHitFirework(world, targetEntity.getPos());
        
        // Send hit confirmation message to nearby players
        sendTargetHitMessage(world);
        
        // Add to hit targets and remove the entity
        hitTargets.add(targetEntity);
        targetEntity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
    }

    /**
     * Launches a single firework at the specified position when a target is hit
     */
    private void launchTargetHitFirework(World world, Vec3d position) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        // Spawn firework explosion particles directly at the position
        // This creates the explosion effect without the rocket launch
        
        // Spawn a burst of colored particles for the explosion effect
        for (int i = 0; i < 20; i++) {
            double offsetX = (serverWorld.getRandom().nextDouble() - 0.5) * 2.0;
            double offsetY = serverWorld.getRandom().nextDouble() * 2.0;
            double offsetZ = (serverWorld.getRandom().nextDouble() - 0.5) * 2.0;
            
            // Use firework particles with the random color
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.FIREWORK, 
                position.x + offsetX, 
                position.y + offsetY, 
                position.z + offsetZ,
                1, // Count
                0.0, 0.0, 0.0, // Motion
                1.0 // Speed
            );
        }
        
        // Also spawn some sparkle particles for extra effect
        for (int i = 0; i < 10; i++) {
            double offsetX = (serverWorld.getRandom().nextDouble() - 0.5) * 1.5;
            double offsetY = serverWorld.getRandom().nextDouble() * 1.5;
            double offsetZ = (serverWorld.getRandom().nextDouble() - 0.5) * 1.5;
            
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.ENCHANT, 
                position.x + offsetX, 
                position.y + offsetY, 
                position.z + offsetZ,
                1, // Count
                0.0, 0.1, 0.0, // Slight upward motion
                0.05 // Speed
            );
        }
    }

    /**
     * Sends a target hit message to nearby players
     */
    private void sendTargetHitMessage(World world) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        String message = "§aTarget hit!";

        // Send chat message to all players within 32 blocks
        Vec3d sparklePos = Vec3d.ofCenter(position);
        for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
            double distance = player.getPos().distanceTo(sparklePos);
            if (distance <= 32.0) {
                player.sendMessage(net.minecraft.text.Text.of(message), false);
            }
        }
    }

    /**
     * Sends a networking packet to sync timer display to all clients
     */
    private void sendTimerPacket(World world, long endTimeMs, boolean isPreparation) {
        if (!(world instanceof ServerWorld serverWorld)) return;

        SparkleNetworking.SyncTimerPacket packet = new SparkleNetworking.SyncTimerPacket(
            sparkleId, playerId, showTimer, endTimeMs, isPreparation
        );

        // Send to all players in the world
        for (net.minecraft.server.network.ServerPlayerEntity player : serverWorld.getPlayers()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /**
     * Completes the current phase and moves to the next
     */
    public void completeCurrentPhase(World world) {
        if (currentPhase >= phases.size()) {
            return;
        }

        // Clean up challenge state if this was a challenge phase
        if (currentPhase < phases.size()) {
            TrialPhase phase = phases.get(currentPhase);
            if (phase.isChallenge()) {
                cleanupChallengePhase(world);
            } else if (phase.isEmitter() || phase.isBurst() || phase.isBoss()) {
                // Clear sequential spawning phase state
                pendingEmitterSpawns.clear();
                nextEmitterSpawnTime = 0;
                remainingSpawnRolls = 0;
            }
        }

        currentPhase++;
        lastPhaseCompletionTime = System.currentTimeMillis();

        // Reset phase timer
        phaseStartTime = 0;
        phaseDurationMs = 0;

        // Check if this was the final phase
        if (currentPhase >= phases.size()) {
            // All phases completed - launch fireworks!
            launchFireworks(world);

            // Clear the phase message for final phase completion
            currentPhaseMessage = null;
            sendPhaseMessagePacket(world, null);
            sendSyncPacket(world);
        } else {
            // Strike lightning when phase completes (but not the final phase)
            strikeLightning(world);

            // Start next phase if available - this sets the new phase message
            startCurrentPhase(world);

            // Send sync packet with the new phase message
            sendSyncPacket(world);
        }

        // Hide the timer display
        showTimer = false;
        sendTimerPacket(world, 0, false);
    }

    /**
     * Cleans up all entities associated with this sparkle to prevent ghost entities
     * when the sparkle is removed (e.g., on player disconnect/reconnect)
     */
    public void cleanupAllEntities(World world) {
        // Clean up spawned mobs
        for (net.minecraft.entity.Entity mob : spawnedMobs) {
            if (mob.isAlive() && !mob.isRemoved()) {
                mob.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        spawnedMobs.clear();

        // Clean up target entities from challenge phases
        for (net.minecraft.entity.Entity targetEntity : targetEntities) {
            if (targetEntity.isAlive() && !targetEntity.isRemoved()) {
                targetEntity.remove(net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        targetEntities.clear();
        hitTargets.clear();

        // Clear challenge state
        particleEmitters.clear();
        pendingEmitterSpawns.clear();
    }
}
