package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Shimmerseek enchantment - increases chances of higher tier sparkles.
 * Can be applied to armor pieces.
 */
public class ShimmerseekEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.ARMOR_ENCHANTABLE), // Any armor
                3, // Weight (rarity - uncommon)
                12, // Max level
                Enchantment.constantCost(10), // Min cost
                Enchantment.constantCost(50), // Max cost
                8, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.ARMOR // Armor slots
            )
        );
    }
}