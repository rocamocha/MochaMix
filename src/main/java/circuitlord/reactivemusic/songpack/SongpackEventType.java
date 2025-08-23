package circuitlord.reactivemusic.songpack;
import java.util.HashMap;
import java.util.Map;

/**
 * Extensible replacement for the original SongpackEventType enum.
 * Allows registration of new event types at runtime.
 */
public final class SongpackEventType {

    private static final Map<String, SongpackEventType> REGISTRY = new HashMap<>();

    private final String id;

    private SongpackEventType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static SongpackEventType[] values() {
        return REGISTRY.values().toArray(new SongpackEventType[0]);
    }

    public static SongpackEventType register(String id) {
        return REGISTRY.computeIfAbsent(id, SongpackEventType::new);
    }

    public static SongpackEventType get(String id) {
        return REGISTRY.get(id);
    }

    public static SongpackEventType valueOf(String name) {
        if (name == null) return null;
        return get(name.toUpperCase(java.util.Locale.ROOT));
    }

    @Override
    public String toString() {
        return id;
    }

    // ---- Predefined values ----
    public static final SongpackEventType NONE = register("NONE");
    public static final SongpackEventType MAIN_MENU = register("MAIN_MENU");
    public static final SongpackEventType CREDITS = register("CREDITS");
    public static final SongpackEventType GENERIC = register("GENERIC");
}
