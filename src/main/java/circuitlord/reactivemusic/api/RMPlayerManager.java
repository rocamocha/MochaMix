package circuitlord.reactivemusic.api;

public interface RMPlayerManager {
    // Factory
    RMPlayer create(String id, RMPlayerOptions opts);   // throws if id already exists

    // Lookup / control
    RMPlayer get(String id);                            // null if missing
    java.util.Collection<RMPlayer> getAll();
    java.util.Collection<RMPlayer> getByGroup(String group);

    // Cross-player ducking (optional, simple + predictable)
    void setGroupDuck(String group, float percent);     // multiplies each player’s duck layer
    float getGroupDuck(String group);

    // Lifecycle
    void closeAllForPlugin(String pluginNamespace);
    void closeAll();                                    // on shutdown

    void tick();
}