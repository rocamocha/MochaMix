package circuitlord.reactivemusic.api;

import java.util.Collection;
import java.util.List;

import circuitlord.reactivemusic.*;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.songpack.RMSongpackZip;

public interface ReactiveMusicAPI {
    public interface ModConfig {
        static boolean debugModeEnabled() { return ReactiveMusic.modConfig.debugModeEnabled; }
    }

    static ReactivePlayerManager audioManager() { return ReactiveMusic.audio(); }
    static Collection<ReactivePlayer> reactivePlayers() { return ReactiveMusic.audio().getAll();}

    static RMSongpackZip currentSongpack() { return ReactiveMusicState.currentSongpack; }
    static RMRuntimeEntry currentEntry() { return ReactiveMusicState.currentEntry; }
    static String currentSong() { return ReactiveMusicState.currentSong; }

    static List<RMRuntimeEntry> validEntries() { return ReactiveMusicState.validEntries; }
    static List<RMRuntimeEntry> loadEntries() { return ReactiveMusicState.loadedEntries; }
    static List<RMRuntimeEntry> previousValidEntries() { return ReactiveMusicState.previousValidEntries; }
    static List<String> recentlyPickedSongs() { return ReactiveMusicState.recentlyPickedSongs; }
}
