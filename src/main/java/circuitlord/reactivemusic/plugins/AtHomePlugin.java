package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.ReactiveMusic;
import circuitlord.reactivemusic.api.*;
import circuitlord.reactivemusic.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Map;

public final class AtHomePlugin implements SongpackEventPlugin {
    @Override public String getId() { return "At Home (Built-In)"; }

    private static final float RADIUS = 45.0f;

    // Plugin-local state; no more SongPicker.wasSleeping
    private static boolean wasSleeping = false;

    // Event handles
    private static SongpackEvent HOME;
    private static SongpackEvent HOME_OVERWORLD;
    private static SongpackEvent HOME_NETHER;
    private static SongpackEvent HOME_END;

    @Override
    public void init() {
        HOME = SongpackEvent.register("HOME");
        HOME_OVERWORLD = SongpackEvent.register("HOME_OVERWORLD");
        HOME_NETHER = SongpackEvent.register("HOME_NETHER");
        HOME_END = SongpackEvent.register("HOME_END");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (player == null || world == null) return;

        // Keys: base (per save/server), and per-dimension
        String baseKey = computeBaseWorldKey();
        String dimPath = world.getRegistryKey().getValue().getPath(); // overworld | the_nether | the_end | ...
        String dimKey = baseKey + "_" + dimPath;

        // On sleep edge, save both base and dimension-specific homes
        if (!wasSleeping && player.isSleeping()) {
            var pos = player.getPos();
            ReactiveMusic.modConfig.savedHomePositions.put(baseKey, pos);
            ReactiveMusic.modConfig.savedHomePositions.put(dimKey, pos);
            ModConfig.saveConfig();
        }
        wasSleeping = player.isSleeping();

        // Emit base HOME (per save/server, regardless of dimension)
        eventMap.put(HOME, isWithinHome(world, player, baseKey));

        // Emit one of the three dimension-specific events (only for vanilla dims)
        Identifier dimId = world.getRegistryKey().getValue();
        if (dimId.equals(World.OVERWORLD.getValue())) {
            eventMap.put(HOME_OVERWORLD, isWithinHome(world, player, dimKey));
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, false);
        } else if (dimId.equals(World.NETHER.getValue())) {
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, isWithinHome(world, player, dimKey));
            eventMap.put(HOME_END, false);
        } else if (dimId.equals(World.END.getValue())) {
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, isWithinHome(world, player, dimKey));
        } else {
            // Non-vanilla dimension: keep the three vanilla-specific flags false
            eventMap.put(HOME_OVERWORLD, false);
            eventMap.put(HOME_NETHER, false);
            eventMap.put(HOME_END, false);
        }
    }

    // --- helpers ---

    private static boolean isWithinHome(World world, PlayerEntity player, String key) {
        var map = ReactiveMusic.modConfig.savedHomePositions;
        if (!map.containsKey(key)) return false;
        Vec3d dist = player.getPos().subtract(map.get(key));
        return dist.length() < RADIUS;
    }

    /** Per-save (singleplayer) or per-server (multiplayer) identifier — no dimension. */
    private static String computeBaseWorldKey() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            if (mc.isInSingleplayer() && mc.getServer() != null && mc.getServer().getSaveProperties() != null) {
                // Singleplayer: user-facing save name (from level.dat)
                String pretty = mc.getServer().getSaveProperties().getLevelName();
                if (pretty != null && !pretty.isBlank()) return pretty;
            } else {
                // Multiplayer: server list entry (client-side safe)
                ServerInfo entry = mc.getCurrentServerEntry();
                if (entry != null) {
                    if (entry.name != null && !entry.name.isBlank()) return entry.name;
                    if (entry.address != null && !entry.address.isBlank()) return entry.address;
                }
            }
        }
        return "unknown_world";
    }
}