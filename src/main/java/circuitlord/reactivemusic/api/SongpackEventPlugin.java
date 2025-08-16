package circuitlord.reactivemusic.api;

import circuitlord.reactivemusic.SongpackEventType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public interface SongpackEventPlugin {
    // default tick throttling interval, configurable through override
    default int tickInterval() { return 20; }

    // Plugins register event ids as SongpackEventType
    void register();

    // Plugins add per-tick results keyed by SongpackEventType.
    void tick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> map);
}

