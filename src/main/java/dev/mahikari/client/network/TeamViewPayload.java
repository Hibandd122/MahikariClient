package dev.mahikari.client.network;

import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record TeamViewPayload(byte[] data) implements CustomPayload
{
    public static final CustomPayload.Id<TeamViewPayload> ID = new CustomPayload.Id(Identifier.of("newgen", "teamview2"));
    public static final PacketCodec<RegistryByteBuf, TeamViewPayload> CODEC = PacketCodec.of((payload, buf) -> buf.writeBytes(payload.data()), buf -> {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new TeamViewPayload(bytes);
    });

    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
