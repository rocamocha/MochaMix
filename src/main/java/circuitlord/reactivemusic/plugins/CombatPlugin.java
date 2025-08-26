package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class CombatPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Combat (Built-In)"; }

    private static SongpackEvent DYING;
    private static final float THRESHOLD = 0.35f;

    @Override
    public void init() {
        DYING = SongpackEvent.register("DYING");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (player == null || world == null) return;
        boolean dying = (player.getHealth() / player.getMaxHealth()) < THRESHOLD;
        eventMap.put(DYING, dying);
    }
}
