package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Soul Sight enchantment - only applicable to head armor pieces.
 * Allows players to see sparkle particles when wearing enchanted helmets.
 */
public class SoulSightEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.HEAD_ARMOR_ENCHANTABLE), // Only head armor
                2, // Weight (rarity)
                1, // Max level
                Enchantment.constantCost(15), // Min cost
                Enchantment.constantCost(65), // Max cost
                8, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.HEAD // Only head slot
            )
        );
    }
}