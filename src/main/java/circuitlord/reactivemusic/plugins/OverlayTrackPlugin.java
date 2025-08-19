package circuitlord.reactivemusic.plugins;

import java.util.Map;

import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public final class OverlayTrackPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Overlay Track (Built-In)"; }

    RMPlayer musicPlayer;
    RMPlayer overlayPlayer;

    @Override public void init() {
        ReactiveMusicAPI.LOGGER.info("Initializing " + getId() + " songpack event plugin");
        musicPlayer = ReactiveMusicAPI.audio().get("reactive:music");

        ReactiveMusicAPI.audio().create(
            "reactive:overlay",
            RMPlayerOptions.create()
            .namespace("reactive")
            .group("overlay")
            .loop(false)
            .gain(1.0f)
            .quietWhenGamePaused(false)
            .linkToMinecraftVolumes(true)
        );
            
        
        overlayPlayer = ReactiveMusicAPI.audio().get("reactive:overlay");
    }
    
    @Override public void newTick() {
        boolean usingOverlay = usingOverlay();
        
        // guard the call
        if (musicPlayer == null || overlayPlayer == null) { return; }
        
        if (usingOverlay) {
            if (!overlayPlayer.isPlaying()) {
                if (!ReactiveMusicAPI.validEntries.isEmpty()) {
                    RMRuntimeEntry newEntry = ReactiveMusicAPI.validEntries.get(0);
                    overlayPlayer.setSong(ReactiveMusicUtils.pickRandomSong(ReactiveMusicAPI.getSelectedSongs(newEntry, ReactiveMusicAPI.validEntries)));
                }
                overlayPlayer.setFadePercent(0);
                overlayPlayer.play();
            }
            overlayPlayer.fade(1f, 140);
            musicPlayer.fade(0f, 70);
            
        }
        if (!usingOverlay) {
            overlayPlayer.fade(0f, 70);
            overlayPlayer.stopOnFadeOut(true);
        }
    };

    /**
     * FIXME
     * This is broken. It should be getting called from processValidEvents... but it isn't.
     * @see SongpackEventPlugin#onValid(RMRuntimeEntry)
     * @see ReactiveMusicCore#processValidEvents(java.util.List, java.util.List)
     */
    @Override public void onValid(RMRuntimeEntry entry) {
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
    @Override public void onInvalid(RMRuntimeEntry entry) {
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
    private static boolean usingOverlay() {
        for (RMRuntimeEntry entry : ReactiveMusicAPI.validEntries) {
            if (entry.useOverlay) {
                return true;
            }
        }
        return false;
    }
}
