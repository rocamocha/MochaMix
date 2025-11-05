package rocamocha.lootsparkle.trial;

import java.util.List;

/**
 * Represents a phase in a hostile sparkle's combat sequence
 */
public class Phase {
    private final String type; // "emitter", "burst", "boss", "challenge", "puzzle"
    private final List<String> sources;
    private final Duration duration;
    private final Integer rate; // seconds between spawns for emitter phases
    private final Integer bonus; // seconds added to timer when mob killed for combat phases

    public Phase(String type, List<String> sources, Duration duration) {
        this(type, sources, duration, null, null);
    }

    public Phase(String type, List<String> sources, Duration duration, Integer rate, Integer bonus) {
        this.type = type;
        this.sources = sources;
        this.duration = duration;
        this.rate = rate;
        this.bonus = bonus;
    }

    public String getType() {
        return type;
    }

    public List<String> getSources() {
        return sources;
    }

    public Duration getDuration() {
        return duration;
    }

    public Integer getRate() {
        return rate;
    }

    public Integer getBonus() {
        return bonus;
    }

    public boolean isEmitter() {
        return "emitter".equals(type);
    }

    public boolean isBurst() {
        return "burst".equals(type);
    }

    public boolean isBoss() {
        return "boss".equals(type);
    }

    public boolean isChallenge() {
        return "challenge".equals(type);
    }

    public boolean isPuzzle() {
        return "puzzle".equals(type);
    }
}
