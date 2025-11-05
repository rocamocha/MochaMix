package rocamocha.lootsparkle.trial;

import java.util.List;

/**
 * Represents a phase list loaded from a JSON file in phase_lists
 */
public class PhaseList {
    private final List<Phase> phases;

    public PhaseList(List<Phase> phases) {
        this.phases = phases;
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public Phase getFirstPhase() {
        return phases.isEmpty() ? null : phases.get(0);
    }
}
