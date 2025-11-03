package rocamocha.lootsparkle;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Treasure Compass item that enables sparkle visibility and progression mechanics
 */
public class TreasureCompassItem extends Item {
    public TreasureCompassItem(Settings settings) {
        super(settings);
    }

    public int getMaxDamage() {
        return LootSparkleConfig.getTreasureCompassDurability();
    }

    public boolean isDamageable() {
        return getMaxDamage() > 0;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 15; // Similar to gold tools
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        tooltip.add(Text.translatable("item.loot-sparkle.treasure_compass.tooltip")
                .formatted(Formatting.GRAY));

        // Show durability if applicable
        if (isDamageable()) {
            int maxDamage = getMaxDamage();
            int damage = stack.getDamage();
            int remaining = maxDamage - damage;

            tooltip.add(Text.translatable("item.durability",
                    remaining, maxDamage).formatted(Formatting.GRAY));
        }
    }

    /**
     * Checks if the treasure compass has Fairy Dust enchantment
     */
    public static boolean hasFairyDust(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(enchantment -> enchantment.getIdAsString().equals("loot-sparkle:fairy_dust"));
    }

    /**
     * Checks if the treasure compass has Soul Sight enchantment
     */
    public static boolean hasSoulSight(ItemStack stack) {
        return stack.getEnchantments().getEnchantments().stream()
                .anyMatch(enchantment -> enchantment.getIdAsString().equals("loot-sparkle:soul_sight"));
    }
}