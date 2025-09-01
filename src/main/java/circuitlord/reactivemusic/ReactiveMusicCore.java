/**
 * This file contains extracted code from ReactiveMusic.java that served as
 * the main logic for songpack loading & selection features.
 * 
 * It is now included in the API package so that plugin developers have convenient access to
 * some functions that relate to parsing the data in songpack entries during runtime.
 */
package circuitlord.reactivemusic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import circuitlord.reactivemusic.api.ReactivePlayer;
import circuitlord.reactivemusic.api.ReactivePlayerManager;
import circuitlord.reactivemusic.api.ReactiveMusicUtils;
import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.plugins.OverlayTrackPlugin;
import circuitlord.reactivemusic.songpack.RMSongpackZip;

public final class ReactiveMusicCore {

    public static final Logger LOGGER = LoggerFactory.getLogger("reactive_music");
    public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

    static boolean queuedToPlayMusic = false;
	static boolean queuedToStopMusic = false;
    static int waitForStopTicks = 0;
	static int waitForNewSongTicks = 99999;
	static int fadeOutTicks = 0;
	static int silenceTicks = 0;
    static Random rand = new Random();

    /**
     * This is the built-in logic for Reactive Music's song switcher.
     * @param players Collection of audio players created by a PlayerManager.
     * @see ReactivePlayerManager
     */
    public static void newTick(Collection<ReactivePlayer> players) {
        RMRuntimeEntry newEntry = null;

		// Pick the highest priority one
		if (!ReactiveMusicState.validEntries.isEmpty()) {
            for (RMRuntimeEntry entry : ReactiveMusicState.validEntries) {
                if (!entry.useOverlay) { 
                    if (OverlayTrackPlugin.usingOverlay()) {
                        newEntry = ReactiveMusicState.currentEntry;
                        break;
                    }
                    newEntry = entry;
                    break;
                }
            }
		}

        for (ReactivePlayer player : players) {
            if (player.isFinished() && !OverlayTrackPlugin.usingOverlay()) {
                ReactiveMusicState.currentEntry = null;
                ReactiveMusicState.currentSong = null;
            }
        }


		if (ReactiveMusicState.currentDimBlacklisted)
			newEntry = null;


		if (newEntry != null) {

			List<String> selectedSongs = SongPicker.getSelectedSongs(newEntry, ReactiveMusicState.validEntries);

			// wants to switch if our current entry doesn't exist -- or is not the same as the new one
			boolean wantsToSwitch = !OverlayTrackPlugin.usingOverlay() && (ReactiveMusicState.currentEntry == null || !java.util.Objects.equals(ReactiveMusicState.currentEntry.eventString, newEntry.eventString));
            
            // ReactiveMusic.LOGGER.info(wantsToSwitch ? "Trying to switch the music." : "The music will not switch.");
			
            // if the new entry contains the same song as our current one, then do a "fake" swap to swap over to the new entry
			if (wantsToSwitch && ReactiveMusicState.currentSong != null && newEntry.songs.contains(ReactiveMusicState.currentSong) && !queuedToStopMusic) {
				LOGGER.info("doing fake swap to new event: " + newEntry.eventString);
				// do a fake swap
				ReactiveMusicState.currentEntry = newEntry;
				wantsToSwitch = false;
				// if this happens, also clear the queued state since we essentially did a switch
				queuedToPlayMusic = false;
			}

			// make sure we're fully faded in if we faded out for any reason but this event is valid
            for (ReactivePlayer player : players) {
                if (player.isPlaying() && !wantsToSwitch) {
                    player.fade(1, FADE_DURATION);
                }
            }
                
            boolean isPlaying = false;
            for (ReactivePlayer player : players) {
                if (player.isPlaying()) {
                    isPlaying = true;
                    break;
                }
            }

			// ---- FADE OUT ----
			if ((wantsToSwitch || queuedToStopMusic) && isPlaying && !OverlayTrackPlugin.usingOverlay()) {
				waitForStopTicks++;
				boolean shouldFadeOutMusic = false;
				// handle fade-out if something's playing when a new event becomes valid
				if (waitForStopTicks > getMusicStopSpeed(ReactiveMusicState.currentSongpack)) {
					shouldFadeOutMusic = true;
				}
				// if we're queued to force stop the music, do so here
				if (queuedToStopMusic) {
					shouldFadeOutMusic = true;
				}

				if (shouldFadeOutMusic) {
                    for (ReactivePlayer player : players) {
                        player.stopOnFadeOut(true);
                        player.resetOnFadeOut(true);
                        player.fade(0, FADE_DURATION);
                    }
				}
			}
			else {
				waitForStopTicks = 0;
			}

			//  ---- SWITCH SONG ----
            // TODO: Refactor the overlay check to something expandable.
            // Also --> where else can that be done???
            // Potentially some really cool possibilities with more hooks like that.
            //
			if ((wantsToSwitch || queuedToPlayMusic) && !isPlaying && !OverlayTrackPlugin.usingOverlay()) {
				waitForNewSongTicks++;
				boolean shouldStartNewSong = false;
				if (waitForNewSongTicks > getMusicDelay(ReactiveMusicState.currentSongpack)) {
					shouldStartNewSong = true;
				}
				// if we're queued to start a new song and we're not playing anything, do it
				if (queuedToPlayMusic) {
					shouldStartNewSong = true;
				}
				if (shouldStartNewSong) {
					String picked = ReactiveMusicUtils.pickRandomSong(selectedSongs);
                    for (ReactivePlayer player : players) {
					    changeCurrentSong(picked, newEntry, player);
                    }
					waitForNewSongTicks = 0;
					queuedToPlayMusic = false;
				}
			}
			else {
				waitForNewSongTicks = 0;
			}
		}

		// no entries are valid, we shouldn't be playing any music!
		// this can happen if no entry is valid or the dimension is blacklisted
		else {
            ReactiveMusic.LOGGER.info("There are no valid songpack entries!");
            for (ReactivePlayer player : players)
			    player.fade(0, FADE_DURATION);
		}
	}
    
