package rocamocha.lootsparkle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import rocamocha.lootsparkle.core.LootSparkleConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Treasure Compass item that enables sparkle visibility and repair mechanics
 *
 * Features:
 * - Points to nearest sparkles when enchanted with Fairy Dust
 * - Can be repaired using various materials to restore durability
 * - Supports enchantments like Fairy Dust and Soul Sight
 */
public class TreasureCompassItem extends Item {
    // Repair material ranges (min, max essence restored)
    private static final Map<Item, RepairMaterial> REPAIR_MATERIALS = new HashMap<>();
    
    static {
        REPAIR_MATERIALS.put(Items.AMETHYST_SHARD, new RepairMaterial(30, 90));
        REPAIR_MATERIALS.put(Items.REDSTONE, new RepairMaterial(15, 45));
        REPAIR_MATERIALS.put(Items.GLOWSTONE_DUST, new RepairMaterial(30, 60));
        REPAIR_MATERIALS.put(Items.EMERALD, new RepairMaterial(45, 105));
        REPAIR_MATERIALS.put(Items.DIAMOND, new RepairMaterial(240, 480));
        REPAIR_MATERIALS.put(Items.QUARTZ, new RepairMaterial(20, 40));
        REPAIR_MATERIALS.put(Items.LAPIS_LAZULI, new RepairMaterial(60, 90));
        REPAIR_MATERIALS.put(Items.CRYING_OBSIDIAN, new RepairMaterial(-1, -1)); // Full repair
    }
    
    private static class RepairMaterial {
        final int minRestore;
        final int maxRestore;
        
        RepairMaterial(int min, int max) {
            this.minRestore = min;
            this.maxRestore = max;
        }
    }
    
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // Only process main hand usage
        if (hand != Hand.MAIN_HAND) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }

        ItemStack compassStack = user.getStackInHand(hand);
        ItemStack offHandStack = user.getOffHandStack();

        // Check if compass needs repair
        if (compassStack.getDamage() == 0) {
            return TypedActionResult.pass(compassStack);
        }

        // Check if offhand has a valid repair material
        if (offHandStack.isEmpty()) {
            return TypedActionResult.pass(compassStack);
        }

        RepairMaterial repairMaterial = REPAIR_MATERIALS.get(offHandStack.getItem());
        if (repairMaterial == null) {
            return TypedActionResult.pass(compassStack);
        }

        // Perform repair on server side
        if (!world.isClient()) {
            int currentDamage = compassStack.getDamage();
            int essenceRestored;

            // Check for crying obsidian (full repair)
            if (offHandStack.getItem() == Items.CRYING_OBSIDIAN) {
                essenceRestored = currentDamage;
            } else {
                // Calculate random essence restoration
                essenceRestored = repairMaterial.minRestore + 
                    world.getRandom().nextInt(repairMaterial.maxRestore - repairMaterial.minRestore + 1);
            }

            // Apply repair (reduce damage)
            int newDamage = Math.max(0, currentDamage - essenceRestored);
            compassStack.setDamage(newDamage);

            // Consume one item from offhand
            if (!user.isCreative()) {
                offHandStack.decrement(1);
            }

            // Play sound and spawn particles
            ServerWorld serverWorld = (ServerWorld) world;
            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), 
                SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 
                1.0f, 1.0f + world.getRandom().nextFloat() * 0.4f);

            // Spawn particles around the player
            for (int i = 0; i < 15; i++) {
                double offsetX = (world.getRandom().nextDouble() - 0.5) * 1.5;
                double offsetY = world.getRandom().nextDouble() * 1.5;
                double offsetZ = (world.getRandom().nextDouble() - 0.5) * 1.5;
                
                serverWorld.spawnParticles(ParticleTypes.ENCHANT,
                    user.getX() + offsetX,
                    user.getY() + offsetY,
                    user.getZ() + offsetZ,
                    1, 0.0, 0.1, 0.0, 0.05);
            }

            // Send feedback message
            int actualRestored = currentDamage - newDamage;
            user.sendMessage(Text.translatable("item.loot-sparkle.treasure_compass.repair_success", 
                actualRestored).formatted(Formatting.GREEN), true);
        }

        return TypedActionResult.success(compassStack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        tooltip.add(Text.translatable("item.loot-sparkle.treasure_compass.tooltip")
                .formatted(Formatting.GRAY));

        // Show essence if applicable
        if (isDamageable()) {
            int maxDamage = getMaxDamage();
            int damage = stack.getDamage();
            int remaining = maxDamage - damage;

            tooltip.add(Text.translatable("item.loot-sparkle.treasure_compass.essence",
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
