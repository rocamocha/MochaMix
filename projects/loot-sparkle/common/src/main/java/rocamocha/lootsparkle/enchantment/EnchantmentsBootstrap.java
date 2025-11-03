package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstrap class for registering custom enchantments.
 * This is called during Enchantments.bootstrap() through mixin injection.
 */
public class EnchantmentsBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger("LootSparkle");
    private static boolean bootstrapped = false;

    /**
     * Registers all custom enchantments
     */
    public static void bootstrap(Registerable<Enchantment> registry) {
        if (bootstrapped) {
            LOGGER.warn("[LootSparkle] Bootstrap already called, returning early");
            return;
        }

        LOGGER.error("[LootSparkle] ===== BOOTSTRAP CALLED! =====");
        bootstrapped = true;

        try {
            // Get the item registry lookup
            var itemLookup = registry.getRegistryLookup(RegistryKeys.ITEM);
            LOGGER.error("[LootSparkle] Got item lookup");

            // Register Soul Sight enchantment
            var soulSightKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "soul_sight"));
            var soulSightBuilder = SoulSightEnchantment.builder(itemLookup);
            registry.register(soulSightKey, soulSightBuilder.build(soulSightKey.getValue()));
            LOGGER.error("[LootSparkle] Registered soul_sight enchantment");

            // Register Fairy Dust enchantment
            var fairyDustKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "fairy_dust"));
            var fairyDustBuilder = FairyDustEnchantment.builder(itemLookup);
            registry.register(fairyDustKey, fairyDustBuilder.build(fairyDustKey.getValue()));
            LOGGER.error("[LootSparkle] Registered fairy_dust enchantment");

            LOGGER.error("[LootSparkle] ===== ALL CUSTOM ENCHANTMENTS REGISTERED SUCCESSFULLY! =====");
        } catch (Exception e) {
            LOGGER.error("[LootSparkle] ===== FAILED TO REGISTER ENCHANTMENTS =====", e);
            throw new RuntimeException("Failed to bootstrap custom enchantments", e);
        }
    }
}
