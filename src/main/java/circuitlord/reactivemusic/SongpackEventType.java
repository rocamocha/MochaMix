package circuitlord.reactivemusic;
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
    public static final SongpackEventType HOME = register("HOME");
    public static final SongpackEventType DAY = register("DAY");
    public static final SongpackEventType NIGHT = register("NIGHT");
    public static final SongpackEventType SUNRISE = register("SUNRISE");
    public static final SongpackEventType SUNSET = register("SUNSET");
    public static final SongpackEventType RAIN = register("RAIN");
    public static final SongpackEventType SNOW = register("SNOW");
    public static final SongpackEventType STORM = register("STORM");
    public static final SongpackEventType UNDERWATER = register("UNDERWATER");
    public static final SongpackEventType UNDERGROUND = register("UNDERGROUND");
    public static final SongpackEventType DEEP_UNDERGROUND = register("DEEP_UNDERGROUND");
    public static final SongpackEventType HIGH_UP = register("HIGH_UP");
    public static final SongpackEventType MINECART = register("MINECART");
    public static final SongpackEventType BOAT = register("BOAT");
    public static final SongpackEventType HORSE = register("HORSE");
    public static final SongpackEventType PIG = register("PIG");
    public static final SongpackEventType FISHING = register("FISHING");
    public static final SongpackEventType DYING = register("DYING");
    public static final SongpackEventType OVERWORLD = register("OVERWORLD");
    public static final SongpackEventType NETHER = register("NETHER");
    public static final SongpackEventType END = register("END");
    public static final SongpackEventType BOSS = register("BOSS");
    public static final SongpackEventType VILLAGE = register("VILLAGE");
    public static final SongpackEventType NEARBY_MOBS = register("NEARBY_MOBS");
    public static final SongpackEventType GENERIC = register("GENERIC");
}

