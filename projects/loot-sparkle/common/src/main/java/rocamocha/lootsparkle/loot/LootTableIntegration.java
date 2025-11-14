package rocamocha.lootsparkle.loot;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resource.ResourceManager;

import rocamocha.lootsparkle.sparkle.SparkleTier;
import rocamocha.lootsparkle.core.LootSparkle;

/**
 * Handles loot table integration for sparkle inventories
 *
 * Manages:
 * - Loot table registration
 * - Tier-based loot generation
 * - Biome and Y-level based loot table selection
 * - Datapack integration
 */
public class LootTableIntegration {
    // Base loot table identifiers
    public static final Identifier COMMON_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/0_common");
    public static final Identifier UNCOMMON_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/1_uncommon");
    public static final Identifier RARE_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/2_rare");
    public static final Identifier EPIC_LOOT_TABLE = Identifier.of(LootSparkle.MOD_ID, "tiers/3_epic");

    public static void initialize() {
        // Loot tables will be loaded directly from mod resources
    }

    /**
     * Generates loot for a sparkle's inventory based on tier and world context
     */
    public static void generateLootForSparkle(SimpleInventory inventory, SparkleTier tier, World world, BlockPos position) {

        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }


        try {
            
            // Get base loot table IDs for this tier
            List<String> baseLootTableIds = tier.getBaseLootTableIds();
            
            // Select one random base table
            List<String> selectedTableIds = new ArrayList<>();
            if (!baseLootTableIds.isEmpty()) {
                String randomBaseTable = baseLootTableIds.get(serverWorld.getRandom().nextInt(baseLootTableIds.size()));
                selectedTableIds.add(randomBaseTable);
            }
            
            // Add biome-specific table if applicable and exists
            Biome biome = world.getBiome(position).value();
            String biomeCategory = getBiomeCategory(biome);
            if (biomeCategory != null) {
                String biomeTableId = "loot-sparkle:tiers/" + tier.getNumberedName() + "/biomes/" + biomeCategory;
                Identifier biomeResourceId = Identifier.of("loot-sparkle", "loot_tables/" + tier.getNumberedName() + "/biomes/" + biomeCategory + ".json");
                if (serverWorld.getServer().getResourceManager().getResource(biomeResourceId).isPresent()) {
                    selectedTableIds.add(biomeTableId);
                }
            }
            
            // Add height-specific table if applicable and exists
            int y = position.getY();
            String heightCategory = getHeightCategory(y);
            if (heightCategory != null) {
                String heightTableId = "loot-sparkle:tiers/" + tier.getNumberedName() + "/heights/" + heightCategory;
                Identifier heightResourceId = Identifier.of("loot-sparkle", "loot_tables/" + tier.getNumberedName() + "/heights/" + heightCategory + ".json");
                if (serverWorld.getServer().getResourceManager().getResource(heightResourceId).isPresent()) {
                    selectedTableIds.add(heightTableId);
                }
            }
            
            List<LootTable> lootTables = new ArrayList<>();

            // Load loot tables directly from mod resources
            ResourceManager resourceManager = serverWorld.getServer().getResourceManager();
            for (String tableId : selectedTableIds) {
                try {
                    Identifier identifier = Identifier.of(tableId);
                    Identifier resourceId = Identifier.of(identifier.getNamespace(), "loot_tables/" + identifier.getPath() + ".json");
                    var resource = resourceManager.getResource(resourceId);
                    if (resource.isPresent()) {
                        var reader = resource.get().getReader();
                        var jsonElement = JsonParser.parseReader(reader);
                        var lootTableResult = LootTable.CODEC.parse(JsonOps.INSTANCE, jsonElement);
                        var lootTable = lootTableResult.result().orElse(LootTable.EMPTY);
                        if (lootTable != LootTable.EMPTY) {
                            lootTables.add(lootTable);
                        } else {
                        }
                        reader.close();
                    } else {
                    }
                } catch (Exception e) {
                }
            }            // If no loot tables found, fall back to basic generation
            if (lootTables.isEmpty()) {
                generateFallbackLoot(inventory, tier);
                return;
            }

            // Generate loot from all selected tables
            List<net.minecraft.item.ItemStack> allLootItems = new ArrayList<>();
            for (LootTable lootTable : lootTables) {
                try {
                    // Create loot context
                    LootContextParameterSet parameterSet = new LootContextParameterSet.Builder(serverWorld)
                        .add(LootContextParameters.ORIGIN, position.toCenterPos())
                        .build(net.minecraft.loot.context.LootContextTypes.CHEST);

                    // Generate loot and collect items
                    var random = net.minecraft.util.math.random.Random.create();
                    ObjectArrayList<net.minecraft.item.ItemStack> lootItems = lootTable.generateLoot(parameterSet, random);
                    for (var itemStack : lootItems) {
                        allLootItems.add(itemStack);
                    }
                } catch (Exception e) {
                }
            }

            // Place items in random slots for a more natural chest-like distribution
            if (!allLootItems.isEmpty()) {
                List<Integer> availableSlots = new ArrayList<>();
                for (int i = 0; i < inventory.size(); i++) {
                    availableSlots.add(i);
                }
                Collections.shuffle(availableSlots);

                int slotIndex = 0;
                for (var itemStack : allLootItems) {
                    if (slotIndex < availableSlots.size()) {
                        int randomSlot = availableSlots.get(slotIndex);
                        inventory.setStack(randomSlot, itemStack);
                        slotIndex++;
                    }
                }
            }


        } catch (Exception e) {
            // Fallback to basic loot generation
            generateFallbackLoot(inventory, tier);
        }
    }

    /**
     * Generates basic fallback loot when loot tables are not available
     */
    private static void generateFallbackLoot(SimpleInventory inventory, SparkleTier tier) {

        try {
            List<net.minecraft.item.ItemStack> fallbackItems = new ArrayList<>();

            // Basic tier-based loot
            switch (tier) {
                case COMMON:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.COAL, 3));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.STICK, 2));
                    break;
                case UNCOMMON:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_INGOT, 2));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_NUGGET, 4));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BREAD, 2));
                    break;
                case RARE:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.EMERALD, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.GOLD_INGOT, 3));
                    break;
                case EPIC:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, 2));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_SCRAP, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK, 1));
                    break;
                case LEGENDARY:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_INGOT, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.TOTEM_OF_UNDYING, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_GOLDEN_APPLE, 1));
                    break;
                case DIVINE:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DRAGON_EGG, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHER_STAR, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BEACON, 1));
                    break;
                case CURSED:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.ROTTEN_FLESH, 5));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.BONE, 3));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.STRING, 2));
                    break;
                case BLIGHTED:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.SPIDER_EYE, 4));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.FERMENTED_SPIDER_EYE, 2));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.POISONOUS_POTATO, 3));
                    break;
                case DOOMED:
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.WITHER_SKELETON_SKULL, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHER_STAR, 1));
                    fallbackItems.add(new net.minecraft.item.ItemStack(net.minecraft.item.Items.DRAGON_EGG, 1));
                    break;
            }

            // Place items in random slots
            if (!fallbackItems.isEmpty()) {
                List<Integer> availableSlots = new ArrayList<>();
                for (int i = 0; i < inventory.size(); i++) {
                    availableSlots.add(i);
                }
                Collections.shuffle(availableSlots);

                int slotIndex = 0;
                for (var itemStack : fallbackItems) {
                    if (slotIndex < availableSlots.size()) {
                        int randomSlot = availableSlots.get(slotIndex);
                        inventory.setStack(randomSlot, itemStack);
                        slotIndex++;
                    }
                }
            }

        } catch (Exception e) {
        }
    }

    private static String getBiomeCategory(Biome biome) {
        // Temperature-based categories
        if (biome.getTemperature() < 0.1) {
            return "frozen";
        } else if (biome.getTemperature() < 0.3) {
            return "cold";
        } else if (biome.getTemperature() > 0.9) {
            return "hot";
        } else if (biome.getTemperature() > 0.7) {
            return "warm";
        }

        // For now, skip precipitation-based categories as the API might be different
        // TODO: Add precipitation-based categories when API is clarified

        return null; // No specific category
    }

    private static String getHeightCategory(int y) {
        if (y < -80) {
            return "deep_caverns";
        } else if (y < 0) {
            return "underground";
        } else if (y > 128) {
            return "sky_high";
        } else if (y > 64) {
            return "mountains";
        }

        return null; // Surface level
    }
}
