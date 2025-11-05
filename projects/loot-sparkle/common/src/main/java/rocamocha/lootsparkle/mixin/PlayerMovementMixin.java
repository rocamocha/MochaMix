package rocamocha.lootsparkle.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import rocamocha.lootsparkle.ClientSparkleManager;
import rocamocha.lootsparkle.LootSparkle;
import rocamocha.lootsparkle.LootSparkleConfig;
import rocamocha.lootsparkle.SparkleManager;
import rocamocha.lootsparkle.SparkleNetworking;
import rocamocha.lootsparkle.TreasureCompassItem;

/**
 * Mixin to handle player movement and sparkle spawning/interaction
 *
 * Injects into player movement to potentially spawn sparkles and detect crouching near them
 */
@Mixin(PlayerEntity.class)
public class PlayerMovementMixin {

    // Track previous crouching state to detect when crouching starts
    private boolean wasSneaking = false;

    // Track ticks for fairy dust durability loss (every 20 ticks)
    private int fairyDustTickCounter = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lootsparkle$onPlayerTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        World world = player.getWorld();

        // Only spawn sparkles on server side
        if (world.isClient()) {
            // Handle client-side crouching detection
            handleClientCrouchingDetection(player);
            return;
        }

        // Server-side sparkle spawning
        // Random chance to spawn sparkle (adjust probability as needed)
        if (world.random.nextFloat() < 0.01f) { // 1% chance per tick for testing
            BlockPos playerPos = player.getBlockPos();
            SparkleManager.spawnSparkleForPlayer(player.getUuid(), world, playerPos);
        }

        // Handle fairy dust durability loss
        handleFairyDustDurabilityLoss(player);
    }

    private void handleClientCrouchingDetection(PlayerEntity player) {
        boolean isCurrentlySneaking = player.isSneaking();

        // Detect when player starts crouching (was not sneaking, now is)
        if (!wasSneaking && isCurrentlySneaking) {
            checkForNearbySparkles(player);
        }

        // Update previous state
        wasSneaking = isCurrentlySneaking;
    }

    /**
     * Handles durability loss for treasure compass with Fairy Dust enchantment
     */
    private void handleFairyDustDurabilityLoss(PlayerEntity player) {
        fairyDustTickCounter++;

        // Check every N ticks (configurable)
        int tickInterval = LootSparkleConfig.getFairyDustTickInterval();
        if (fairyDustTickCounter >= tickInterval) {
            fairyDustTickCounter = 0;

            // Check if player has treasure compass with Fairy Dust
            var treasureCompass = findTreasureCompassWithFairyDust(player);
            if (treasureCompass != null) {
                // Check if compass has durability remaining
                if (treasureCompass.getDamage() >= treasureCompass.getMaxDamage()) return;

                // Check if there are nearby sparkles (fairy dust active condition)
                if (hasNearbySparkles(player)) {
                    // Damage the compass by 1
                    if (treasureCompass.getDamage() < treasureCompass.getMaxDamage()) {
                        treasureCompass.setDamage(treasureCompass.getDamage() + 1);
                    }
                }
            }
        }
    }

    /**
     * Finds a treasure compass with Fairy Dust enchantment in the player's inventory
     */
    private net.minecraft.item.ItemStack findTreasureCompassWithFairyDust(PlayerEntity player) {
        // Check main hand
        var mainHand = player.getMainHandStack();
        if (mainHand.getItem() == LootSparkle.TREASURE_COMPASS && TreasureCompassItem.hasFairyDust(mainHand)) {
            return mainHand;
        }

        // Check off hand
        var offHand = player.getOffHandStack();
        if (offHand.getItem() == LootSparkle.TREASURE_COMPASS && TreasureCompassItem.hasFairyDust(offHand)) {
            return offHand;
        }

        // Check hotbar slots (0-8)
        for (int slot = 0; slot < 9; slot++) {
            var hotbarStack = player.getInventory().getStack(slot);
            if (hotbarStack.getItem() == LootSparkle.TREASURE_COMPASS && TreasureCompassItem.hasFairyDust(hotbarStack)) {
                return hotbarStack;
            }
        }

        return null;
    }

    /**
     * Checks if the player has nearby sparkles
     */
    private boolean hasNearbySparkles(PlayerEntity player) {
        var playerSparkles = SparkleManager.getPlayerSparkles(player.getUuid());
        return !playerSparkles.isEmpty();
    }

    private void checkForNearbySparkles(PlayerEntity player) {
        // Only allow sparkle interaction if player has a Treasure Compass in inventory
        if (!hasCompassInInventory(player)) {
            return;
        }
        
        // Check if player is near any sparkles
        ClientSparkleManager.ClientSparkle nearbySparkle = findNearbySparkle(player);
        if (nearbySparkle != null) {
            // Send interaction packet to server
            ClientPlayNetworking.send(new SparkleNetworking.InteractSparklePacket(nearbySparkle.getSparkleId()));
        }
    }
    
    /**
     * Check if the player has a Treasure Compass in their inventory
     */
    private boolean hasCompassInInventory(PlayerEntity player) {
        // Check main hand
        if (player.getMainHandStack().getItem() == LootSparkle.TREASURE_COMPASS) {
            return true;
        }

        // Check off hand
        if (player.getOffHandStack().getItem() == LootSparkle.TREASURE_COMPASS) {
            return true;
        }

        // Check hotbar slots (0-8) - matches SparkleParticleRenderer visibility logic
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).getItem() == LootSparkle.TREASURE_COMPASS) {
                return true;
            }
        }

        return false;
    }

    private ClientSparkleManager.ClientSparkle findNearbySparkle(PlayerEntity player) {
        var playerSparkles = ClientSparkleManager.getPlayerSparkles(player.getUuid());

        Vec3d playerPos = player.getPos();
        final double INTERACTION_RADIUS = 3.0;

        for (ClientSparkleManager.ClientSparkle sparkle : playerSparkles) {
            BlockPos sparklePos = sparkle.getPosition();
            Vec3d sparkleVec = new Vec3d(sparklePos.getX() + 0.5, sparklePos.getY() + 0.5, sparklePos.getZ() + 0.5);

            double distance = playerPos.distanceTo(sparkleVec);
            if (distance <= INTERACTION_RADIUS) {
                return sparkle;
            }
        }

        return null;
    }
}