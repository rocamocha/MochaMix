package circuitlord.reactivemusic;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.config.MusicDelayLength;
import circuitlord.reactivemusic.config.MusicSwitchSpeed;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReactiveMusic implements ModInitializer {

	private static String entryKey(RMRuntimeEntry e) {
		return (e == null) ? "null" : e.eventString; // or a real stable id if you have one
	}

	public static final String MOD_ID = "reactive_music";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int FADE_DURATION = 150;
	public static final int SILENCE_DURATION = 100;

	public static int additionalSilence = 0;

	public static RMPlayer musicPlayer;


	public static SongpackZip currentSongpack = null;

	static boolean queuedToPlayMusic = false;
	static boolean queuedToStopMusic = false;

	//static List<RMRuntimeEntry> currentEntries = new ArrayList<>();

	static String currentSong = null;
	static RMRuntimeEntry currentEntry = null;

	// Identify the currently-applied entry without object identity flapping
	private static String currentEntryKey = null;

	// Optional: short guard against flapping (can be 0 if you don’t want it)
	private static int switchCooldownTicks = 0;
	private static final int SWITCH_COOLDOWN = 5; // ~250ms at 20 TPS

	//static List<SongpackEntry> currentGenericEntries = new ArrayList<>();
	
	//static String nextSong;
	static int waitForStopTicks = 0;
	static int waitForNewSongTicks = 99999;
	static int fadeOutTicks = 0;
	//static int fadeInTicks = 0;
	static int silenceTicks = 0;

	static int musicTrackedSoundsDuckTicks = 0;

	static int slowTickUpdateCounter = 0;

	static boolean currentDimBlacklisted = false;

	boolean doSilenceForNextQueuedSong = true;

	static List<RMRuntimeEntry> previousValidEntries = new ArrayList<>();


	static Random rand = new Random();


	public static ModConfig config;


	// Add this static list to the class
	//private static List<SongpackEntry> validEntries = new ArrayList<>();


	private static List<RMRuntimeEntry> loadedEntries = new ArrayList<>();


	public static final List<SoundInstance> trackedSoundsMuteMusic = new ArrayList<SoundInstance>();


	@Override
	public void onInitialize() {

		LOGGER.info("Initializing Reactive Music");

		if (circuitlord.reactivemusic.api.ReactiveMusicUtils.isClientEnv()) {
			try {
				Class.forName("circuitlord.reactivemusic.ClientBootstrap")
					.getMethod("install").invoke(null);
			} catch (Throwable ignored) {
				// leave delegate null on failure; API calls will return false
			}
		}

		ModConfig.GSON.load();
		config = ModConfig.getConfig();

		SongPicker.initialize();


		RMPlayerManager audioManager = ReactiveMusicAPI.audio();
		musicPlayer = audioManager.create(
			"reactive:music",
			RMPlayerOptions.create()
				.namespace("reactive")
				.group("music")
				.loop(false)
				.gain(1.0f)
				.quietWhenGamePaused(false)
		);

		RMSongpackLoader.fetchAvailableSongpacks();

		boolean loadedUserSongpack = false;

		// try to load a saved songpack
		if (!config.loadedUserSongpack.isEmpty()) {

			for (var songpack : RMSongpackLoader.availableSongpacks) {
				if (!songpack.config.name.equals(config.loadedUserSongpack)) continue;

				// something is broken in this songpack, don't load it
				if (songpack.blockLoading)
					continue;

				setActiveSongpack(songpack);
				loadedUserSongpack = true;

				break;
			}
		}

		// load the default one
		if (!loadedUserSongpack) {

			// for the cases where something is broken in the base songpack
			if (!RMSongpackLoader.availableSongpacks.get(0).blockLoading) {
				// first is the default songpack
				setActiveSongpack(RMSongpackLoader.availableSongpacks.get(0));
			}
		}




        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("reactivemusic")
				.executes(context -> {
							MinecraftClient mc = context.getSource().getClient();
							Screen screen = ModConfig.createScreen(mc.currentScreen);
							mc.send(() -> mc.setScreen(screen));
							return 1;
						}
				)


				.then(ClientCommandManager.literal("logBlockCounter")
						.executes(context -> {

							SongPicker.queuedToPrintBlockCounter = true;

							return 1;
						})
				)

				.then(ClientCommandManager.literal("blacklistDimension")
						.executes(context -> {

							String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

							if (config.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was already in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Added " + key + " to blacklist."));

							config.blacklistedDimensions.add(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

				.then(ClientCommandManager.literal("unblacklistDimension")
						.executes(context -> {
							String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

							if (!config.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was not in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Removed " + key + " from blacklist."));

							config.blacklistedDimensions.remove(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

			)
		);
	}

	public static void newTick() {

		if (musicPlayer == null) return;
		if (currentSongpack == null) return;
		if (loadedEntries.isEmpty()) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) return;


		// force a reasonable volume once on mod install, if you have full 100% everything it's way too loud
		if (!config.hasForcedInitialVolume) {
			config.hasForcedInitialVolume = true;
			ModConfig.saveConfig();

			if (mc.options.getSoundVolume(SoundCategory.MASTER) > 0.5) {

				LOGGER.info("Forcing master volume to a lower default, this will only happen once on mod-install to avoid loud defaults.");

				mc.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.5);
				mc.options.write();
			}
		}
		
		{
			currentDimBlacklisted = false;

			// see if the dimension we're in is blacklisted -- update at same time as event map to keep them in sync
			if (mc != null && mc.world != null) {
				String curDim = mc.world.getRegistryKey().getValue().toString();

				for (String dim : config.blacklistedDimensions) {
					if (dim.equals(curDim)) {
						currentDimBlacklisted = true;
						break;
					}
				}
			}

			SongPicker.tickEventMap();
		}


		// -------------------------

		// clear playing state if not playing
		// if (musicPlayer != null && !musicPlayer.isPlaying()) {
		// 	resetPlayer();
		// }


		// -------------------------

		processTrackedSoundsMuteMusic();


		RMRuntimeEntry newEntry = null;

		List<RMRuntimeEntry> validEntries = getValidEntries();

		// Pick the highest priority one
		if (!validEntries.isEmpty()) {
			newEntry = validEntries.get(0);
		}

		processValidEvents(validEntries, previousValidEntries);


		if (currentDimBlacklisted)
			newEntry = null;


		if (newEntry != null) {

			List<String> selectedSongs = getSelectedSongs(newEntry, validEntries);


			// wants to switch if our current entry doesn't exist -- or is not the same as the new one
			boolean wantsToSwitch = currentEntry == null || !java.util.Objects.equals(currentEntry.eventString, newEntry.eventString);

			// if the new entry contains the same song as our current one, then do a "fake" swap to swap over to the new entry
			if (wantsToSwitch && currentSong != null && newEntry.songs.contains(currentSong) && !queuedToStopMusic) {
				LOGGER.info("doing fake swap to new event: " + newEntry.eventString);
				// do a fake swap
				currentEntry = newEntry;
				wantsToSwitch = false;
				// if this happens, also clear the queued state since we essentially did a switch
				queuedToPlayMusic = false;
			}

			// make sure we're fully faded in if we faded out for any reason but this event is valid
			if (musicPlayer.isPlaying() && !wantsToSwitch && fadeOutTicks > 0) {
				fadeOutTicks--;
				// Copy the behavior from below where it fades out
				musicPlayer.setGainPercent(1f - (fadeOutTicks / (float)FADE_DURATION));
			}



			// ---- FADE OUT ----
			if ((wantsToSwitch || queuedToStopMusic) && musicPlayer.isPlaying()) {
				waitForStopTicks++;
				boolean shouldFadeOutMusic = false;
				// handle fade-out if something's playing when a new event becomes valid
				if (waitForStopTicks > getMusicStopSpeed(currentSongpack)) {
					shouldFadeOutMusic = true;
				}
				// if we're queued to force stop the music, do so here
				if (queuedToStopMusic) {
					shouldFadeOutMusic = true;
				}

				if (shouldFadeOutMusic) {
					tickFadeOut();
				}
			}
			else {
				waitForStopTicks = 0;
			}

			//  ---- SWITCH SONG ----

			if ((wantsToSwitch || queuedToPlayMusic) && !musicPlayer.isPlaying()) {
				waitForNewSongTicks++;
				boolean shouldStartNewSong = false;
				if (waitForNewSongTicks > getMusicDelay(currentSongpack)) {
					shouldStartNewSong = true;
				}
				// if we're queued to start a new song and we're not playing anything, do it
				if (queuedToPlayMusic) {
					shouldStartNewSong = true;
				}
				if (shouldStartNewSong) {
					String picked = SongPicker.pickRandomSong(selectedSongs);
					changeCurrentSong(picked, newEntry);
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
			tickFadeOut();
		}

		// new player processes gain continuously internally
		// thread.processRealGain();

		previousValidEntries = new java.util.ArrayList<>(validEntries);

	}

	private static @NotNull List<String> getSelectedSongs(RMRuntimeEntry newEntry, List<RMRuntimeEntry> validEntries) {

		// if we have non-recent songs then just return those
		if (SongPicker.hasSongNotPlayedRecently(newEntry.songs)) {
			return newEntry.songs;
		}

		// Fallback behaviour
		if (newEntry.allowFallback) {
			for (int i = 1; i < validEntries.size(); i++) {
				if (validEntries.get(i) == null)
					continue;

				// check if we have songs not played recently and early out
				if (SongPicker.hasSongNotPlayedRecently(validEntries.get(i).songs)) {
					return validEntries.get(i).songs;
				}
			}
		}


		// we've played everything recently, just give up and return this event's songs
		return newEntry.songs;
	}


	public static List<RMRuntimeEntry> getValidEntries() {
		List<RMRuntimeEntry> validEntries = new ArrayList<>();

        for (RMRuntimeEntry loadedEntry : loadedEntries) {

            boolean isValid = SongPicker.isEntryValid(loadedEntry);

            if (isValid) {
                validEntries.add(loadedEntry);
            }
        }

		return validEntries;
	}

	private static void processValidEvents(List<RMRuntimeEntry> validEntries, List<RMRuntimeEntry> previousValidEntries) {


		for (var entry : previousValidEntries) {
			// if this event was valid before and is invalid now
			if (entry.forceStopMusicOnInvalid && validEntries.stream().noneMatch(e -> java.util.Objects.equals(e.eventString, entry.eventString))) {
				LOGGER.info("trying forceStopMusicOnInvalid: " + entry.eventString);
				if (entry.cachedRandomChance <= entry.forceChance) {
					LOGGER.info("doing forceStopMusicOnInvalid: " + entry.eventString);
					queuedToStopMusic = true;
				}
				break;
			}
		}
		for (var entry : validEntries) {

			if (previousValidEntries.stream().noneMatch(e -> java.util.Objects.equals(e.eventString, entry.eventString))) {
				// use the same random chance for all so they always happen together
				entry.cachedRandomChance = rand.nextFloat();
				boolean randSuccess = entry.cachedRandomChance <= entry.forceChance;

				// if this event wasn't valid before and is now
				if (entry.forceStopMusicOnValid) {
					LOGGER.info("trying forceStopMusicOnValid: " + entry.eventString);
					if (randSuccess) {
						LOGGER.info("doing forceStopMusicOnValid: " + entry.eventString);
						queuedToStopMusic = true;
					}
				}
				if (entry.forceStartMusicOnValid) {
					LOGGER.info("trying forceStartMusicOnValid: " + entry.eventString);
					if (randSuccess) {
						LOGGER.info("doing forceStartMusicOnValid: " + entry.eventString);
						queuedToPlayMusic = true;
					}
				}
			}
		}
	}


	public static void tickFadeOut() {
		if (musicPlayer == null || !musicPlayer.isPlaying()) return;
		if (fadeOutTicks < FADE_DURATION) {
			LOGGER.info("RM:[tickFadeOut]: Fading out... " + fadeOutTicks + "/" + FADE_DURATION);
			fadeOutTicks++;
			musicPlayer.setGainPercent(1f - (fadeOutTicks / (float) FADE_DURATION));
		} else {
			LOGGER.info("RM:[tickFadeOut]: Fadeout completed!");
			resetPlayer();
		}
	}


	public static void changeCurrentSong(String song, RMRuntimeEntry newEntry) {
		// No change? Do nothing.
		if (java.util.Objects.equals(currentSong, song)) {
			queuedToPlayMusic = false;
			return;
		}

		// Stop only if we’re switching tracks (not just metadata)
		final boolean switchingTrack = !java.util.Objects.equals(currentSong, song);
		if (switchingTrack && musicPlayer != null && musicPlayer.isPlaying()) {
			musicPlayer.stop(); // RMPlayerImpl stops underlying AdvancedPlayer.play()
		}

		currentSong = song;
		currentEntry = newEntry;

		if (musicPlayer != null && song != null) {
			// if you do a fade-in elsewhere, set 0 here; otherwise set 1
			musicPlayer.setGainPercent(1.0f);
			musicPlayer.setDuckPercent(1.0f);
			musicPlayer.setSong(song);   // resolves to music/<song>.mp3 inside RMPlayerImpl
			musicPlayer.play();          // worker thread runs blocking play() internally
		}

		queuedToPlayMusic = false;
		switchCooldownTicks = SWITCH_COOLDOWN;
	}



	public static void setActiveSongpack(SongpackZip songpackZip) {

		// TODO: more than one songpack?
		if (currentSongpack != null) {
			deactivateSongpack(currentSongpack);
		}

		resetPlayer();

		currentSongpack = songpackZip;

		loadedEntries = songpackZip.runtimeEntries;

		// always start new music immediately
		queuedToPlayMusic = true;

	}

	public static void deactivateSongpack(SongpackZip songpackZip) {

		// remove all entries that match that name
		for (int i = loadedEntries.size() - 1; i >= 0; i--) {
			if (loadedEntries.get(i).songpack == songpackZip.config.name) {
				loadedEntries.remove(i);
			}
		}

	}

	public static int getMusicStopSpeed(SongpackZip songpack) {

		MusicSwitchSpeed speed = config.musicSwitchSpeed2;

		if (config.musicSwitchSpeed2 == MusicSwitchSpeed.SONGPACK_DEFAULT) {
			speed = songpack.config.musicSwitchSpeed;
		}

		if (config.debugModeEnabled) {
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

	public static int getMusicDelay(SongpackZip songpack) {

		MusicDelayLength delay = config.musicDelayLength2;

		if (config.musicDelayLength2 == MusicDelayLength.SONGPACK_DEFAULT) {
			delay = songpack.config.musicDelayLength;
		}

		if (config.debugModeEnabled) {
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

	static void resetPlayer() {
		if (musicPlayer != null && musicPlayer.isPlaying()) {
			musicPlayer.stop();
		}
		fadeOutTicks = 0;
		currentEntry = null;
		currentSong = null;
	}




	private static void processTrackedSoundsMuteMusic() {

		// remove if the song is null or not playing anymore
		trackedSoundsMuteMusic.removeIf(soundInstance -> soundInstance == null || !MinecraftClient.getInstance().getSoundManager().isPlaying(soundInstance));

		GameOptions options = MinecraftClient.getInstance().options;

		boolean foundSoundInstance = false;

		for (SoundInstance soundInstance : trackedSoundsMuteMusic) {

			// if this is a sound with some sort of falloff
			if (soundInstance.getAttenuationType() != SoundInstance.AttenuationType.NONE) {

				Vec3d pos = new Vec3d(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());

				if (MinecraftClient.getInstance().player != null) {
					Vec3d dist = MinecraftClient.getInstance().player.getPos().subtract(pos);

					if (dist.length() > 65.f) {
						continue;
					}
				}
			}

			// if we can't hear it, don't include it
			if (options.getSoundVolume(soundInstance.getCategory()) < 0.04) {
				continue;
			}

			foundSoundInstance = true;

			break;
		}




		// only duck for jukebox if our volume is loud enough to where it would matter
		if (foundSoundInstance) {

			if (musicTrackedSoundsDuckTicks < FADE_DURATION) {
				musicTrackedSoundsDuckTicks++;
			}

		}
		else {
			if (musicTrackedSoundsDuckTicks > 0) {
				musicTrackedSoundsDuckTicks--;
			}
		}

		musicPlayer.setDuckPercent(1f - (musicTrackedSoundsDuckTicks / (float) FADE_DURATION));


	}







}