package circuitlord.reactivemusic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.songpack.SongpackEventType;
import circuitlord.reactivemusic.songpack.SongpackZip;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Public entrypoint for Reactive Music. Plugin authors should import this class.
 */
public final class ReactiveMusicState {
    
    
    private ReactiveMusicState() {
        logicFreeze.put("ReactiveMusicCore", false);
    }
    
    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");

    public static SongpackZip currentSongpack = null;
    public static Boolean currentDimBlacklisted = false;
    
    public static Map<String, Boolean>            logicFreeze      = new HashMap<>();
    public static Map<SongpackEventType, Boolean> songpackEventMap = new HashMap<>();
    
    public static RMRuntimeEntry       currentEntry         = null;
    public static String               currentSong          = null;

    public static List<RMRuntimeEntry> validEntries         = new ArrayList<>();
    public static List<RMRuntimeEntry> loadedEntries        = new ArrayList<>();
    public static List<RMRuntimeEntry> previousValidEntries = new ArrayList<>();
    public static List<String>         recentlyPickedSongs  = new ArrayList<>();

    public static final void freezeCore() {
        LOGGER.info("Freezing Reactive Music's core logic execution");
        logicFreeze.put("ReactiveMusicCore", true);
    }

    public static final void unfreezeCore() {
        LOGGER.info("Unfreezing Reactive Music's core logic execution");
        logicFreeze.put("ReactiveMusicCore", false);
    }
    
    /**
     * NOTE: Not implemented, do not use! Does nothing!
     * @param player
     * @param eventString
     * @return
     */
    public static boolean eventActive(PlayerEntity player, String eventString) {
        throw new UnsupportedOperationException(
            "You've called eventActive(PlayerEntity, SonpackEventType) in your code. This is meant to check event state in multiplayer - this is not implemented yet."
        );
    }

    // --- room to grow ---
    // public static SongpackManager songpacks() { ... }
    // public static EventRegistry events() { ... }
}

