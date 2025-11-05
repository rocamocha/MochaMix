package rocamocha.lootsparkle.trial;

/**
 * Represents a challenge configuration loaded from JSON
 */
public class Challenge {
    private final String type;
    private final boolean camo;
    private final int count;

    public Challenge(String type, boolean camo, int count) {
        this.type = type;
        this.camo = camo;
        this.count = count;
    }

    public String getType() {
        return type;
    }

    public boolean isCamo() {
        return camo;
    }

    public int getCount() {
        return count;
    }

    public boolean isTarget() {
        return "target".equals(type);
    }
}
