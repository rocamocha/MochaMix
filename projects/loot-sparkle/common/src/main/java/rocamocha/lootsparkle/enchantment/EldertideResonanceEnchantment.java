package rocamocha.lootsparkle.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.tag.ItemTags;

/**
 * Eldertide Resonance enchantment - intended for Trident only.
 * Levels 1-3 provide increasing underwater guidance effects.
 */
public class EldertideResonanceEnchantment {

    public static Enchantment.Builder builder(RegistryEntryLookup<net.minecraft.item.Item> itemLookup) {
        // Use weapon-enchantable; JSON will restrict specifically to trident
        return Enchantment.builder(
            Enchantment.definition(
                itemLookup.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                2, // Weight (rarity)
                3, // Max level
                Enchantment.constantCost(18), // Min cost
                Enchantment.constantCost(48), // Max cost
                6, // Anvil cost
                net.minecraft.component.type.AttributeModifierSlot.MAINHAND
            )
        );
    }
}
