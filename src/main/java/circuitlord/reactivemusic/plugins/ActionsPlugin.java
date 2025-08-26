package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ActionsPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Actions (Built-In)"; }
    Logger LOGGER = LoggerFactory.getLogger(getId());

    private static SongpackEvent FISHING, MINECART, BOAT, HORSE, PIG;

    @Override public void init() {
        LOGGER.info("Initializing " + getId() + " songpack event plugin");

        FISHING = SongpackEvent.register("FISHING");
        MINECART = SongpackEvent.register("MINECART");
        BOAT = SongpackEvent.register("BOAT");
        HORSE = SongpackEvent.register("HORSE");
        PIG = SongpackEvent.register("PIG");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (player == null) return;

        eventMap.put(FISHING, player.fishHook != null);

        Entity v = player.getVehicle();
        eventMap.put(MINECART, v instanceof MinecartEntity);
        eventMap.put(BOAT,     v instanceof BoatEntity);
        eventMap.put(HORSE,    v instanceof HorseEntity);
        eventMap.put(PIG,      v instanceof PigEntity);
    }
}