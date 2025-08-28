package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.ReactiveMusicCore;
import circuitlord.reactivemusic.ReactiveMusicState;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;

public final class OverlayTrackPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Overlay Track (Built-In)"; }

    ReactivePlayer musicPlayer;
    ReactivePlayer overlayPlayer;

    @Override public void init() {
        ReactiveMusicState.LOGGER.info("Initializing " + getId() + " songpack event plugin");
        musicPlayer = ReactiveMusic.audio().get("reactive:music");

        ReactiveMusic.audio().create(
            "reactive:overlay",
            ReactivePlayerOptions.create()
            .namespace("reactive")
            .group("overlay")
            .loop(false)
            .gain(1.0f)
            .quietWhenGamePaused(false)
            .linkToMinecraftVolumes(true)
        );
            
        
        overlayPlayer = ReactiveMusic.audio().get("reactive:overlay");
    }
    
    @Override public void newTick() {
        boolean usingOverlay = usingOverlay();
        
        // guard the call
        if (musicPlayer == null || overlayPlayer == null) { return; }
        
        if (usingOverlay) {
            if (!overlayPlayer.isPlaying()) {
                if (!ReactiveMusicState.validEntries.isEmpty()) {
                    RuntimeEntry newEntry = ReactiveMusicState.validEntries.get(0);
                    overlayPlayer.setSong(ReactiveMusicUtils.pickRandomSong(SongPicker.getSelectedSongs(newEntry, ReactiveMusicState.validEntries)));
                }
                overlayPlayer.setFadePercent(0);
                overlayPlayer.play();
            }
            overlayPlayer.fade(1f, 140);
            musicPlayer.fade(0f, 70);
            musicPlayer.stopOnFadeOut(false);
            
        }
        if (!usingOverlay) {
            overlayPlayer.fade(0f, 70);
            overlayPlayer.stopOnFadeOut(true);
            musicPlayer.fade(1, 140);
            musicPlayer.stopOnFadeOut(true);
        }
    };

    /**
     * FIXME
     * This is broken. It should be getting called from processValidEvents... but it isn't.
     * @see SongpackEventPlugin#onValid(RMRuntimeEntry)
     * @see ReactiveMusicCore#processValidEvents(java.util.List, java.util.List)
     */
    @Override public void onValid(RuntimeEntry entry) {
        // ReactiveMusicAPI.LOGGER.info("Overlay enabled");
        // if (entry.useOverlay) {
        //     ReactiveMusicAPI.freezeCore();
        // }
    }
    
    /**
     * FIXME
     * This is broken. It should be getting called from processValidEvents... but it isn't.
     * Or is it? It's not logging, but sometimes the main player breaks.
     * @see SongpackEventPlugin#onInvalid(RMRuntimeEntry)
     * @see ReactiveMusicCore#processValidEvents(java.util.List, java.util.List)
     */
    @Override public void onInvalid(RuntimeEntry entry) {
        // ReactiveMusicAPI.LOGGER.info("Overlay disabled");
        // if (entry.useOverlay) {
        //     ReactiveMusicAPI.unfreezeCore();
        // }
    }

    /**
     * Calling this from <code>newTick()</code> for now since the event processing calls are broken...
     * Or is it? It's not logging, but sometimes the main player breaks.
     * @return
     */
    public static boolean usingOverlay() {
        for (RuntimeEntry entry : ReactiveMusicState.validEntries) {
            if (entry.shouldOverlay()) {
                return true;
            }
        }
        return false;
    }
}
