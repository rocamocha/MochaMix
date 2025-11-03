package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Fairy Dust enchantment - only applicable to treasure compass.
 * Enhances the treasure compass functionality.
 */
public class FairyDustEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        // For now, allow enchanting on any item that can be enchanted
        // We'll restrict this properly in the TreasureCompassItem class
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE), // Items that can be enchanted and have durability
                1, // Weight (rarity - very rare)
                1, // Max level
                Enchantment.constantCost(25), // Min cost
                Enchantment.constantCost(75), // Max cost
                8, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND // Main hand slot
            )
        );
    }
}