package circuitlord.reactivemusic.api;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.Map;

import circuitlord.reactivemusic.songpack.SongpackEventState;
import circuitlord.reactivemusic.songpack.SongpackEventType;

/** Public read-only surface for event conditions. */
public final class SongpackEventAPI {
    SongpackEventAPI() {} // package-private; only ReactiveMusicAPI constructs it

    /** Is this event active for the player on the last tick? */
    public static boolean isActive(PlayerEntity player, SongpackEventType type) {
        if (player == null || type == null) return false;
        Map<SongpackEventType, Boolean> m = SongpackEventState.snapshot(player.getUuid());
        return Boolean.TRUE.equals(m.get(type));
    }

    /** Overload: lookup by ID (case-insensitive via SongpackEventType.valueOf). */
    public static boolean isActive(PlayerEntity player, String id) {
        if (player == null || id == null) return false;
        SongpackEventType t = SongpackEventType.valueOf(id);
        return t != null && isActive(player, t);
    }

    /** Snapshot of last tick’s conditions for the player (unmodifiable). */
    public static Map<SongpackEventType, Boolean> snapshot(PlayerEntity player) {
        return Collections.unmodifiableMap(SongpackEventState.snapshot(player.getUuid()));
    }
}
