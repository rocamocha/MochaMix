package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class TimeOfDayPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Time of Day (Built-In)"; }
    private static SongpackEvent DAY, NIGHT, SUNSET, SUNRISE;

    @Override
    public void init() {
        DAY     = SongpackEvent.register("DAY");
        NIGHT   = SongpackEvent.register("NIGHT");
        SUNSET  = SongpackEvent.register("SUNSET");
        SUNRISE = SongpackEvent.register("SUNRISE");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
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