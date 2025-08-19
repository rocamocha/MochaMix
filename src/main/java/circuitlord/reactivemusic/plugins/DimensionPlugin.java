package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.SongpackEventType;
import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.SongPicker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

public final class DimensionPlugin implements SongpackEventPlugin {
    @Override public String getId() { return "Dimensions (Built-In)"; }
    private static SongpackEventType OVERWORLD, NETHER, END;

    @Override
    public void init() {
        OVERWORLD = SongpackEventType.register("OVERWORLD");
        NETHER    = SongpackEventType.register("NETHER");
        END       = SongpackEventType.register("END");
    }

    @Override
    public void gameTick(PlayerEntity player, World world, Map<SongpackEventType, Boolean> eventMap) {
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
