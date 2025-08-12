package circuitlord.reactivemusic.api;

import circuitlord.reactivemusic.SongpackEventType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public interface SongpackEventPlugin {
    void register();
    // Plugins add per-tick results keyed by SongpackEventType.
    void tick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> map);
}

