package rocamocha.logicalloadouts.network.packets;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import rocamocha.logicalloadouts.LogicalLoadouts;
import rocamocha.logicalloadouts.data.Loadout;

public record ApplyLocalLoadoutPayload(Loadout loadout, boolean consumeAfterApply) implements CustomPayload {
    public static final CustomPayload.Id<ApplyLocalLoadoutPayload> ID = new CustomPayload.Id<>(
        Identifier.of(LogicalLoadouts.MOD_ID, "apply_local_loadout")
    );
    
    public static final PacketCodec<RegistryByteBuf, ApplyLocalLoadoutPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            PacketCodecs.NBT_COMPOUND.encode(buf, value.loadout.toNbt(buf.getRegistryManager()));
            PacketCodecs.BOOL.encode(buf, value.consumeAfterApply);
        },
        buf -> {
            var nbt = PacketCodecs.NBT_COMPOUND.decode(buf);
            var consume = PacketCodecs.BOOL.decode(buf);
            return new ApplyLocalLoadoutPayload(Loadout.fromNbt(buf.getRegistryManager(), nbt), consume);
        }
    );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}