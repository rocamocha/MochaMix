package circuitlord.reactivemusic.api;

import java.util.List;

import circuitlord.reactivemusic.*;
import circuitlord.reactivemusic.songpack.RMSongpackLoader;

public interface ReactiveMusicAPI {
    public interface ModConfig {
        static boolean debugModeEnabled() { return ReactiveMusic.modConfig.debugModeEnabled; }
    }

    /**
     * API view for the anything related to the EVENT system should go here.
     * This allows for expandability in the future past the event system, and better
     * modularity.
     */
    public interface EventSys { 
        static RuntimeEntry currentEntry() { return ReactiveMusicState.currentEntry; }
        static String currentSong() { return ReactiveMusicState.currentSong; }
        static List<String> recentSongs() { return ReactiveMusicState.recentlyPickedSongs; }
        static List<RuntimeEntry> validEntries() { return List.copyOf(ReactiveMusicState.validEntries); }
        static List<RuntimeEntry> loadedEntries() { return List.copyOf(ReactiveMusicState.loadedEntries); }
        static List<RuntimeEntry> previousValidEntries() { return List.copyOf(ReactiveMusicState.previousValidEntries); }
    }

    public interface Songpack {
        static SongpackZip getCurrent() { return ReactiveMusicState.currentSongpack; }
        static List<SongpackZip> getAvailable() { return List.copyOf(RMSongpackLoader.availableSongpacks); }
    }

    static ReactivePlayerManager audioManager() { return ReactiveMusic.audio(); }


}
