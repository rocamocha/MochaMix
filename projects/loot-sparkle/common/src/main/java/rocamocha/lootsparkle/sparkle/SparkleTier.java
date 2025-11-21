package rocamocha.lootsparkle.sparkle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import rocamocha.lootsparkle.enchantment.ShimmerseekWeightModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the tier/rarity of a sparkle, which determines loot quality and sources.
 * Notes:
 * - Trial tiers (levels 6-8) and Underwater tiers (levels 9-13) have weight 0 so normal tier rolls never pick them; they are spawned by dedicated systems.
 * - Underwater base loot tables use the namespace path: loot-sparkle:underwater/<tier>/basic
 * - Trial tiers are handled by the trial system and use different base paths.
 */
public enum SparkleTier {
    COMMON(0, "common", 65, List.of(
        "loot-sparkle:tiers/0_common/basic"
    )),
    UNCOMMON(1, "uncommon", 20, List.of(
        "loot-sparkle:tiers/1_uncommon/treasure",
        "loot-sparkle:tiers/1_uncommon/overworld"
    )),
    RARE(2, "rare", 12, List.of(
        "loot-sparkle:tiers/2_rare/valuable",
        "loot-sparkle:tiers/2_rare/special"
    )),
    EPIC(3, "epic", 3, List.of(
        "loot-sparkle:tiers/3_epic/legendary",
        "loot-sparkle:tiers/3_epic/enchanted"
    )),
    LEGENDARY(4, "legendary", 1, List.of(
        "loot-sparkle:tiers/4_legendary/mythical",
        "loot-sparkle:tiers/4_legendary/artifacts"
    )),
    DIVINE(5, "divine", 0, List.of(
        "loot-sparkle:tiers/5_divine/divine",
        "loot-sparkle:tiers/5_divine/celestial"
    )),

    
    // trial sparkle tiers
    CURSED(6, "cursed", 0, List.of(
        "loot-sparkle:trials/cursed/basic"
    )),
    BLIGHTED(7, "blighted", 0, List.of(
        "loot-sparkle:trials/blighted/treasure",
        "loot-sparkle:trials/blighted/special"
    )),
    DOOMED(8, "doomed", 0, List.of(
        "loot-sparkle:trials/doomed/legendary",
        "loot-sparkle:trials/doomed/mythical"
    )),

    // underwater sparkle tiers (weights set to 0 so they are not selected by normal tier rolls)
    DRIFTWOOD(9, "driftwood", 0, List.of(
        "loot-sparkle:underwater/driftwood/basic"
    )),
    KELP(10, "kelp", 0, List.of(
        "loot-sparkle:underwater/kelp/basic"
    )),
    CORAL(11, "coral", 0, List.of(
        "loot-sparkle:underwater/coral/basic"
    )),
    CAVERN(12, "cavern", 0, List.of(
        "loot-sparkle:underwater/cavern/basic"
    )),
    SEABED(13, "seabed", 0, List.of(
        "loot-sparkle:underwater/seabed/basic"
    ));

    private final int level;
    private final String name;
    private final int weight;
    private final List<String> lootTableIds;

