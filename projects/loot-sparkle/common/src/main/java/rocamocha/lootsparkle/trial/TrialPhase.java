package rocamocha.lootsparkle.trial;

import java.util.List;

/**
 * Represents a phase in a trial sparkle's mob spawning sequence
 */
public class TrialPhase {
    private final String type; // "emitter", "burst", "boss", "challenge", "puzzle"
    private final List<SpawnEntry> spawns;
    private final Rolls rolls;
    private final List<Challenge> challenges;
    private final Duration duration;
    private final Integer rate; // seconds between spawns for emitter phases
    private final Integer bonus; // seconds added to timer when mob killed for combat phases

    public TrialPhase(String type, List<SpawnEntry> spawns, Rolls rolls, Duration duration) {
        this(type, spawns, rolls, null, duration, null, null);
    }

    public TrialPhase(String type, List<Challenge> challenges, Duration duration) {
        this(type, null, null, challenges, duration, null, null);
    }

    public TrialPhase(String type, List<SpawnEntry> spawns, Rolls rolls, Duration duration, Integer rate, Integer bonus) {
        this(type, spawns, rolls, null, duration, rate, bonus);
    }

    private TrialPhase(String type, List<SpawnEntry> spawns, Rolls rolls, List<Challenge> challenges, Duration duration, Integer rate, Integer bonus) {
        this.type = type;
        this.spawns = spawns;
        this.rolls = rolls != null ? rolls : new Rolls(1);
        this.challenges = challenges;
        this.duration = duration;
        this.rate = rate;
        this.bonus = bonus;
    }

    public String getType() {
        return type;
    }

    public List<SpawnEntry> getSpawns() {
        return spawns;
    }

    public Rolls getRolls() {
        return rolls;
    }

    public List<Challenge> getChallenges() {
        return challenges;
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
