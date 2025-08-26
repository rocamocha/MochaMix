package circuitlord.reactivemusic;

import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import circuitlord.reactivemusic.songpack.RMSongpackEventState;
import circuitlord.reactivemusic.songpack.RMSongpackEvent;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
//import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.*;

import org.jetbrains.annotations.NotNull;

public final class SongPicker {

    static int pluginTickCounter = 0;

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;


    public static Map<String, Integer> blockCounterMap = new HashMap<>();
    public static Map<String, Integer> cachedBlockChecker = new HashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";

    public static final Field[] BIOME_TAG_FIELDS = ConventionalBiomeTags.class.getDeclaredFields();
    public static final List<TagKey<Biome>> BIOME_TAGS = new ArrayList<>();

    static {

        for (Field field : BIOME_TAG_FIELDS) {
            TagKey<Biome> biomeTag = getBiomeTagFromField(field);

            BIOME_TAGS.add(biomeTag);
            biomeTagEventMap.put(biomeTag, false);
        }
    }

    public static TagKey<Biome> getBiomeTagFromField(Field field) {
        if (field.getType() == TagKey.class) {
            try {
                @SuppressWarnings("unchecked")
                TagKey<Biome> tag = (TagKey<Biome>) field.get(null);
                return tag;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void tickEventMap() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null)
            return;

        ClientPlayerEntity player = mc.player;
        World world = mc.world;

        
        pluginTickCounter++;
        
        for (SongpackEventPlugin plugin : ReactiveMusic.PLUGINS) {
            
            if (ReactiveMusicState.logicFreeze.computeIfAbsent(plugin.getId(), k -> false)) {
                ReactiveMusicState.LOGGER.info("Skipping execution for " + plugin.getId());
                continue;
            }

            plugin.newTick();

            if (player == null || world == null) {
                continue;
            }
            
            
            // throttled tick
            int interval = plugin.tickSchedule();
            if (interval <= 1 || (pluginTickCounter % interval) == 0L) {
                plugin.gameTick(player, world, ReactiveMusicState.songpackEventMap);              
            }
        }

        if (player == null || world == null) initialize();

        ReactiveMusicState.songpackEventMap.put(RMSongpackEvent.GENERIC, true);
        ReactiveMusicState.songpackEventMap.put(RMSongpackEvent.MAIN_MENU, player == null || world == null);
        ReactiveMusicState.songpackEventMap.put(RMSongpackEvent.CREDITS, mc.currentScreen instanceof CreditsScreen);
        
        /**
         * Not implemented yet.
         * @see RMSongpackEventState
         */
        RMSongpackEventState.updateForPlayer(player, ReactiveMusicState.songpackEventMap);
    }

    public static void initialize() {
        // build string -> type map from the internal registry
        ReactiveMusicState.songpackEventMap.clear();
        for (RMSongpackEvent set : RMSongpackEvent.values()) {
            ReactiveMusicState.songpackEventMap.put(set, false);
        }
    }

    //----------------------------------------------------------------------------------------
    public static boolean isEntryValid(RMRuntimeEntry entry) {

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;
            for (RMSongpackEvent songpackEvent : condition.songpackEvents) {
                if (ReactiveMusicState.songpackEventMap.containsKey(songpackEvent) && ReactiveMusicState.songpackEventMap.get(songpackEvent)) {
                    songpackEventsValid = true;
                    break;
                }
            }

            boolean blocksValid = false;
            for (var blockCond : condition.blocks) {
                for (var kvp : cachedBlockChecker.entrySet()) {
                    if (kvp.getKey().contains(blockCond.block) && kvp.getValue() >= blockCond.requiredCount) {
                        blocksValid = true;
                        break;
                    }
                }
            }

            boolean biomeTypesValid = false;
            for (var biome : condition.biomeTypes) {
                if (currentBiomeName.contains(biome)) {
                    biomeTypesValid = true;
                    break;
                }
            }

            boolean biomeTagsValid = false;
            for (var biomeTag : condition.biomeTags) {
                if (biomeTagEventMap.containsKey(biomeTag) && biomeTagEventMap.get(biomeTag)) {
                    biomeTagsValid = true;
                    break;
                }
            }

            boolean dimsValid = false;
            for (var dim : condition.dimTypes) {
                if (currentDimName.contains(dim)) {
                    dimsValid = true;
                    break;
                }
            }


            if (!songpackEventsValid && !biomeTypesValid && !biomeTagsValid && !dimsValid && !blocksValid) {
                // none of the OR conditions were valid on this condition, return false
                return false;
            }

        }

        // we passed without failing so it must be true
        return true;
        
    }

    public static @NotNull List<String> getSelectedSongs(RMRuntimeEntry newEntry, List<RMRuntimeEntry> validEntries) {
		// if we have non-recent songs then just return those
		if (ReactiveMusicUtils.hasSongNotPlayedRecently(newEntry.songs)) {
			return newEntry.songs;
		}
		// Fallback behaviour
		if (newEntry.allowFallback) {
			for (int i = 1; i < ReactiveMusicState.validEntries.size(); i++) {
				if (ReactiveMusicState.validEntries.get(i) == null)
					continue;
				// check if we have songs not played recently and early out
				if (ReactiveMusicUtils.hasSongNotPlayedRecently(ReactiveMusicState.validEntries.get(i).songs)) {
					return ReactiveMusicState.validEntries.get(i).songs;
				}
			}
		}
		// we've played everything recently, just give up and return this event's songs
		return newEntry.songs;
	}
}
