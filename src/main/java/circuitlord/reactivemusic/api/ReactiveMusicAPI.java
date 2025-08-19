package circuitlord.reactivemusic.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.SongpackZip;
import circuitlord.reactivemusic.audio.RMPlayerManagerImpl;
import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Public entrypoint for Reactive Music. Plugin authors should import this class.
 */
public final class ReactiveMusicAPI {
    private ReactiveMusicAPI() {
        logicFreeze.put("ReactiveMusicCore", false);
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");
    
    public static ModConfig   modConfig;
    public static SongpackZip currentSongpack = null;
    public static Boolean currentDimBlacklisted = false;
    
    public static Map<String, Boolean>            logicFreeze      = new HashMap<>();
    public static Map<SongpackEventType, Boolean> songpackEventMap = new HashMap<>();
    
    public static List<RMRuntimeEntry> validEntries         = new ArrayList<>();
    public static List<RMRuntimeEntry> loadedEntries        = new ArrayList<>();
    public static List<RMRuntimeEntry> previousValidEntries = new ArrayList<>();
    public static List<String>         recentlyPickedSongs  = new ArrayList<>();
    
    public static Collection<RMPlayer> corePlayers = audio().getAll();

    public static final void freezeCore() {
        LOGGER.info("Freezing Reactive Music's core logic execution");
        logicFreeze.put("ReactiveMusicCore", true);
    }

    public static final void unfreezeCore() {
        LOGGER.info("Unfreezing Reactive Music's core logic execution");
        logicFreeze.put("ReactiveMusicCore", false);
    }
    
    /**
     * Audio subsystem (player creation, grouping, ducking).
     * @return The core Reactive Music audio player manager. Unless you are doing something
     * very complicated, you should not need to instance a new manager. 
     */
    public static RMPlayerManager audio() {
        // return the stable API type, backed by the impl singleton
        return RMPlayerManagerImpl.get();
    }

    public static @NotNull List<String> getSelectedSongs(RMRuntimeEntry newEntry, List<RMRuntimeEntry> validEntries) {

		// if we have non-recent songs then just return those
		if (ReactiveMusicUtils.hasSongNotPlayedRecently(newEntry.songs)) {
			return newEntry.songs;
		}

		// Fallback behaviour
		if (newEntry.allowFallback) {
			for (int i = 1; i < ReactiveMusicAPI.validEntries.size(); i++) {
				if (ReactiveMusicAPI.validEntries.get(i) == null)
					continue;

				// check if we have songs not played recently and early out
				if (ReactiveMusicUtils.hasSongNotPlayedRecently(ReactiveMusicAPI.validEntries.get(i).songs)) {
					return ReactiveMusicAPI.validEntries.get(i).songs;
				}
			}
		}

		// we've played everything recently, just give up and return this event's songs
		return newEntry.songs;
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

