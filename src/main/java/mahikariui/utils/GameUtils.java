/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.session.Session
 *  net.minecraft.client.gui.screen.SplashOverlay
 *  net.minecraft.client.gui.screen.Screen
 */
package mahikariui.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.Screen;
import mahikariui.utils.ModUtils;
import mahikariui.utils.WorldUtils;

public class GameUtils {
    public static MinecraftClient getMinecraft() {
        return ModUtils.getMinecraft();
    }

    public static PlayerEntity getPlayer() {
        return WorldUtils.getPlayer(GameUtils.getMinecraft());
    }

    public static Session getSession(MinecraftClient client) {
        return client != null ? client.getSession() : null;
    }

    public static Session getSession() {
        return GameUtils.getSession(GameUtils.getMinecraft());
    }

    public static String getUsername(MinecraftClient client) {
        return GameUtils.getSession(client).getUsername();
    }

    public static String getUsername() {
        return GameUtils.getUsername(GameUtils.getMinecraft());
    }

    public static String getUuid(MinecraftClient client) {
        return GameUtils.getSession(client).getUuidOrNull().toString();
    }

    public static String getUuid() {
        return GameUtils.getUuid(GameUtils.getMinecraft());
    }

    public static Screen getCurrentScreen(MinecraftClient client) {
        return client != null ? client.currentScreen : null;
    }

    public static Screen getCurrentScreen() {
        return GameUtils.getCurrentScreen(GameUtils.getMinecraft());
    }

    public static boolean isFocused(MinecraftClient client) {
        Screen screen = GameUtils.getCurrentScreen(client);
        return screen != null && (screen.getFocused() != null || WorldUtils.getPlayer(client) != null);
    }

    public static boolean isFocused() {
        return GameUtils.isFocused(GameUtils.getMinecraft());
    }

    public static boolean isLoaded(MinecraftClient client) {
        return GameUtils.getCurrentScreen(client) != null && !(client.getOverlay() instanceof SplashOverlay) || WorldUtils.getPlayer(client) != null;
    }

    public static boolean isLoaded() {
        return GameUtils.isLoaded(GameUtils.getMinecraft());
    }
}

