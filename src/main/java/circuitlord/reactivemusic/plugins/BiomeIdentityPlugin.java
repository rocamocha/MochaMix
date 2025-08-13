package circuitlord.reactivemusic.plugins;

import circuitlord.reactivemusic.api.SongpackEventPlugin;
import circuitlord.reactivemusic.SongPicker;
import circuitlord.reactivemusic.SongpackEventType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.registry.entry.RegistryEntry;

public final class BiomeIdentityPlugin implements SongpackEventPlugin {
    @Override public void register() { /* no-op */ }

    @Override
    public void tick(PlayerEntity player, World world, java.util.Map<SongpackEventType, Boolean> out) {
        if (player == null || world == null) return;

        BlockPos pos = player.getBlockPos();
        RegistryEntry<Biome> entry = world.getBiome(pos);

        // Mirror SongPicker’s original assignment of currentBiomeName
        String name = entry.getKey()
                .map(k -> k.getValue().toString())
                .orElse("[unregistered]");
        SongPicker.currentBiomeName = name; // isEntryValid() uses this
    }
}

