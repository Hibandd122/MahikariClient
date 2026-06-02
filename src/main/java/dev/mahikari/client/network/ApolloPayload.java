package dev.mahikari.client.network;

import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

/**
 * Payload for the Apollo (LunarClient) channel used by Hoplite server.
 * Channel: lunar:apollo
 * Data format: raw protobuf bytes (com.google.protobuf.Any wrapping team messages)
 */
public record ApolloPayload(byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<ApolloPayload> ID =
            new CustomPayload.Id<>(Identifier.of("lunar", "apollo"));

    public static final PacketCodec<RegistryByteBuf, ApolloPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeBytes(payload.data()),
                    buf -> {
                        byte[] bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                        return new ApolloPayload(bytes);
                    }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
