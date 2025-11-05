package rocamocha.lootsparkle.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rocamocha.lootsparkle.sparkle.SparkleManager;
import rocamocha.lootsparkle.sparkle.SparkleTier;
import rocamocha.lootsparkle.loot.LootTableIntegration;
import rocamocha.lootsparkle.network.SparkleNetworking;
import rocamocha.lootsparkle.enchantment.EnchantmentRegistration;
import rocamocha.lootsparkle.item.TreasureCompassItem;

/**
 * Main entry point for Loot Sparkle mod
 *
 * This mod adds per-player instanced particle effects ("sparkles") at random block locations
 * that contain loot table-generated inventories. Players can interact with sparkles by
 * crouching near them to open an inventory GUI.
 */
public class LootSparkle implements ModInitializer {
    public static final String MOD_ID = "loot-sparkle";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Treasure Compass item
    public static final Item TREASURE_COMPASS = new TreasureCompassItem(
        new Item.Settings().maxDamage(100)
    );

    @Override
    public void onInitialize() {

        // Load configuration first
        LootSparkleConfig.loadConfig();

        // Register items
        registerItems();

        // Register sparkle entities/effects
        SparkleManager.initialize();

        // Register loot table integration
        LootTableIntegration.initialize();

        // Initialize networking
        SparkleNetworking.initialize();

        // Register event handler for enchantment registration verification
        EnchantmentRegistration.registerEventHandlers();

        // Register enchantments directly during initialization
        // This happens before the mixin fires, ensuring enchantments are available
        registerEnchantmentsDirect();

        // Register debug commands
        registerDebugCommands();

        // Register player disconnect handler to clean up orphaned entities
        registerPlayerDisconnectHandler();
    }

