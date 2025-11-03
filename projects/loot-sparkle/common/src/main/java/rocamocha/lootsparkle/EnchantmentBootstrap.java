package rocamocha.lootsparkle;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;

import rocamocha.lootsparkle.enchantment.FairyDustEnchantment;
import rocamocha.lootsparkle.enchantment.SoulSightEnchantment;

/**
 * Registers custom enchantments during data generation
 * This class is used for registering enchantments in the bootstrap phase
 */
public class EnchantmentBootstrap {

    public static void bootstrap(Registerable<Enchantment> registry) {
        System.err.println("[LootSparkle] EnchantmentBootstrap.bootstrap() called!");
        
        try {
            RegistryEntryLookup<net.minecraft.item.Item> itemLookup = registry.getRegistryLookup(RegistryKeys.ITEM);
            
            // Register Soul Sight
            register(
                registry,
                RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "soul_sight")),
                SoulSightEnchantment.builder(itemLookup)
            );
            System.err.println("[LootSparkle] Registered soul_sight");
            
            // Register Fairy Dust
            register(
                registry,
                RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "fairy_dust")),
                FairyDustEnchantment.builder(itemLookup)
            );
            System.err.println("[LootSparkle] Registered fairy_dust");
            
        } catch (Exception e) {
            System.err.println("[LootSparkle] Error registering enchantments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void register(Registerable<Enchantment> registry, RegistryKey<Enchantment> key, Enchantment.Builder builder) {
        registry.register(key, builder.build(key.getValue()));
    }
}