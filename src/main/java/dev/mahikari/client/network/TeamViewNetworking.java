package dev.mahikari.client.network;

import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewManager;
import dev.mahikari.client.network.TeamViewPayload;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class TeamViewNetworking {
    public static void registerClient() {
        PayloadTypeRegistry.playS2C().register(TeamViewPayload.ID, TeamViewPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TeamViewPayload.ID, (payload, context) -> {
            String raw = new String(payload.data(), StandardCharsets.UTF_8);
            context.client().execute(() -> TeamViewNetworking.handleServerMessage(raw));
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> MahikariClient.MANAGER.clearAll());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MahikariClient.MANAGER.clearAll());
    }

    private static void handleServerMessage(String raw) {
        TeamViewManager manager = MahikariClient.MANAGER;
        if (raw.startsWith("POS|")) {
            String[] parts = raw.split("\\|", 8);
            if (parts.length < 6) {
                return;
            }
            try {
                String name = parts[1];
                String world = parts[2];
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);
                double z = Double.parseDouble(parts[5]);
                String biome = parts.length >= 7 ? parts[6] : "";
                String role = parts.length >= 8 ? parts[7] : "";
                manager.updatePosition(name, world, x, y, z, biome, role);
            }
            catch (NumberFormatException name) {}
        } else if (raw.startsWith("OFFLINE|")) {
            manager.setOffline(raw.substring("OFFLINE|".length()));
        } else if (raw.startsWith("HEALTH|")) {
            String[] parts = raw.split("\\|", 5);
            if (parts.length >= 4) {
                try {
                    float absorption = parts.length >= 5 ? Float.parseFloat(parts[4]) : 0.0f;
                    manager.updateHealth(parts[1], Float.parseFloat(parts[2]), Float.parseFloat(parts[3]), absorption);
                }
                catch (NumberFormatException name) {}
            }
        } else if (raw.startsWith("EFFECTS|")) {
            String digest;
            String name;
            int firstPipe = raw.indexOf(124);
            int secondPipe = raw.indexOf(124, firstPipe + 1);
            if (secondPipe < 0) {
                name = raw.substring(firstPipe + 1);
                digest = "";
            } else {
                name = raw.substring(firstPipe + 1, secondPipe);
                digest = raw.substring(secondPipe + 1);
            }
            manager.updateRemoteEffects(name, TeamViewNetworking.parseEffectDigest(digest));
        } else if (raw.startsWith("MODE|")) {
            manager.setCurrentMode(raw.substring("MODE|".length()));
        } else if (raw.equals("CLEARALL")) {
            manager.clearAll();
            manager.setCurrentMode("");
        }
    }

    private static List<TeamViewManager.RemoteEffect> parseEffectDigest(String digest) {
        if (digest == null || digest.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<TeamViewManager.RemoteEffect> out = new ArrayList<TeamViewManager.RemoteEffect>();
        for (String token : digest.split(",")) {
            String[] parts = token.split(":");
            if (parts.length < 1) continue;
            String id = parts[0];
            int dur = 0;
            int amp = 0;
            try {
                if (parts.length > 1) {
                    dur = Integer.parseInt(parts[1]);
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            try {
                if (parts.length > 2) {
                    amp = Integer.parseInt(parts[2]);
                }
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
            out.add(new TeamViewManager.RemoteEffect(id, dur, amp));
        }
        return out;
    }
}
