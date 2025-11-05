package rocamocha.lootsparkle.trial;

/**
 * Represents a duration for a phase
 */
public class Duration {
    private final int value; // seconds
    private final String type; // "advance", "limit", "survive"

    public Duration(int value, String type) {
        this.value = value;
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public boolean isAdvance() {
        return "advance".equals(type);
    }

    public boolean isLimit() {
        return "limit".equals(type);
    }

    public boolean isSurvive() {
        return "survive".equals(type);
    }
}
