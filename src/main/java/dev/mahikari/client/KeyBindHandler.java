package dev.mahikari.client;

import dev.mahikari.client.screen.MahikariClickGui;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.util.Identifier;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.Screen;

public final class KeyBindHandler {
    private static KeyBinding settingsKey;
    public static void register() {
        KeyBinding.Category category = MahikariClient.MAIN_CATEGORY;
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.mahikari-client.settings", InputUtil.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (settingsKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new MahikariClickGui(null));
            }
        });
    }
}
