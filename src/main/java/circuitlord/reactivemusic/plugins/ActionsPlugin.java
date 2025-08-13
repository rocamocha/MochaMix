package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.api.SongpackEventPlugin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class ActionsPlugin implements SongpackEventPlugin {
    private static SongpackEventType FISHING, MINECART, BOAT, HORSE, PIG;

    @Override public void register() {
        FISHING = SongpackEventType.register("FISHING");
        MINECART = SongpackEventType.register("MINECART");
        BOAT = SongpackEventType.register("BOAT");
        HORSE = SongpackEventType.register("HORSE");
        PIG = SongpackEventType.register("PIG");
    }

    @Override
    public void tick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
        if (player == null) return;

        eventMap.put(FISHING, player.fishHook != null);

        Entity v = player.getVehicle();
        eventMap.put(MINECART, v instanceof MinecartEntity);
        eventMap.put(BOAT,     v instanceof BoatEntity);
        eventMap.put(HORSE,    v instanceof HorseEntity);
        eventMap.put(PIG,      v instanceof PigEntity);
    }
}