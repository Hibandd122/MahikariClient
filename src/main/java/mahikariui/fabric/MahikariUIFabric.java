/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.minecraft.server.command.ServerCommandSource
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.TitleScreen
 */
package mahikariui.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import mahikariui.MahikariUI;
import mahikariui.core.Constants;
import mahikariui.core.commands.MahikariUICommand;

import mahikariui.fabric.mods.ModScreenSuppliers;
import mahikariui.utils.OSUtils;

public final class MahikariUIFabric
implements ClientModInitializer {
    public void onInitializeClient() {
        try {
            if (OSUtils.JAVA_SPEC < 1.8f) {
                throw new UnsupportedOperationException(String.format("Incompatible JVM!!! %1$s requires Java 8 or above to work properly!", "mahikariui"));
            }
            CommandRegistrationCallback.EVENT.register((commandDispatcher, commandBuildContext, commandSelection) -> MahikariUICommand.register((CommandDispatcher<ServerCommandSource>)commandDispatcher));
            Constants.GAME_LOADER = "fabric";
            Constants.MOD_SCREEN_SUPPLIER = new ModScreenSuppliers();
            new MahikariUI(this::setupIntegrations);
            ScreenEvents.AFTER_INIT.register(this::onScreenOpen);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void setupIntegrations() {
    }

    public void onScreenOpen(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (screen instanceof net.minecraft.client.gui.screen.TitleScreen) {
            client.setScreen((Screen)new mahikariui.core.gui.screens.TitleScreen());
        }
    }
}

