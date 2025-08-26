package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class DimensionPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Dimensions (Built-In)"; }
    private static SongpackEvent OVERWORLD, NETHER, END;

    @Override
    public void init() {
        OVERWORLD = SongpackEvent.register("OVERWORLD");
        NETHER    = SongpackEvent.register("NETHER");
        END       = SongpackEvent.register("END");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEvent, Boolean> eventMap) {
        if (world == null) return;

        var indimension = world.getRegistryKey();
        SongPicker.currentDimName = indimension.getValue().toString();

        boolean isOverworld = indimension == World.OVERWORLD;
        boolean isNether    = indimension == World.NETHER;
        boolean isEnd       = indimension == World.END;

        eventMap.put(OVERWORLD, isOverworld);
        eventMap.put(NETHER,    isNether);
        eventMap.put(END,       isEnd);
    }
}
