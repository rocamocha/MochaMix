package rocamocha.lootsparkle.trial;

import java.util.List;

/**
 * Represents a phase source loaded from JSON
 */
public class PhaseSource {
    private final List<SpawnEntry> spawns;
    private final Rolls rolls;

    public PhaseSource(List<SpawnEntry> spawns, Rolls rolls) {
        this.spawns = spawns;
        this.rolls = rolls != null ? rolls : new Rolls(1);
    }

    public List<SpawnEntry> getSpawns() {
        return spawns;
    }

    public Rolls getRolls() {
        return rolls;
    }
}
