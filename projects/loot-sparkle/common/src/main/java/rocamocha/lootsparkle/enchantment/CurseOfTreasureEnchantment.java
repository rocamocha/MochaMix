package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Curse of Treasure - a curse applied to the Treasure Compass.
 * Used to gate Trial Sparkle spawning nearby.
 */
public class CurseOfTreasureEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        // Use durability-enchantable; JSON restricts to Treasure Compass and marks as curse
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                1, // Weight (rarity)
                1, // Max level
                Enchantment.constantCost(5), // Min cost (unused for curses typically)
                Enchantment.constantCost(25), // Max cost
                1, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND
            )
        );
    }
}
