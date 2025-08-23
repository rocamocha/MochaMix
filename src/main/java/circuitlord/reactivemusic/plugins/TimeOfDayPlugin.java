package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.songpack.SongpackEventType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class TimeOfDayPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Time of Day (Built-In)"; }
    private static SongpackEventType DAY, NIGHT, SUNSET, SUNRISE;

    @Override
    public void init() {
        DAY     = SongpackEventType.register("DAY");
        NIGHT   = SongpackEventType.register("NIGHT");
        SUNSET  = SongpackEventType.register("SUNSET");
        SUNRISE = SongpackEventType.register("SUNRISE");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
        if (player == null || world == null) return;

        long time = world.getTimeOfDay() % 24000L;
        boolean night   = (time >= 13000L && time < 23000L);
        boolean sunset  = (time >= 12000L && time < 13000L);
        boolean sunrise = (time >= 23000L); // mirrors your SongPicker logic

        eventMap.put(DAY,     !night);
        eventMap.put(NIGHT,    night);
        eventMap.put(SUNSET,   sunset);
        eventMap.put(SUNRISE,  sunrise);
    }
}