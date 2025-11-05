package rocamocha.lootsparkle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers custom enchantments by checking and logging at server world load time
 */
public class EnchantmentRegistration {
    private static final Logger LOGGER = LoggerFactory.getLogger("LootSparkle");
    private static boolean checked = false;

    public static void registerEventHandlers() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!checked) {
                checked = true;
                checkEnchantments(server);
            }
        });
    }

    /**
     * Attempt to directly register enchantments.
     * This is called early during mod init but registries may not be ready yet.
     */
    public static void directRegister() {
        LOGGER.warn("[LootSparkle] directRegister called - registries may not be ready yet");
        // This will be a no-op in early init, but the method exists for the call
    }

    private static void checkEnchantments(net.minecraft.server.MinecraftServer server) {
        LOGGER.warn("====== ENCHANTMENT CHECK ======");
        try {
            var enchantmentRegistry = server.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            
            var soulSight = enchantmentRegistry.get(Identifier.of("loot-sparkle", "soul_sight"));
            var fairyDust = enchantmentRegistry.get(Identifier.of("loot-sparkle", "fairy_dust"));
            var shimmerseek = enchantmentRegistry.get(Identifier.of("loot-sparkle", "shimmerseek"));
            
            LOGGER.warn("Soul Sight: {}", soulSight != null ? "FOUND" : "NOT FOUND");
            LOGGER.warn("Fairy Dust: {}", fairyDust != null ? "FOUND" : "NOT FOUND");
            LOGGER.warn("Shimmerseek: {}", shimmerseek != null ? "FOUND" : "NOT FOUND");
            
            var allLootSparkleEnchants = enchantmentRegistry.getIds().stream()
                .filter(id -> id.getNamespace().equals("loot-sparkle"))
                .toList();
            LOGGER.warn("Total loot-sparkle enchantments: {}", allLootSparkleEnchants.size());
            for (var id : allLootSparkleEnchants) {
                LOGGER.warn("  - {}", id);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking enchantments", e);
        }
        LOGGER.warn("==============================");
    }
}
