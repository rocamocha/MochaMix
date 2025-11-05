package rocamocha.lootsparkle.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

import rocamocha.lootsparkle.core.LootSparkle;
import rocamocha.lootsparkle.sparkle.SparkleManager;

/**
 * Network packets for sparkle synchronization between server and client
 */
public class SparkleNetworking {
    // Packet identifiers
    public static final CustomPayload.Id<SyncSparklePacket> SYNC_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "sync_sparkle"));
    public static final CustomPayload.Id<RemoveSparklePacket> REMOVE_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "remove_sparkle"));
    public static final CustomPayload.Id<InteractSparklePacket> INTERACT_SPARKLE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "interact_sparkle"));
    public static final CustomPayload.Id<InteractionFailedPacket> INTERACTION_FAILED = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "interaction_failed"));
    public static final CustomPayload.Id<SyncSpawnedMobsPacket> SYNC_SPAWNED_MOBS = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "sync_spawned_mobs"));
    public static final CustomPayload.Id<SyncTimerPacket> SYNC_TIMER = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "sync_timer"));
    public static final CustomPayload.Id<PhaseMessagePacket> PHASE_MESSAGE = new CustomPayload.Id<>(Identifier.of(LootSparkle.MOD_ID, "phase_message"));

    public static void initialize() {

        // Register packet codecs
        registerCodecs();

        // Register packet receivers
        ServerPlayNetworking.registerGlobalReceiver(INTERACT_SPARKLE, (packet, context) -> {
            UUID sparkleId = packet.sparkleId();
            context.server().execute(() -> {
                SparkleManager.triggerSparkleInteraction(context.player(), sparkleId);
            });
        });
    }

    public static void registerClientCodecs() {

        // Register codecs for packets the client receives (S2C) and sends (C2S)
        try {
            PayloadTypeRegistry.playS2C().register(SYNC_SPARKLE, SyncSparklePacket.CODEC);
            PayloadTypeRegistry.playS2C().register(REMOVE_SPARKLE, RemoveSparklePacket.CODEC);
            PayloadTypeRegistry.playS2C().register(INTERACTION_FAILED, InteractionFailedPacket.CODEC);
            PayloadTypeRegistry.playC2S().register(INTERACT_SPARKLE, InteractSparklePacket.CODEC);
            PayloadTypeRegistry.playS2C().register(SYNC_SPAWNED_MOBS, SyncSpawnedMobsPacket.CODEC);
            PayloadTypeRegistry.playS2C().register(SYNC_TIMER, SyncTimerPacket.CODEC);
            PayloadTypeRegistry.playS2C().register(PHASE_MESSAGE, PhaseMessagePacket.CODEC);
        } catch (IllegalArgumentException e) {
            // Codecs already registered, ignore
        }
    }

    private static void registerCodecs() {
        PayloadTypeRegistry.playS2C().register(SYNC_SPARKLE, SyncSparklePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(REMOVE_SPARKLE, RemoveSparklePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(INTERACTION_FAILED, InteractionFailedPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(INTERACT_SPARKLE, InteractSparklePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SYNC_SPAWNED_MOBS, SyncSpawnedMobsPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(SYNC_TIMER, SyncTimerPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PHASE_MESSAGE, PhaseMessagePacket.CODEC);
    }

    /**
     * Packet sent from server to client to sync a sparkle
     */
    public record SyncSparklePacket(UUID sparkleId, UUID playerId, BlockPos position, int tierLevel, String phaseMessage) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, SyncSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, SyncSparklePacket::sparkleId,
            UUID_CODEC, SyncSparklePacket::playerId,
            BlockPos.PACKET_CODEC, SyncSparklePacket::position,
            PacketCodecs.INTEGER, SyncSparklePacket::tierLevel,
            STRING_OR_NULL_CODEC, SyncSparklePacket::phaseMessage,
            SyncSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_SPARKLE;
        }
    }

    /**
     * Packet sent from server to client to remove a sparkle
     */
    public record RemoveSparklePacket(UUID sparkleId, UUID playerId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, RemoveSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, RemoveSparklePacket::sparkleId,
            UUID_CODEC, RemoveSparklePacket::playerId,
            RemoveSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return REMOVE_SPARKLE;
        }
    }

    /**
     * Packet sent from client to server to interact with a sparkle
     */
    public record InteractSparklePacket(UUID sparkleId) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, InteractSparklePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, InteractSparklePacket::sparkleId,
            InteractSparklePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return INTERACT_SPARKLE;
        }
    }

    /**
     * Packet sent from server to client when sparkle interaction fails
     */
    public record InteractionFailedPacket(String reason) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, InteractionFailedPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, InteractionFailedPacket::reason,
            InteractionFailedPacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return INTERACTION_FAILED;
        }
    }

    /**
     * Packet sent from server to client to sync spawned mobs for a hostile sparkle
     */
    public record SyncSpawnedMobsPacket(UUID sparkleId, UUID playerId, java.util.List<UUID> mobIds) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, SyncSpawnedMobsPacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, SyncSpawnedMobsPacket::sparkleId,
            UUID_CODEC, SyncSpawnedMobsPacket::playerId,
            PacketCodecs.collection(java.util.ArrayList::new, UUID_CODEC), SyncSpawnedMobsPacket::mobIds,
            SyncSpawnedMobsPacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_SPAWNED_MOBS;
        }
    }

    /**
     * Packet sent from server to client to sync timer display for a hostile sparkle
     */
    public record SyncTimerPacket(UUID sparkleId, UUID playerId, boolean showTimer, long endTimeMs, boolean isPreparation) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, SyncTimerPacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, SyncTimerPacket::sparkleId,
            UUID_CODEC, SyncTimerPacket::playerId,
            PacketCodecs.BOOL, SyncTimerPacket::showTimer,
            PacketCodecs.VAR_LONG, SyncTimerPacket::endTimeMs,
            PacketCodecs.BOOL, SyncTimerPacket::isPreparation,
            SyncTimerPacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return SYNC_TIMER;
        }
    }

    /**
     * Packet sent from server to client to display phase start messages on HUD
     */
    public record PhaseMessagePacket(UUID sparkleId, UUID playerId, String message) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, PhaseMessagePacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, PhaseMessagePacket::sparkleId,
            UUID_CODEC, PhaseMessagePacket::playerId,
            STRING_OR_NULL_CODEC, PhaseMessagePacket::message,
            PhaseMessagePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return PHASE_MESSAGE;
        }
    }

    /**
     * UUID codec using most/least significant bits, handles null values
     */
    private static final PacketCodec<PacketByteBuf, UUID> UUID_CODEC = new PacketCodec<PacketByteBuf, UUID>() {
        @Override
        public UUID decode(PacketByteBuf buf) {
            boolean isNull = buf.readBoolean();
            if (isNull) {
                return null;
            }
            return new UUID(buf.readLong(), buf.readLong());
        }

        @Override
        public void encode(PacketByteBuf buf, UUID value) {
            if (value == null) {
                buf.writeBoolean(true);
            } else {
                buf.writeBoolean(false);
                buf.writeLong(value.getMostSignificantBits());
                buf.writeLong(value.getLeastSignificantBits());
            }
        }
    };

    /**
     * String codec that handles null values
     */
    private static final PacketCodec<PacketByteBuf, String> STRING_OR_NULL_CODEC = new PacketCodec<PacketByteBuf, String>() {
        @Override
        public String decode(PacketByteBuf buf) {
            boolean isNull = buf.readBoolean();
            if (isNull) {
                return null;
            }
            return buf.readString();
        }

        @Override
        public void encode(PacketByteBuf buf, String value) {
            if (value == null) {
                buf.writeBoolean(true);
            } else {
                buf.writeBoolean(false);
                buf.writeString(value);
            }
        }
    };
}
