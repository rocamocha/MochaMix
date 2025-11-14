package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * Bootstrap class for registering custom enchantments.
 * This is called during Enchantments.bootstrap() through mixin injection.
 */
public class EnchantmentsBootstrap {
    private static boolean bootstrapped = false;

    /**
     * Registers all custom enchantments
     */
    public static void bootstrap(Registerable<Enchantment> registry) {
        if (bootstrapped) {
            return;
        }

        bootstrapped = true;

        try {
            // Get the item registry lookup
            var itemLookup = registry.getRegistryLookup(RegistryKeys.ITEM);

            // Register Soul Sight enchantment
            var soulSightKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "soul_sight"));
            var soulSightBuilder = SoulSightEnchantment.builder(itemLookup);
            registry.register(soulSightKey, soulSightBuilder.build(soulSightKey.getValue()));

            // Register Fairy Dust enchantment
            var fairyDustKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "fairy_dust"));
            var fairyDustBuilder = FairyDustEnchantment.builder(itemLookup);
            registry.register(fairyDustKey, fairyDustBuilder.build(fairyDustKey.getValue()));

            // Register Shimmerseek enchantment
            var shimmerseekKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "shimmerseek"));
            var shimmerseekBuilder = ShimmerseekEnchantment.builder(itemLookup);
            registry.register(shimmerseekKey, shimmerseekBuilder.build(shimmerseekKey.getValue()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap custom enchantments", e);
        }
    }
}
