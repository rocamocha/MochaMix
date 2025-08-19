package circuitlord.reactivemusic;

import circuitlord.reactivemusic.api.*;
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

    public static Map<TagKey<Biome>, Boolean> biomeTagEventMap = new HashMap<>();

    public static Map<Entity, Long> recentEntityDamageSources = new HashMap<>();

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
            
            if (ReactiveMusicAPI.logicFreeze.computeIfAbsent(plugin.getId(), k -> false)) {
                ReactiveMusicAPI.LOGGER.info("Skipping execution for " + plugin.getId());
                continue;
            }

            plugin.newTick();

            if (player == null || world == null) {
                continue;
            }
            
            
            // throttled tick
            int interval = plugin.tickSchedule();
            if (interval <= 1 || (pluginTickCounter % interval) == 0L) {
                plugin.gameTick(player, world, ReactiveMusicAPI.songpackEventMap);              
            }
        }

        if (player == null || world == null) initialize();

        ReactiveMusicAPI.songpackEventMap.put(SongpackEventType.GENERIC, true);
        ReactiveMusicAPI.songpackEventMap.put(SongpackEventType.MAIN_MENU, player == null || world == null);
        ReactiveMusicAPI.songpackEventMap.put(SongpackEventType.CREDITS, mc.currentScreen instanceof CreditsScreen);
        
        SongpackEventState.updateForPlayer(player, ReactiveMusicAPI.songpackEventMap);
    }

    public static void initialize() {
        // build string -> type map from the internal registry
        ReactiveMusicAPI.songpackEventMap.clear();
        for (SongpackEventType set : SongpackEventType.values()) {
            ReactiveMusicAPI.songpackEventMap.put(set, false);
        }
    }

    //----------------------------------------------------------------------------------------
    public static boolean isEntryValid(RMRuntimeEntry entry) {

        for (var condition : entry.conditions) {

            // each condition functions as an OR, if at least one of them is true then the condition is true


            boolean songpackEventsValid = false;
            for (SongpackEventType songpackEvent : condition.songpackEvents) {
                if (ReactiveMusicAPI.songpackEventMap.containsKey(songpackEvent) && ReactiveMusicAPI.songpackEventMap.get(songpackEvent)) {
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
