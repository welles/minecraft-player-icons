package dev.welles.playericons;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The positions of the other players in the receiver's dimension, sent by the server every {@link PlayerIcons#SEND_INTERVAL} ticks. */
public record PositionsPayload(List<Entry> players) implements CustomPacketPayload {
    public static final Type<PositionsPayload> TYPE = new Type<>(PlayerIcons.id("positions"));
    public static final StreamCodec<FriendlyByteBuf, PositionsPayload> CODEC = StreamCodec.ofMember(PositionsPayload::write, PositionsPayload::read);

    /** Longer than any player name, just a limit against broken packets. */
    private static final int MAX_NAME_LENGTH = 64;

    public record Entry(UUID uuid, String name, double x, double y, double z) {}

    private static PositionsPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Entry> players = new ArrayList<>(Math.min(count, 1024));
        for (int i = 0; i < count; i++) {
            players.add(new Entry(buf.readUUID(), buf.readUtf(MAX_NAME_LENGTH), buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }
        return new PositionsPayload(players);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(players.size());
        for (Entry player : players) {
            buf.writeUUID(player.uuid());
            buf.writeUtf(player.name(), MAX_NAME_LENGTH);
            buf.writeDouble(player.x());
            buf.writeDouble(player.y());
            buf.writeDouble(player.z());
        }
    }

    @Override
    public Type<PositionsPayload> type() {
        return TYPE;
    }
}
