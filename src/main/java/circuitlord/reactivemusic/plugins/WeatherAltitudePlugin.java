package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.songpack.SongpackEventType;
import circuitlord.reactivemusic.api.ReactiveMusicUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;

public final class WeatherAltitudePlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Weather & Altitude (Built-In)"; }
    private static SongpackEventType RAIN, SNOW, STORM, UNDERWATER, UNDERGROUND, DEEP_UNDERGROUND, HIGH_UP;

    @Override public void init() {
        RAIN = SongpackEventType.register("RAIN");
        SNOW = SongpackEventType.register("SNOW");
        STORM = SongpackEventType.register("STORM");
        UNDERWATER = SongpackEventType.register("UNDERWATER");
        UNDERGROUND = SongpackEventType.register("UNDERGROUND");
        DEEP_UNDERGROUND = SongpackEventType.register("DEEP_UNDERGROUND");
        HIGH_UP = SongpackEventType.register("HIGH_UP");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
        if (player == null || world == null) return;
        BlockPos pos = player.getBlockPos();

        eventMap.put(STORM, ReactiveMusicUtils.isStorm(world));
        eventMap.put(RAIN, ReactiveMusicUtils.isRainingAt(world, pos));
        eventMap.put(SNOW, ReactiveMusicUtils.isSnowingAt(world, pos));
        eventMap.put(UNDERWATER, player.isSubmergedInWater());
        eventMap.put(UNDERGROUND, ReactiveMusicUtils.isUnderground(world, pos, 55));
        eventMap.put(DEEP_UNDERGROUND, ReactiveMusicUtils.isDeepUnderground(world, pos, 15));
        eventMap.put(HIGH_UP, ReactiveMusicUtils.isHighUp(pos, 128));
    }
}

