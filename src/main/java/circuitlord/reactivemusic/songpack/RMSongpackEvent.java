package circuitlord.reactivemusic.songpack;

import java.util.*;
import circuitlord.reactivemusic.api.SongpackEvent;

/**
 * This is coupled to the API's matching interface.
 * @see SongpackEvent
 */
public final class RMSongpackEvent implements SongpackEvent {
    private static final Map<String, RMSongpackEvent> REGISTRY = new HashMap<>();
    private final String id;

    private RMSongpackEvent(String id) { this.id = id; }
    public String getId() { return id; }

    @Override
    public Map<String, ? extends SongpackEvent> getMap() { return REGISTRY; }

    public static RMSongpackEvent[] values() {
        return REGISTRY.values().toArray(new RMSongpackEvent[0]);
    }
    public static RMSongpackEvent register(String id) {
        return REGISTRY.computeIfAbsent(id, RMSongpackEvent::new);
    }
    public static RMSongpackEvent get(String id) { return REGISTRY.get(id); }
    public static RMSongpackEvent valueOf(String name) {
        return name == null ? null : get(name.toUpperCase(Locale.ROOT));
    }

    public static final RMSongpackEvent NONE = register("NONE");
    public static final RMSongpackEvent MAIN_MENU = register("MAIN_MENU");
    public static final RMSongpackEvent CREDITS = register("CREDITS");
    public static final RMSongpackEvent GENERIC = register("GENERIC");
}