package circuitlord.reactivemusic.api;

import circuitlord.reactivemusic.audio.RMPlayerManagerImpl;

/**
 * Public entrypoint for Reactive Music. Plugin authors should import this class.
 * Keep interfaces (RMPlayer, RMPlayerManager, RMPlayerOptions, SongpackEventPlugin, etc.)
 * in the same 'api' package.
 */
public final class ReactiveMusicAPI {
    private ReactiveMusicAPI() {}

    /** Audio subsystem (player creation, grouping, ducking). */
    public static RMPlayerManager audio() {
        // return the stable API type, backed by the impl singleton
        return RMPlayerManagerImpl.get();
    }

    // --- room to grow ---
    // public static SongpackManager songpacks() { ... }
    // public static EventRegistry events() { ... }
}

