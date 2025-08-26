package circuitlord.reactivemusic.api;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.Map;

import circuitlord.reactivemusic.songpack.RMSongpackEventState;

/** 
 * Public read-only surface for event conditions, by player.
 * This is not implemented yet.
 * @see RMSongpackEventState
 */
public final class SongpackEventState {
    SongpackEventState() {} // package-private; only ReactiveMusicAPI constructs it

    /** Is this event active for the player on the last tick? */
    public static boolean isActive(PlayerEntity player, SongpackEvent type) {
        if (player == null || type == null) return false;
        Map<SongpackEvent, Boolean> m = RMSongpackEventState.snapshot(player.getUuid());
        return Boolean.TRUE.equals(m.get(type));
    }

    /** Overload: lookup by ID (case-insensitive via SongpackEventType.valueOf). */
    public static boolean isActive(PlayerEntity player, String id) {
        if (player == null || id == null) return false;
        SongpackEvent t = SongpackEvent.valueOf(id);
        return t != null && isActive(player, t);
    }

    /** Snapshot of last tick’s conditions for the player (unmodifiable). */
    public static Map<SongpackEvent, Boolean> snapshot(PlayerEntity player) {
        return Collections.unmodifiableMap(RMSongpackEventState.snapshot(player.getUuid()));
    }
}
