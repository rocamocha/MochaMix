package rocamocha.lootsparkle.trial;

/**
 * Represents a count value that can be either a fixed integer or a range with min/max
 */
public class Count {
    private final Integer fixed;
    private final Integer min;
    private final Integer max;

    public Count(int fixed) {
        this.fixed = fixed;
        this.min = null;
        this.max = null;
    }

    public Count(int min, int max) {
        this.fixed = null;
        this.min = min;
        this.max = max;
    }

    public boolean isFixed() {
        return fixed != null;
    }

    public int getFixed() {
        return fixed != null ? fixed : 1;
    }

    public int getMin() {
        return min != null ? min : 1;
    }

    public int getMax() {
        return max != null ? max : 1;
    }

    public int getValue(net.minecraft.util.math.random.Random random) {
        if (isFixed()) {
            return getFixed();
        } else {
            return min + random.nextInt(max - min + 1);
        }
    }
}
