package circuitlord.reactivemusic.songpack;

import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 
/** Internal cache of last-known conditions per player. */
public final class SongpackEventState {
    private SongpackEventState() {}
    private static final Map<UUID, Map<SongpackEventType, Boolean>> LAST = new ConcurrentHashMap<>();

    public static void updateForPlayer(PlayerEntity player, Map<SongpackEventType, Boolean> conditions) {
        if (player == null || conditions == null) return;
        LAST.put(player.getUuid(), Collections.unmodifiableMap(new HashMap<>(conditions)));
    }

    public static Map<SongpackEventType, Boolean> snapshot(UUID playerId) {
        Map<SongpackEventType, Boolean> m = LAST.get(playerId);
        return (m != null) ? m : Collections.emptyMap();
    }

    public static void clear(UUID playerId) { LAST.remove(playerId); }
    public static void clearAll() { LAST.clear(); }
}
