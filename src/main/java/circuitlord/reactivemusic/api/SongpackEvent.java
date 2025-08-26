package circuitlord.reactivemusic.api;

import java.util.Map;
import circuitlord.reactivemusic.songpack.RMSongpackEvent;

/**
 * This had to be structured as a coupling.
 * This is the core of RM, please be careful if you are going to touch or change this.
 * @see RMSongpackEvent
 */
public interface SongpackEvent {
    // Do not leak impl here
    Map<String, ? extends SongpackEvent> getMap();

    // Static API that delegates to the impl
    static SongpackEvent get(String id) { return RMSongpackEvent.get(id); }
    static SongpackEvent register(String id) { return RMSongpackEvent.register(id); }
    static SongpackEvent[] values() { return RMSongpackEvent.values(); }
    static SongpackEvent valueOf(String name) { return RMSongpackEvent.valueOf(name); }

    // Optional: expose predefined constants as interface-typed fields
    SongpackEvent NONE = RMSongpackEvent.NONE;
    SongpackEvent MAIN_MENU = RMSongpackEvent.MAIN_MENU;
    SongpackEvent CREDITS = RMSongpackEvent.CREDITS;
    SongpackEvent GENERIC = RMSongpackEvent.GENERIC;
}
