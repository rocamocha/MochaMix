package rocamocha.lootsparkle.trial;

/**
 * Represents a mob entry in a hostile phase
 */
public class MobEntry {
    private final String mobId;
    private final int weight;
    private final int count;
    private final int emit; // seconds for emitter phases

    public MobEntry(String mobId, int weight, int count, int emit) {
        this.mobId = mobId;
        this.weight = weight;
        this.count = count;
        this.emit = emit;
    }

    public String getMobId() {
        return mobId;
    }

    public int getWeight() {
        return weight;
    }

    public int getCount() {
        return count;
    }

    public int getEmit() {
        return emit;
    }

    @Override
    public String toString() {
        return "MobEntry{mobId='" + mobId + "', weight=" + weight + ", count=" + count + ", emit=" + emit + "}";
    }
}
