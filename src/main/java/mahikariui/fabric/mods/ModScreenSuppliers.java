/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.terraformersmc.modmenu.gui.ModsScreen
 *  net.minecraft.client.gui.screen.Screen
 */
package mahikariui.fabric.mods;


import net.minecraft.client.gui.screen.Screen;
import mahikariui.core.Constants;

public class ModScreenSuppliers
implements Constants.ModScreenSupplier {
    @Override
    public Screen getModListScreen(Screen parentScreen) {
        try {
            Class<?> modsScreenClass = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            return (Screen) modsScreenClass.getConstructor(Screen.class).newInstance(parentScreen);
        } catch (Exception e) {
            Constants.LOG.warn("ModMenu is not installed or failed to load. Returning to parent screen.", new Object[0]);
            return parentScreen;
        }
    }
}