    public static List<RMRuntimeEntry> getValidEntries() {
        List<RMRuntimeEntry> validEntries = new ArrayList<>();
    
        for (RMRuntimeEntry loadedEntry : ReactiveMusicState.loadedEntries) {
    
            boolean isValid = SongPicker.isEntryValid(loadedEntry);
    
            if (isValid) {
                validEntries.add(loadedEntry);
            }
        }
    
        return validEntries;
    }
    
    public final static void processValidEvents(List<RMRuntimeEntry> validEntries, List<RMRuntimeEntry> previousValidEntries) {

        for (var entry : previousValidEntries) {
            // if this event was valid before and is invalid now
            if (validEntries.stream().noneMatch(e -> java.util.Objects.equals(e.eventString, entry.eventString))) {
                
                ReactiveMusic.LOGGER.info("Triggering onInvalid() for songpack event plugins");
                for (SongpackEventPlugin plugin : ReactiveMusic.PLUGINS) plugin.onInvalid(entry);
    
                if (entry.forceStopMusicOnInvalid) {
                    ReactiveMusic.LOGGER.info("trying forceStopMusicOnInvalid: " + entry.eventString);
                    if (entry.cachedRandomChance <= entry.forceChance) {
                        ReactiveMusic.LOGGER.info("doing forceStopMusicOnInvalid: " + entry.eventString);
                        queuedToStopMusic = true;
                    }
                    break;
                }
            }
        }
        
        for (var entry : validEntries) {
    
            if (previousValidEntries.stream().noneMatch(e -> java.util.Objects.equals(e.eventString, entry.eventString))) {
                // use the same random chance for all so they always happen together
                entry.cachedRandomChance = rand.nextFloat();
                boolean randSuccess = entry.cachedRandomChance <= entry.forceChance;
    
                // if this event wasn't valid before and is now
                ReactiveMusic.LOGGER.info("Triggering onValid() for songpack event plugins");
                for (SongpackEventPlugin plugin : ReactiveMusic.PLUGINS) plugin.onValid(entry);
    
                if (entry.forceStopMusicOnValid) {
                    ReactiveMusic.LOGGER.info("trying forceStopMusicOnValid: " + entry.eventString);
                    if (randSuccess) {
                        ReactiveMusic.LOGGER.info("doing forceStopMusicOnValid: " + entry.eventString);
                        queuedToStopMusic = true;
                    }
                }
                if (entry.forceStartMusicOnValid) {
                    ReactiveMusic.LOGGER.info("trying forceStartMusicOnValid: " + entry.eventString);
                    if (randSuccess) {
                        ReactiveMusic.LOGGER.info("doing forceStartMusicOnValid: " + entry.eventString);
                        queuedToPlayMusic = true;
                    }
                }
            }
        }
    }
    
    
    public static void changeCurrentSong(String song, RMRuntimeEntry newEntry, ReactivePlayer player) {
        // No change? Do nothing.
        if (java.util.Objects.equals(ReactiveMusicState.currentSong, song)) {
            queuedToPlayMusic = false;
            return;
        }
    
        // Stop only if we’re switching tracks (not just metadata)
        final boolean switchingTrack = !java.util.Objects.equals(ReactiveMusicState.currentSong, song);
        if (switchingTrack && player != null && player.isPlaying()) {
            player.stop(); // RMPlayerImpl stops underlying AdvancedPlayer.play()
        }
    
        ReactiveMusicState.currentSong = song;
        ReactiveMusicState.currentEntry = newEntry;
    
        if (player != null && song != null) {
            // if you do a fade-in elsewhere, set 0 here; otherwise set 1
            player.setGainPercent(1.0f);
            player.setDuckPercent(1.0f);
            player.setSong(song);   // resolves to music/<song>.mp3 inside RMPlayerImpl
            player.play();          // worker thread runs blocking play() internally
        }
    
        queuedToPlayMusic = false;
    }
    
    
    
