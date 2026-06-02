package dev.mahikari.client.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Apollo (LunarClient) networking handler for Hoplite server compatibility.
 * Listens on the "lunar:apollo" channel for protobuf-encoded team data,
 * decodes it, and feeds it into the shared TeamViewManager.
 */
public final class ApolloNetworking {
    private static final Logger LOGGER = LoggerFactory.getLogger("mahikari-client-apollo");

    public static void registerClient() {
        try {
            PayloadTypeRegistry.playS2C().register(ApolloPayload.ID, ApolloPayload.CODEC);
        } catch (IllegalArgumentException e) {
            // Already registered (e.g. another mod uses same channel)
            LOGGER.warn("[Mahikari] Apollo channel already registered, skipping: {}", e.getMessage());
        }

        ClientPlayNetworking.registerGlobalReceiver(ApolloPayload.ID, (payload, context) -> {
            context.client().execute(() -> handleApolloPayload(payload.data()));
        });
        // JOIN/DISCONNECT clearAll() is handled by TeamViewNetworking
    }

    private static void handleApolloPayload(byte[] data) {
        try {
            Any any = Any.parseFrom(data);
            handleApolloMessage(any);
        } catch (Exception e) {
            LOGGER.debug("[Mahikari] Failed to parse Apollo payload: {}", e.getMessage());
        }
    }

    private static void handleApolloMessage(Any any) {
        try {
            if (any.is(lunarclient.apollo.team.v1.Schema.UpdateTeamMembersMessage.class)) {
                lunarclient.apollo.team.v1.Schema.UpdateTeamMembersMessage msg =
                        any.unpack(lunarclient.apollo.team.v1.Schema.UpdateTeamMembersMessage.class);
                handleUpdate(msg);
            } else if (any.is(lunarclient.apollo.team.v1.Schema.ResetTeamMembersMessage.class)) {
                handleReset();
            } else if (any.getTypeUrl().endsWith("OverrideGlowMessage")) {
                handleGlow(any.getValue().toByteArray());
            }
        } catch (Exception e) {
            LOGGER.debug("[Mahikari] Ignored Apollo packet: {}", e.getMessage());
        }
    }

    private static void handleGlow(byte[] data) {
        try {
            com.google.protobuf.CodedInputStream in = com.google.protobuf.CodedInputStream.newInstance(data);
            UUID targetUuid = null;
            java.awt.Color glowColor = null;
            
            while (!in.isAtEnd()) {
                int tag = in.readTag();
                if (tag == 0) break;
                int fieldNum = com.google.protobuf.WireFormat.getTagFieldNumber(tag);
                
                if (fieldNum == 1) { // target Uuid
                    int length = in.readRawVarint32();
                    int oldLimit = in.pushLimit(length);
                    long most = 0, least = 0;
                    while (!in.isAtEnd()) {
                        int subTag = in.readTag();
                        if (subTag == 0) break;
                        int subField = com.google.protobuf.WireFormat.getTagFieldNumber(subTag);
                        if (subField == 1) most = in.readInt64();
                        else if (subField == 2) least = in.readInt64();
                        else in.skipField(subTag);
                    }
                    in.popLimit(oldLimit);
                    targetUuid = new UUID(most, least);
                } else if (fieldNum == 2) { // Color
                    int length = in.readRawVarint32();
                    int oldLimit = in.pushLimit(length);
                    while (!in.isAtEnd()) {
                        int subTag = in.readTag();
                        if (subTag == 0) break;
                        int subField = com.google.protobuf.WireFormat.getTagFieldNumber(subTag);
                        if (subField == 1) {
                            String hex = in.readString();
                            if (hex.startsWith("#")) {
                                try {
                                    glowColor = java.awt.Color.decode(hex);
                                } catch (Exception ignored) {}
                            }
                        } else {
                            in.skipField(subTag);
                        }
                    }
                    in.popLimit(oldLimit);
                } else {
                    in.skipField(tag);
                }
            }
            
            if (targetUuid != null && glowColor != null) {
                TeamViewManager.TeammateData td = MahikariClient.MANAGER.findByUuid(targetUuid);
                if (td != null) {
                    td.updateApolloColor(glowColor);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[Mahikari] Failed to parse OverrideGlowMessage: {}", e.getMessage());
        }
    }

    private static void handleUpdate(lunarclient.apollo.team.v1.Schema.UpdateTeamMembersMessage msg) {
        long now = System.currentTimeMillis();
        TeamViewManager manager = MahikariClient.MANAGER;
        MinecraftClient mc = MinecraftClient.getInstance();

        for (lunarclient.apollo.team.v1.Schema.TeamMember member : msg.getMembersList()) {
            // Extract location
            lunarclient.apollo.common.v1.LocationOuterClass.Location loc = member.getLocation();
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();
            String world = loc.getWorld();

            // Extract color
            java.awt.Color color = new java.awt.Color(member.getMarkerColor().getColor());

            // Extract name from adventure JSON format
            String adventureJson = member.getAdventureJsonPlayerName();
            String name = extractName(adventureJson);

            // Extract UUID and check if it's self
            lunarclient.apollo.common.v1.UuidOuterClass.Uuid apolloUuid = member.getPlayerUuid();
            UUID mcUuid = toMinecraftUuid(apolloUuid);

            if (mc.player != null && mcUuid.equals(mc.player.getUuid())) {
                // Skip self - but store world for cross-world detection
                continue;
            }

            // Feed into TeamViewManager
            // Apollo data doesn't have biome/role, so we pass empty strings
            manager.updatePosition(name, world, x, y, z, "", "apollo");
            TeamViewManager.TeammateData td = manager.findByName(name);
            if (td != null) {
                td.updateApolloData(mcUuid, color);
            }
        }
    }

    private static void handleReset() {
        MahikariClient.MANAGER.clearAll();
        MahikariClient.MANAGER.setCurrentMode("");
        LOGGER.info("[Mahikari] Apollo team reset received");
    }

    /**
     * Convert Apollo protobuf UUID to java.util.UUID
     */
    public static UUID toMinecraftUuid(lunarclient.apollo.common.v1.UuidOuterClass.Uuid uuid) {
        return new UUID(uuid.getHigh64(), uuid.getLow64());
    }

    /**
     * Extract player name from adventure JSON format.
     * The name can be a simple JSON string like "\"PlayerName\""
     * or an object like {"text":"PlayerName"}.
     */
    public static String extractName(String adventureJson) {
        try {
            JsonElement element = JsonParser.parseString(adventureJson);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("text")) {
                    return obj.get("text").getAsString();
                }
            }
        } catch (Exception e) {
            // Fall through to raw return
        }
        // Fallback: return as-is, stripping quotes
        return adventureJson.replace("\"", "");
    }
}
