package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Diver's Crystal enchantment - intended for Treasure Compass only.
 * Max level 3; gating for underwater sparkle detection/spawning.
 */
public class DiversCrystalEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        // Use generic durability-enchantable for builder; JSON restricts to treasure compass
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.DURABILITY_ENCHANTABLE),
                2, // Weight (rarity)
                3, // Max level
                Enchantment.constantCost(12), // Min cost
                Enchantment.constantCost(40), // Max cost
                4, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND
            )
        );
    }
}
