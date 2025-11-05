package rocamocha.lootsparkle.enchantment;

import rocamocha.lootsparkle.SparkleTier;

/**
 * Weight modifier for Shimmerseek enchantment.
 * Modifies sparkle tier weights based on enchantment level.
 */
public class ShimmerseekWeightModifier {

    /**
     * Modifies the base weights for each tier based on Shimmerseek enchantment level.
     *
     * @param baseWeights Array of base weights for each tier (COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, DIVINE)
     * @param totalLevel The total stacked level of the Shimmerseek enchantment across all armor pieces
     * @param maxLevelCount The number of armor pieces that have the maximum level (12)
     * @return Modified weights array
     */
    public static int[] modifyWeights(int[] baseWeights, int totalLevel, int maxLevelCount) {
        if (totalLevel < 1) {
            return baseWeights.clone(); // No modification for invalid levels
        }

        int[] modifiedWeights = baseWeights.clone();

        // For each level: common weight decreases by 1
        modifiedWeights[SparkleTier.COMMON.getLevel()] = Math.max(0, modifiedWeights[SparkleTier.COMMON.getLevel()] - totalLevel);

        // For each level: uncommon weight increases by 1
        modifiedWeights[SparkleTier.UNCOMMON.getLevel()] += totalLevel;

        // For every two levels: rare weight increases by 1
        int rareIncrease = totalLevel / 2;
        modifiedWeights[SparkleTier.RARE.getLevel()] += rareIncrease;

        // For every four levels: epic weight increases by 1
        int epicIncrease = totalLevel / 4;
        modifiedWeights[SparkleTier.EPIC.getLevel()] += epicIncrease;

        // For levels six and twelve: legendary weight increases by 1
        int legendaryIncrease = 0;
        if (totalLevel >= 6) legendaryIncrease++;
        if (totalLevel >= 12) legendaryIncrease++;
        modifiedWeights[SparkleTier.LEGENDARY.getLevel()] += legendaryIncrease;

        // Divine weight increases by 1 for each armor piece at max level
        modifiedWeights[SparkleTier.DIVINE.getLevel()] += maxLevelCount;

        return modifiedWeights;
    }

    /**
     * Gets the base weights array for all tiers.
     */
    public static int[] getBaseWeights() {
        SparkleTier[] tiers = SparkleTier.values();
        int[] weights = new int[tiers.length];
        for (SparkleTier tier : tiers) {
            weights[tier.getLevel()] = tier.getWeight();
        }
        return weights;
    }
}