package rocamocha.lootsparkle;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * Registers and enforces enchantment restrictions for Soul Sight and Fairy Dust.
 * Handles enchanting table visibility and anvil restrictions.
 */
public class EnchantmentRestrictionHandler {
    
    /**
     * Register enchantment restriction handlers
     */
    public static void register() {
        // Enchantment restrictions are primarily handled through JSON configuration
        // Additional runtime validation can be added here if needed
    }

    /**
     * Checks if an enchantment can be applied to an item based on restrictions.
     */
    public static boolean canApplyEnchantment(RegistryEntry<Enchantment> enchantmentEntry, ItemStack stack) {
        String enchantmentId = enchantmentEntry.getIdAsString();
        
        // Soul Sight: Only on helmets and carved pumpkins
        if ("loot-sparkle:soul_sight".equals(enchantmentId)) {
            return isValidSoulSightItem(stack);
        }
        
        // Fairy Dust: Only on Treasure Compass
        if ("loot-sparkle:fairy_dust".equals(enchantmentId)) {
            return isValidFairyDustItem(stack);
        }
        
        return true;
    }

    /**
     * Checks if an item is valid for Soul Sight enchantment.
     * Valid items: All helmet types and carved pumpkins
     */
    public static boolean isValidSoulSightItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        var item = stack.getItem();
        
        // Check for all helmet types
        return item == Items.LEATHER_HELMET ||
               item == Items.CHAINMAIL_HELMET ||
               item == Items.IRON_HELMET ||
               item == Items.DIAMOND_HELMET ||
               item == Items.GOLDEN_HELMET ||
               item == Items.NETHERITE_HELMET ||
               item == Items.CARVED_PUMPKIN;
    }

    /**
     * Checks if an item is valid for Fairy Dust enchantment.
     * Valid items: Treasure Compass
     */
    public static boolean isValidFairyDustItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        return stack.getItem() == LootSparkle.TREASURE_COMPASS;
    }
}
