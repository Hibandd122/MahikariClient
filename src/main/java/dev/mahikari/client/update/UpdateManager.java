package dev.mahikari.client.update;

import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.update.UpdateChecker.UpdateInfo;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;

import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateManager {
    private static final AtomicBoolean checked = new AtomicBoolean(false);
    private static volatile UpdateInfo pendingInfo;
    private static volatile String currentVersion = "0.0.0";

    public static void init() {
        currentVersion = FabricLoader.getInstance()
                .getModContainer("mahikari-client")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");

        ClientTickEvents.END_CLIENT_TICK.register(UpdateManager::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (!checked.get()) {
            // Only kick off the HTTP check once the client is past the early-boot tick
            // (currentScreen is non-null and we're on the main thread).
            if (client.currentScreen != null && checked.compareAndSet(false, true)) {
                kickOffCheck();
            }
            return;
        }

        UpdateInfo info = pendingInfo;
        if (info == null) return;
        if (!(client.currentScreen instanceof TitleScreen)) return;

        pendingInfo = null;
        client.setScreen(new UpdatePromptScreen(client.currentScreen, info, currentVersion));
    }

    private static void kickOffCheck() {
        UpdateChecker.checkLatest().thenAccept(info -> {
            if (info == null) return;
            if (!UpdateChecker.isNewer(info.version(), currentVersion)) return;
            if (info.version().equals(TeamViewConfig.get().skipUpdateForVersion)) return;
            pendingInfo = info;
        });
    }
}