    SparkleTier(int level, String name, int weight, List<String> lootTableIds) {
        this.level = level;
        this.name = name;
        this.weight = weight;
        this.lootTableIds = lootTableIds;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the numbered folder name for this tier (e.g., "0_common")
     */
    public String getNumberedName() {
        return level + "_" + name;
    }

    public int getWeight() {
        return weight;
    }

    public List<String> getLootTableIds() {
        return lootTableIds;
    }

    /**
     * Gets only the base loot table IDs for this tier (without biome/height modifiers)
     */
    public List<String> getBaseLootTableIds() {
        return lootTableIds;
    }

    /**
     * Selects a random tier based on weights, with optional modifiers from world context and player enchantments
     */
    public static SparkleTier selectRandomTier(World world, BlockPos position, PlayerEntity player) {
        int[] baseWeights = ShimmerseekWeightModifier.getBaseWeights();
        int[] weights = baseWeights.clone();

        // Apply Shimmerseek enchantment modifier if player has it
        if (player != null) {
            int[] shimmerseekInfo = getShimmerseekInfo(player);
            int totalLevel = shimmerseekInfo[0];
            int maxLevelCount = shimmerseekInfo[1];
            if (totalLevel > 0) {
                weights = ShimmerseekWeightModifier.modifyWeights(weights, totalLevel, maxLevelCount);
            }
        }

        int totalWeight = 0;

        // Calculate base weights
        for (int weight : weights) {
            totalWeight += weight;
        }

        // Apply world context modifiers
        int modifiedTotalWeight = totalWeight;
        SparkleTier[] tiers = values();

        // Biome-based modifiers
        Biome biome = world.getBiome(position).value();
        if (biome.getTemperature() < 0.3) { // Cold biomes
            // Increase rare/epic chances in cold biomes
            modifiedTotalWeight += 8; // Add more weight for better tiers
        } else if (biome.getTemperature() > 0.8) { // Hot biomes
            // Slightly increase uncommon chances in hot biomes
            modifiedTotalWeight += 3;
        }

        // Y-level based modifiers
        int y = position.getY();
        if (y < 0) { // Deep underground
            // Significantly increase rare/epic chances deep underground
            modifiedTotalWeight += 20;
        } else if (y > 100) { // High altitudes
            // Slightly increase uncommon chances at high altitudes
            modifiedTotalWeight += 4;
        }

        // Special conditions for legendary/divine tiers
        boolean canSpawnLegendary = false;
        boolean canSpawnDivine = false;

        // Legendary conditions: Very deep underground OR specific rare biomes
        if (y < -32 || (biome.getTemperature() < 0.1 && y < 32)) {
            canSpawnLegendary = true;
            modifiedTotalWeight += 5; // Add legendary weight
        }

        // Divine conditions: Extremely rare - only in the deepest depths or most extreme conditions
        if (y < -64 || (biome.getTemperature() < 0.05 && y < -32)) {
            canSpawnDivine = true;
            modifiedTotalWeight += 2; // Add divine weight
        }

        // Select tier based on modified weights
        int roll = world.getRandom().nextInt(modifiedTotalWeight);
        int currentWeight = 0;

        for (SparkleTier tier : tiers) {
            int tierWeight = weights[tier.getLevel()];

            // Add legendary weight if conditions met
            if (tier == LEGENDARY && canSpawnLegendary) {
                tierWeight += 2;
            }
            // Add divine weight if conditions met
            if (tier == DIVINE && canSpawnDivine) {
                tierWeight += 1;
            }

            currentWeight += tierWeight;
            if (roll < currentWeight) {
                return tier;
            }
        }

        // Fallback to common
        return COMMON;
    }

    /**
     * Gets the total Shimmerseek level (stacked) and count of pieces at max level
     * @return int[] where [0] is total level, [1] is count of pieces at max level
     */
    private static int[] getShimmerseekInfo(PlayerEntity player) {
        int totalLevel = 0;
        int maxLevelCount = 0;
        for (var stack : player.getArmorItems()) {
            if (!stack.isEmpty()) {
                var enchantments = stack.getEnchantments();
                for (var entry : enchantments.getEnchantments()) {
                    if (entry.getIdAsString().equals("loot-sparkle:shimmerseek")) {
                        int level = enchantments.getLevel(entry);
                        totalLevel += level;
                        if (level >= 12) {
                            maxLevelCount++;
                        }
                    }
                }
            }
        }
        return new int[]{totalLevel, maxLevelCount};
    }
    public List<String> getLootTableIds(World world, BlockPos position) {
        // Start with base loot tables (create mutable copy)
        List<String> tables = new ArrayList<>(lootTableIds);

        // Use different path structure for trial vs normal tiers
        String basePath = getCategory() == SparkleCategory.TRIAL ? "trial_tiers" : "tiers";

        // Add biome-specific tables
        Biome biome = world.getBiome(position).value();
        String biomeCategory = getBiomeCategory(biome);

        if (biomeCategory != null) {
            tables.add("loot-sparkle:" + basePath + "/" + getNumberedName() + "/biomes/" + biomeCategory);
        }

        // Add Y-level specific tables
        int y = position.getY();
        String heightCategory = getHeightCategory(y);

        if (heightCategory != null) {
            tables.add("loot-sparkle:" + basePath + "/" + getNumberedName() + "/heights/" + heightCategory);
        }

        return tables;
    }

    private String getBiomeCategory(Biome biome) {
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

    private String getHeightCategory(int y) {
        if (y < -32) {
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

    /**
     * Gets the category of this sparkle tier
     */
    public SparkleCategory getCategory() {
        if (this == CURSED || this == BLIGHTED || this == DOOMED) {
            return SparkleCategory.TRIAL;
        }
        if (this == DRIFTWOOD || this == KELP || this == CORAL || this == CAVERN || this == SEABED) {
            return SparkleCategory.UNDERWATER;
        }
        return SparkleCategory.NORMAL;
    }
}
