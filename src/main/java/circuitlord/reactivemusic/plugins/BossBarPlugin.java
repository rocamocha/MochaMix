package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.songpack.SongpackEventType;
import circuitlord.reactivemusic.api.ReactiveMusicUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class BossBarPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Boss Bar (Built-In)"; }
    private static SongpackEventType BOSS;

    @Override public void init() {
        BOSS = SongpackEventType.register("BOSS");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
        if (BOSS == null) return;
        boolean active = ReactiveMusicUtils.isBossBarActive();
        eventMap.put(BOSS, active);
    }
}