    /**
     * Registers items for the mod
     */
    private void registerItems() {
        // Register Treasure Compass
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "treasure_compass"), TREASURE_COMPASS);

        // Add to creative tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(TREASURE_COMPASS);
        });
    }

    /**
     * Registers debug commands for testing purposes
     */
    private void registerDebugCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("sparkle")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2
                .then(CommandManager.literal("expire")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        int expiredCount = SparkleManager.expireAllSparkles();
                        source.sendFeedback(() -> Text.literal("Expired " + expiredCount + " sparkles"), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("expire_hostile")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        int expiredCount = SparkleManager.expireAllHostileSparkles();
                        source.sendFeedback(() -> Text.literal("Expired " + expiredCount + " hostile sparkles"), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("ring")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getPlayer() == null) {
                            source.sendError(Text.literal("This command can only be run by a player"));
                            return 0;
                        }
                        
                        // Expire all existing sparkles
                        int expiredCount = SparkleManager.expireAllSparkles();
                        
                        // Spawn one sparkle of each tier in a ring around the player
                        net.minecraft.server.network.ServerPlayerEntity player = source.getPlayer();
                        net.minecraft.server.world.ServerWorld world = source.getWorld();
                        net.minecraft.util.math.Vec3d playerPos = player.getPos();
                        
                        SparkleTier[] tiers = SparkleTier.values();
                        double radius = 7.0; // Distance from player
                        double angleStep = (2.0 * Math.PI) / tiers.length;
                        
                        for (int i = 0; i < tiers.length; i++) {
                            double angle = angleStep * i;
                            double x = playerPos.x + radius * Math.cos(angle);
                            double z = playerPos.z + radius * Math.sin(angle);
                            int y = player.getBlockY();
                            
                            // Find a valid spawn position at this location
                            net.minecraft.util.math.BlockPos spawnPos = new net.minecraft.util.math.BlockPos(
                                (int) Math.round(x),
                                y,
                                (int) Math.round(z)
                            );
                            
                            // Try to find a valid air block with solid ground
                            net.minecraft.util.math.BlockPos validPos = null;
                            for (int yOffset = 0; yOffset <= 5; yOffset++) {
                                net.minecraft.util.math.BlockPos checkPos = spawnPos.up(yOffset);
                                if (world.getBlockState(checkPos).isAir() && 
                                    world.getBlockState(checkPos.down()).isSolidBlock(world, checkPos.down())) {
                                    validPos = checkPos;
                                    break;
                                }
                            }
                            
                            // If no valid position found in range, use the original position
                            if (validPos == null) {
                                validPos = spawnPos;
                            }
                            
                            SparkleManager.spawnSparkleOfTierForPlayer(player.getUuid(), world, validPos, tiers[i]);
                        }
                        
                        source.sendFeedback(() -> Text.literal("Expired " + expiredCount + " sparkles and spawned a ring of " + tiers.length + " sparkles (one per tier)"), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("spawn_hostile")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getPlayer() == null) {
                            source.sendError(Text.literal("This command can only be run by a player"));
                            return 0;
                        }
                        
                        net.minecraft.server.network.ServerPlayerEntity player = source.getPlayer();
                        net.minecraft.server.world.ServerWorld world = source.getWorld();
                        
                        // Spawn a random hostile sparkle
                        SparkleManager.spawnHostileSparkle(world, player.getBlockPos());
                        
                        source.sendFeedback(() -> Text.literal("Spawned a hostile sparkle near your position"), true);
                        return 1;
                    })
                )
                .then(CommandManager.literal("list_enchantments")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        var enchantmentRegistry = source.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                        var lootSparkleEnchantments = enchantmentRegistry.getIds().stream()
                            .filter(key -> key.getNamespace().equals("loot-sparkle"))
                            .map(Identifier::toString)
                            .toList();
                        source.sendFeedback(() -> Text.literal("Loot Sparkle enchantments: " + lootSparkleEnchantments), false);
                        return 1;
                    })
                )
                .then(CommandManager.literal("test_enchantments")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        var enchantmentRegistry = source.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);

                        // Check if our enchantments are registered
                        var soulSight = enchantmentRegistry.get(Identifier.of("loot-sparkle", "soul_sight"));
                        var fairyDust = enchantmentRegistry.get(Identifier.of("loot-sparkle", "fairy_dust"));
                        var shimmerseek = enchantmentRegistry.get(Identifier.of("loot-sparkle", "shimmerseek"));

                        StringBuilder message = new StringBuilder("Enchantment status:\n");
                        message.append("Soul Sight: ").append(soulSight != null ? "REGISTERED" : "NOT FOUND").append("\n");
                        message.append("Fairy Dust: ").append(fairyDust != null ? "REGISTERED" : "NOT FOUND").append("\n");
                        message.append("Shimmerseek: ").append(shimmerseek != null ? "REGISTERED" : "NOT FOUND").append("\n");

                        // List all loot-sparkle enchantments
                        var allEnchantments = enchantmentRegistry.getIds().stream()
                            .filter(key -> key.getNamespace().equals("loot-sparkle"))
                            .map(Identifier::toString)
                            .toList();
                        message.append("All loot-sparkle enchantments: ").append(allEnchantments);

                        source.sendFeedback(() -> Text.literal(message.toString()), false);
                        return 1;
                    })
                )
                .then(CommandManager.literal("show_weights")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getPlayer() == null) {
                            source.sendError(Text.literal("This command can only be run by a player"));
                            return 0;
                        }

                        net.minecraft.server.network.ServerPlayerEntity player = source.getPlayer();
                        StringBuilder message = new StringBuilder("Sparkle Tier Weights:\n");

                        // Show base weights
                        message.append("Base weights:\n");
                        SparkleTier[] tiers = SparkleTier.values();
                        for (SparkleTier tier : tiers) {
                            message.append("  ").append(tier.getName()).append(": ").append(tier.getWeight()).append("\n");
                        }

                        // Show modified weights if player has Shimmerseek
                        int[] shimmerseekInfo = getShimmerseekInfo(player);
                        int shimmerseekLevel = shimmerseekInfo[0];
                        int maxLevelCount = shimmerseekInfo[1];
                        if (shimmerseekLevel > 0) {
                            message.append("\nModified weights (Shimmerseek total level ").append(shimmerseekLevel);
                            if (maxLevelCount > 0) {
                                message.append(", ").append(maxLevelCount).append(" max level piece");
                                if (maxLevelCount > 1) message.append("s");
                            }
                            message.append("):\n");
                            int[] baseWeights = rocamocha.lootsparkle.enchantment.ShimmerseekWeightModifier.getBaseWeights();
                            int[] modifiedWeights = rocamocha.lootsparkle.enchantment.ShimmerseekWeightModifier.modifyWeights(baseWeights, shimmerseekLevel, maxLevelCount);

                            for (SparkleTier tier : tiers) {
                                int baseWeight = tier.getWeight();
                                int modifiedWeight = modifiedWeights[tier.getLevel()];
                                int difference = modifiedWeight - baseWeight;
                                String diffStr = difference > 0 ? "+" + difference : (difference < 0 ? "" + difference : "");
                                message.append("  ").append(tier.getName()).append(": ").append(modifiedWeight);
                                if (!diffStr.isEmpty()) {
                                    message.append(" (").append(diffStr).append(")");
                                }
                                message.append("\n");
                            }
                        } else {
                            message.append("\nNo Shimmerseek enchantment detected.");
                        }

                        source.sendFeedback(() -> Text.literal(message.toString()), false);
                        return 1;
                    })
                )
            );
        });
    }

    /**
     * Gets the total Shimmerseek level (stacked) and count of pieces at max level
     * @return int[] where [0] is total level, [1] is count of pieces at max level
     */
    private static int[] getShimmerseekInfo(net.minecraft.server.network.ServerPlayerEntity player) {
        int totalLevel = 0;
        int maxLevelCount = 0;
        for (var stack : player.getArmorItems()) {
            if (!stack.isEmpty()) {
                var enchantments = stack.getEnchantments();
                for (var entry : enchantments.getEnchantments()) {
                    if (entry.getIdAsString().equals("loot-sparkle:shimmerseek")) {
                        int level = enchantments.getLevel(entry);
                        totalLevel += level;
                        if (level >= 12) {
                            maxLevelCount++;
                        }
                    }
                }
            }
        }
        return new int[]{totalLevel, maxLevelCount};
    }

    /**
     * Directly registers enchantments via EnchantmentsBootstrap.
     * This is called during mod initialization to ensure enchantments are available
     * even if the mixin doesn't fire during the bootstrap phase.
     */
    private void registerEnchantmentsDirect() {
        try {
            // This will be called before server startup, but we can at least log if it works
            EnchantmentRegistration.directRegister();
        } catch (Exception e) {
            // This is expected to fail early in init, will work later via mixin/events
        }
    }

    /**
     * Registers a handler for player disconnect events
     * to clean up any orphaned entities or data
     */
    private void registerPlayerDisconnectHandler() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Clean up all entities from this player's sparkles when they disconnect
            SparkleManager.onPlayerDisconnect(handler.getPlayer().getUuid());
        });
    }
}
