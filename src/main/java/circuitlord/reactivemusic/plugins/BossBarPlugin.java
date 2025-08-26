package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class BossBarPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Boss Bar (Built-In)"; }
    private static SongpackEvent BOSS;

    @Override public void init() {
        BOSS = SongpackEvent.register("BOSS");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (BOSS == null) return;
        boolean active = ReactiveMusicUtils.isBossBarActive();
        eventMap.put(BOSS, active);
    }
}

