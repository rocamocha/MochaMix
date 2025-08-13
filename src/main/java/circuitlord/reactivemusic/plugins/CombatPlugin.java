package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.api.SongpackEventPlugin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class CombatPlugin implements SongpackEventPlugin {

    private static SongpackEventType DYING;
    private static final float THRESHOLD = 0.35f;

    @Override
    public void register() {
        DYING = SongpackEventType.register("DYING");
    }

    @Override
    public void tick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
        if (player == null || world == null) return;
        boolean dying = (player.getHealth() / player.getMaxHealth()) < THRESHOLD;
        eventMap.put(DYING, dying);
    }
}
