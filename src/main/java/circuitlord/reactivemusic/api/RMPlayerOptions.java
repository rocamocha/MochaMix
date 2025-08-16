package circuitlord.reactivemusic.api;

/** Builder-style options for creating RMPlayers. */
public final class RMPlayerOptions {
    // --- sensible defaults ---
    private String pluginNamespace = "default";
    private String group = "default";
    private boolean loop = false;
    private boolean autostart = true;
    private boolean linkToMinecraftVolumes = true;  // MASTER * MUSIC coupling
    private boolean quietWhenGamePaused = true;     // “quiet” layer when paused
    private int gainRefreshIntervalTicks = 10;      // 0/1 = every tick

    private float initialGainPercent = 1.0f;        // 0..1
    private float initialDuckPercent = 1.0f;        // 0..1

    private RMPlayerOptions() {}

    /** Start a new options object with defaults. */
    public static RMPlayerOptions create() { return new RMPlayerOptions(); }

    // --- fluent setters (all return this) ---
    public RMPlayerOptions namespace(String ns) { this.pluginNamespace = ns; return this; }
    public RMPlayerOptions group(String g) { this.group = g; return this; }
    public RMPlayerOptions loop(boolean v) { this.loop = v; return this; }
    public RMPlayerOptions autostart(boolean v) { this.autostart = v; return this; }

    public RMPlayerOptions linkToMinecraftVolumes(boolean v) { this.linkToMinecraftVolumes = v; return this; }
    public RMPlayerOptions quietWhenGamePaused(boolean v) { this.quietWhenGamePaused = v; return this; }
    public RMPlayerOptions gainRefreshIntervalTicks(int ticks) { this.gainRefreshIntervalTicks = Math.max(0, ticks); return this; }

    /** Initial volume [0..1]. */
    public RMPlayerOptions gain(float pct) { this.initialGainPercent = clamp01(pct); return this; }

    /** Initial per-player duck [0..1]. Multiplies with any group duck. */
    public RMPlayerOptions duck(float pct) { this.initialDuckPercent = clamp01(pct); return this; }

    // --- getters (used by the manager/impl) ---
    public String pluginNamespace() { return pluginNamespace; }
    public String group() { return group; }
    public boolean loop() { return loop; }
    public boolean autostart() { return autostart; }
    public boolean linkToMinecraftVolumes() { return linkToMinecraftVolumes; }
    public boolean quietWhenGamePaused() { return quietWhenGamePaused; }
    public int gainRefreshIntervalTicks() { return gainRefreshIntervalTicks; }
    public float initialGainPercent() { return initialGainPercent; }
    public float initialDuckPercent() { return initialDuckPercent; }

    private static float clamp01(float f) { return (f < 0f) ? 0f : (f > 1f ? 1f : f); }
}
