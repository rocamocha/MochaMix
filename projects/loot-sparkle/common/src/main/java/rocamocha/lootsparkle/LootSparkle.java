package rocamocha.lootsparkle;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LOGGER.info("Initializing Loot Sparkle...");

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

        LOGGER.info("Loot Sparkle initialized successfully!");
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

                        StringBuilder message = new StringBuilder("Enchantment status:\n");
                        message.append("Soul Sight: ").append(soulSight != null ? "REGISTERED" : "NOT FOUND").append("\n");
                        message.append("Fairy Dust: ").append(fairyDust != null ? "REGISTERED" : "NOT FOUND").append("\n");

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
                .then(CommandManager.literal("give_soul_sight_book")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getPlayer() != null) {
                            giveEnchantedBook(source.getPlayer(), "loot-sparkle:soul_sight");
                            source.sendFeedback(() -> Text.literal("Attempted to give Soul Sight enchanted book"), true);
                        } else {
                            source.sendError(Text.literal("This command can only be run by a player"));
                        }
                        return 1;
                    })
                )
                .then(CommandManager.literal("give_fairy_dust_book")
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        if (source.getPlayer() != null) {
                            giveEnchantedBook(source.getPlayer(), "loot-sparkle:fairy_dust");
                            source.sendFeedback(() -> Text.literal("Attempted to give Fairy Dust enchanted book"), true);
                        } else {
                            source.sendError(Text.literal("This command can only be run by a player"));
                        }
                        return 1;
                    })
                )
            );
        });
    }

    /**
     * Gives an enchanted book with the specified enchantment to the player
     */
    private void giveEnchantedBook(net.minecraft.server.network.ServerPlayerEntity player, String enchantmentId) {
        ItemStack bookStack = new ItemStack(Items.ENCHANTED_BOOK);

        try {
            // Get the enchantment from the registry
            Identifier id = Identifier.of(enchantmentId);
            var enchantmentRegistry = player.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            Enchantment enchantment = enchantmentRegistry.get(id);

            if (enchantment != null) {
                // Create ItemEnchantmentsComponent with the enchantment using builder
                var builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                // Get the registry entry for the enchantment using the ID
                var registryEntry = enchantmentRegistry.getEntry(id);
                if (registryEntry.isPresent()) {
                    builder.add(registryEntry.get(), 1);
                    ItemEnchantmentsComponent component = builder.build();
                    bookStack.set(DataComponentTypes.STORED_ENCHANTMENTS, component);
                    LOGGER.info("Successfully created enchanted book with enchantment {}", enchantmentId);
                } else {
                    LOGGER.error("Could not get registry entry for enchantment {}", enchantmentId);
                }
            } else {
                LOGGER.error("Enchantment {} not found in registry! Available loot-sparkle enchantments: {}",
                    enchantmentId,
                    enchantmentRegistry.getIds().stream()
                        .filter(key -> key.getNamespace().equals("loot-sparkle"))
                        .map(Identifier::toString)
                        .toList());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create enchanted book for {}: {}", enchantmentId, e.getMessage(), e);
        }

        boolean addedToInventory = player.getInventory().insertStack(bookStack);

        if (!addedToInventory) {
            // If inventory is full, drop it on the ground
            ItemEntity itemEntity = new ItemEntity(player.getWorld(), player.getX(), player.getY(), player.getZ(), bookStack);
            player.getWorld().spawnEntity(itemEntity);
        }
    }

    /**
     * Directly registers enchantments via EnchantmentsBootstrap.
     * This is called during mod initialization to ensure enchantments are available
     * even if the mixin doesn't fire during the bootstrap phase.
     */
    private void registerEnchantmentsDirect() {
        LOGGER.warn("[LootSparkle] Attempting direct enchantment registration during init");
        try {
            // This will be called before server startup, but we can at least log if it works
            EnchantmentRegistration.directRegister();
        } catch (Exception e) {
            LOGGER.error("[LootSparkle] Failed in direct registration attempt: {}", e.getMessage());
            // This is expected to fail early in init, will work later via mixin/events
        }
    }
}