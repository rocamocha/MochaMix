package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;

public record CreateLoadoutFromDataPayload(Loadout loadout) implements CustomPayload {
    public static final CustomPayload.Id<CreateLoadoutFromDataPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "create_loadout_from_data")
    );
    
    public static final PacketCodec<RegistryByteBuf, CreateLoadoutFromDataPayload> CODEC = PacketCodec.of(
        (value, buf) -> PacketCodecs.NBT_COMPOUND.encode(buf, value.loadout.toNbt(buf.getRegistryManager())),
        buf -> new CreateLoadoutFromDataPayload(Loadout.fromNbt(buf.getRegistryManager(), PacketCodecs.NBT_COMPOUND.decode(buf)))
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}