    public static final void setActiveSongpack(RMSongpackZip songpackZip) {
    
        // TODO: Support more than one songpack?
        if (ReactiveMusicState.currentSongpack != null) {
            deactivateSongpack(ReactiveMusicState.currentSongpack);
        }
    
        for (ReactivePlayer player : ReactiveMusic.audio().getAll()) {
            resetPlayer(player);
        }

        ReactiveMusicState.currentEntry = null;
        ReactiveMusicState.currentSong = null;
    
        ReactiveMusicState.currentSongpack = songpackZip;
    
        ReactiveMusicState.loadedEntries = songpackZip.runtimeEntries;
    
        // always start new music immediately
        queuedToPlayMusic = true;
    
    }
    
    public static final void deactivateSongpack(RMSongpackZip songpackZip) {
    
        // remove all entries that match that name
        for (int i = ReactiveMusicState.loadedEntries.size() - 1; i >= 0; i--) {
            if (ReactiveMusicState.loadedEntries.get(i).songpack == songpackZip.config.name) {
                ReactiveMusicState.loadedEntries.remove(i);
            }
        }
    
    }
    
    public final static int getMusicStopSpeed(RMSongpackZip songpack) {
    
        MusicSwitchSpeed speed = ReactiveMusic.modConfig.musicSwitchSpeed2;
    
        if (ReactiveMusic.modConfig.musicSwitchSpeed2 == MusicSwitchSpeed.SONGPACK_DEFAULT) {
            speed = songpack.config.musicSwitchSpeed;
        }
    
        if (ReactiveMusic.modConfig.debugModeEnabled) {
            speed = MusicSwitchSpeed.INSTANT;
        }
    
        switch (speed) {
            case INSTANT:
                return 100;
            case SHORT:
                return 250;
            case NORMAL:
                return 900;
            case LONG:
                return 2400;
            default:
                break;
        }
    
        return 100;
    
    }
    
    public final static int getMusicDelay(RMSongpackZip songpack) {
    
        MusicDelayLength delay = ReactiveMusic.modConfig.musicDelayLength2;
    
        if (ReactiveMusic.modConfig.musicDelayLength2 == MusicDelayLength.SONGPACK_DEFAULT) {
            delay = songpack.config.musicDelayLength;
        }
    
        if (ReactiveMusic.modConfig.debugModeEnabled) {
            delay = MusicDelayLength.NONE;
        }
    
        switch (delay) {
            case NONE:
                return 0;
            case SHORT:
                return 250;
            case NORMAL:
                return 900;
            case LONG:
                return 2400;
            default:
                break;
        }
    
        return 100;
    
    }
    
    public static final void resetPlayer(ReactivePlayer player) {
        if (player != null && player.isPlaying()) {
            player.stop();
            player.reset();
        }
    }

}
