package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;

/**
 * Registers custom enchantments during data generation
 * This class is used for registering enchantments in the bootstrap phase
 */
public class EnchantmentBootstrap {

    public static void bootstrap(Registerable<Enchantment> registry) {
        
        try {
            RegistryEntryLookup<net.minecraft.item.Item> itemLookup = registry.getRegistryLookup(RegistryKeys.ITEM);
            
            // Register Soul Sight
            register(
                registry,
                RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "soul_sight")),
                SoulSightEnchantment.builder(itemLookup)
            );
            
            // Register Fairy Dust
            register(
                registry,
                RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "fairy_dust")),
                FairyDustEnchantment.builder(itemLookup)
            );
            
            // Register Shimmerseek
            register(
                registry,
                RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "shimmerseek")),
                ShimmerseekEnchantment.builder(itemLookup)
            );
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}
