package circuitlord.reactivemusic;


import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.entries.RMRuntimeEntry;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.CreditsScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;

//import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.reflect.Field;
import java.util.*;

public final class SongPicker {

    static int pluginTickCounter = 0;

    public static Map<SongpackEventType, Boolean> songpackEventMap = new HashMap<>();

    // Cache the loader so we don’t rescan every tick
    private static final ServiceLoader<SongpackEventPlugin> PLUGINS = ServiceLoader.load(SongpackEventPlugin.class);

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();

    public static boolean queuedToPrintBlockCounter = false;
    public static BlockPos cachedBlockCounterOrigin;


    public static Map<String, Integer> blockCounterMap = new HashMap<>();
    public static Map<String, Integer> cachedBlockChecker = new HashMap<>();

    public static String currentBiomeName = "";
    public static String currentDimName = "";


    private static final Random rand = new Random();

    private static List<String> recentlyPickedSongs = new ArrayList<>();

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

        songpackEventMap.put(SongpackEventType.GENERIC, true);
        songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
        songpackEventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);

        // Early out if not in-game
        if (player == null || world == null)
            return;

        pluginTickCounter++;

        // Throttled plugin loop:
        for (SongpackEventPlugin p : PLUGINS) {
            int interval = p.tickInterval();
            if (interval <= 1 || (pluginTickCounter % interval) == 0L) {
                p.tick(player, world, songpackEventMap);
            }
        }
    }

    public static void initialize() {
        // build string -> type map from the internal registry
        songpackEventMap.clear();
        for (var plugin : PLUGINS) plugin.register();
        for (SongpackEventType set : SongpackEventType.values()) {
            songpackEventMap.put(set, false);
        }
    }




    private static final List<SongpackEntry> reusableValidEntries = new ArrayList<>();


/*    public static List<SongpackEntry> getAllValidEntries() {

        reusableValidEntries.clear();

        for (int i = 0; i < SongLoader.activeSongpack.entries.length; i++) {

            SongpackEntry entry = SongLoader.activeSongpack.entries[i];
            if (entry == null) continue;

            boolean eventsMet = true;

            for (SongpackEventType songpackEvent : entry.songpackEvents) {

                if (!songpackEventMap.containsKey(songpackEvent))
                    continue;

                if (!songpackEventMap.get(songpackEvent)) {
                    eventsMet = false;
                    break;
                }
            }

            for (TagKey<Biome> biomeTagEvent : entry.biomeTagEvents) {

                if (!biomeTagEventMap.containsKey(biomeTagEvent))
                    continue;

                if (!biomeTagEventMap.get(biomeTagEvent)) {
                    eventsMet = false;
                    break;
                }
            }

            if (eventsMet) {
                reusableValidEntries.add(entry);
            }
        }

        return reusableValidEntries;
    }*/


    static boolean hasSongNotPlayedRecently(List<String> songs) {
        for (String song : songs) {
            if (!recentlyPickedSongs.contains(song)) {
                return true;
            }
        }
        return false;
    }


    static List<String> getNotRecentlyPlayedSongs(String[] songs) {
        List<String> notRecentlyPlayed = new ArrayList<>(Arrays.asList(songs));
        notRecentlyPlayed.removeAll(recentlyPickedSongs);
        return notRecentlyPlayed;
    }


    static String pickRandomSong(List<String> songs) {

        if (songs.isEmpty()) {
            return null;
        }

        List<String> cleanedSongs = new ArrayList<>(songs);

        cleanedSongs.removeAll(recentlyPickedSongs);


        String picked;

        // If there's remaining songs, pick one of those
        if (!cleanedSongs.isEmpty()) {
            int randomIndex = rand.nextInt(cleanedSongs.size());
            picked = cleanedSongs.get(randomIndex);
        }

        // Else we've played all these recently so just pick a new random one
        else {
            int randomIndex = rand.nextInt(songs.size());
            picked = songs.get(randomIndex);
        }


        // only track the past X songs
        if (recentlyPickedSongs.size() >= 8) {
            recentlyPickedSongs.remove(0);
        }

        recentlyPickedSongs.add(picked);


        return picked;
    }


    public static String getSongName(String song) {
        return song == null ? "" : song.replaceAll("([^A-Z])([A-Z])", "$1 $2");
    }

    //----------------------------------------------------------------------------------------
    public static boolean isEntryValid(RMRuntimeEntry entry) {

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;
            for (SongpackEventType songpackEvent : condition.songpackEvents) {
                if (songpackEventMap.containsKey(songpackEvent) && songpackEventMap.get(songpackEvent)) {
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









}
