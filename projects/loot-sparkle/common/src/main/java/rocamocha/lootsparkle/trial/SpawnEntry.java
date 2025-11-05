package rocamocha.lootsparkle.trial;

import java.util.Map;

/**
 * Represents a spawn entry in a phase source
 */
public class SpawnEntry {
    private final String mobId;
    private final Map<String, String> armor; // slot -> itemId
    private final Map<String, Object> attributes; // attribute -> value
    private final String name;
    private final boolean boss;
    private final Integer weight; // null means guaranteed spawn
    private final Count count;

    public SpawnEntry(String mobId, Map<String, String> armor, Map<String, Object> attributes,
                     String name, boolean boss, Integer weight, Count count) {
        this.mobId = mobId;
        this.armor = armor;
        this.attributes = attributes;
        this.name = name;
        this.boss = boss;
        this.weight = weight;
        this.count = count != null ? count : new Count(1);
    }

    public String getMobId() {
        return mobId;
    }

    public Map<String, String> getArmor() {
        return armor;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public boolean isBoss() {
        return boss;
    }

    public boolean isWeighted() {
        return weight != null;
    }

    public int getWeight() {
        return weight != null ? weight : 0;
    }

    public Count getCount() {
        return count;
    }
}
