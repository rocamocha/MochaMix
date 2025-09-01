package circuitlord.reactivemusic;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.audio.RMPlayerManager;
import circuitlord.reactivemusic.config.ModConfig;
import circuitlord.reactivemusic.songpack.RMSongpackLoader;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ReactiveMusic implements ModInitializer {

	public static final String MOD_ID = "reactive_music";
	public static final ServiceLoader<SongpackEventPlugin> PLUGINS = ServiceLoader.load(SongpackEventPlugin.class);

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ModConfig modConfig;

	public static int additionalSilence = 0;
	private static ReactivePlayer musicPlayer;
	static int musicTrackedSoundsDuckTicks = 0;

	boolean doSilenceForNextQueuedSong = true;
	public static final List<SoundInstance> trackedSoundsMuteMusic = new ArrayList<SoundInstance>();

	/**
     * Audio subsystem (player creation, grouping, ducking).
     * @return The core Reactive Music audio player manager. Unless you are doing something
     * very complicated, you should not need to instance a new manager. 
     */
    public static ReactivePlayerManager audio() { return RMPlayerManager.get(); }

	@Override public void onInitialize() {
		ModConfig.GSON.load();
		modConfig = ModConfig.getConfig();
		
		ReactiveMusicState.logicFreeze.put("ReactiveMusicCore", false);
		LOGGER.info("Initializing Reactive Music");
		
		if (circuitlord.reactivemusic.api.ReactiveMusicUtils.isClientEnv()) {
			try {
				Class.forName("circuitlord.reactivemusic.ClientBootstrap")
				.getMethod("install").invoke(null);
			} catch (Throwable ignored) {
				// leave delegate null on failure; API calls will return false
			}
		}
		
		// Create the primary audio player
		musicPlayer = audio().create(
			"reactive:music",
			ReactivePlayerOptions.create()
			.namespace("reactive")
			.group("music")
			.loop(false)
			.gain(1.0f)
			.quietWhenGamePaused(false)
		);
			
			SongPicker.initialize();
			
			for (SongpackEventPlugin plugin: PLUGINS) { plugin.init(); }
			
			RMSongpackLoader.fetchAvailableSongpacks();
			boolean loadedUserSongpack = false;
			
			// try to load a saved songpack
		if (!modConfig.loadedUserSongpack.isEmpty()) {
			LOGGER.info("Initialization is attempting to load user songpack.");
			for (var songpack : RMSongpackLoader.availableSongpacks) {
				if (songpack.config == null) continue;
				if (!songpack.config.name.equals(modConfig.loadedUserSongpack)) continue;

				// something is broken in this songpack, don't load it
				if (songpack.blockLoading)
					continue;

				ReactiveMusicCore.setActiveSongpack(songpack);
				loadedUserSongpack = true;

				break;
			}
		}

		// load the default one
		if (!loadedUserSongpack) {

			// for the cases where something is broken in the base songpack
			if (!RMSongpackLoader.availableSongpacks.get(0).blockLoading) {
				// first is the default songpack
				ReactiveMusicCore.setActiveSongpack(RMSongpackLoader.availableSongpacks.get(0));
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

							if (modConfig.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was already in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Added " + key + " to blacklist."));

							modConfig.blacklistedDimensions.add(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

				.then(ClientCommandManager.literal("unblacklistDimension")
						.executes(context -> {
							String key = context.getSource().getClient().world.getRegistryKey().getValue().toString();

							if (!modConfig.blacklistedDimensions.contains(key)) {
								context.getSource().sendFeedback(Text.literal("ReactiveMusic: " + key + " was not in blacklist."));
								return 1;
							}

							context.getSource().sendFeedback(Text.literal("ReactiveMusic: Removed " + key + " from blacklist."));

							modConfig.blacklistedDimensions.remove(key);
							ModConfig.saveConfig();

							return 1;
						})
				)

				.then(ClientCommandManager.literal("plugins")
						.executes(context -> {
							for (SongpackEventPlugin plugin : PLUGINS) {
								context.getSource().sendFeedback(Text.literal(plugin.getId()));
							}

							return 1;
						})
				)

				.then(ClientCommandManager.literal("skip")
						.executes(context -> {
							ReactiveMusicState.currentEntry = null;
							ReactiveMusicState.currentSong = null;
							
							return 1;
						})
				)

			)
		);
	}

	public static void newTick() {

		if (musicPlayer == null) return;
		if (ReactiveMusicState.currentSongpack == null) return;
		if (ReactiveMusicState.loadedEntries.isEmpty()) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc == null) return;


		// force a reasonable volume once on mod install, if you have full 100% everything it's way too loud
		if (!modConfig.hasForcedInitialVolume) {
			modConfig.hasForcedInitialVolume = true;
			ModConfig.saveConfig();

			if (mc.options.getSoundVolume(SoundCategory.MASTER) > 0.5) {

				LOGGER.info("Forcing master volume to a lower default, this will only happen once on mod-install to avoid loud defaults.");

				mc.options.getSoundVolumeOption(SoundCategory.MASTER).setValue(0.5);
				mc.options.write();
			}
		}
		
		{
			ReactiveMusicState.currentDimBlacklisted = false;

			// see if the dimension we're in is blacklisted -- update at same time as event map to keep them in sync
			if (mc != null && mc.world != null) {
				String curDim = mc.world.getRegistryKey().getValue().toString();

				for (String dim : modConfig.blacklistedDimensions) {
					if (dim.equals(curDim)) {
						ReactiveMusicState.currentDimBlacklisted = true;
						break;
					}
				}
			}

		}

		ReactiveMusicState.validEntries = ReactiveMusicCore.getValidEntries();

		if (!ReactiveMusicState.logicFreeze.get("ReactiveMusicCore")) {
			ReactiveMusicCore.newTick(audio().getByGroup("music"));
		}
		
		// TODO: Priority system for logic calls?
		SongPicker.tickEventMap(); // ticks after core audio, so that plugin logic happens later
		
		audio().tick();
		
		processTrackedSoundsMuteMusic();

		// Previously, this was in the core tick logic.
		// Extracted so that the core logic can be frozen, but onValid and onInvalid can still trigger.
		ReactiveMusicState.previousValidEntries = new java.util.ArrayList<>(ReactiveMusicState.validEntries);
        ReactiveMusicCore.processValidEvents(ReactiveMusicState.validEntries, ReactiveMusicState.previousValidEntries);
	}

	// TODO: Add querying foundSoundInstance from API
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



		// TODO: Add config parameter to RMPlayer to set the level to duck to.
		// TODO: Extract into ReactiveMusicCore
		// only duck for jukebox if our volume is loud enough to where it would matter
		if (foundSoundInstance) {
			musicPlayer.fade(0, 70);
		}
		else {
			musicPlayer.fade(1, 140);
		}
	}
}