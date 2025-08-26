package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class ProximityPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Proximity (Built-In)"; }
    private static SongpackEvent NEARBY_MOBS, VILLAGE;

    @Override public void init() {
        NEARBY_MOBS = SongpackEvent.register("NEARBY_MOBS");
        VILLAGE     = SongpackEvent.register("VILLAGE");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (player == null || world == null) return;

        // Nearby mobs
        var hostiles = ReactiveMusicUtils.getEntitiesInSphere(HostileEntity.class, player, 12.0, null);
        boolean mobsNearby = !hostiles.isEmpty();
        eventMap.put(NEARBY_MOBS, mobsNearby);

        // Village proximity (simple heuristic using VillageManager distance)
        var villagers = ReactiveMusicUtils.getEntitiesInSphere(VillagerEntity.class, player, 30.0, null);
        boolean inVillage = !villagers.isEmpty();
        eventMap.put(VILLAGE, inVillage);
    }
}