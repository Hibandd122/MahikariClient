/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.SharedConstants
 *  net.minecraft.client.gui.screen.Screen
 */
package mahikariui.core;

import java.io.File;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.Screen;
import mahikariui.logging.ApacheLogger;
import mahikariui.logging.LoggingImpl;
import mahikariui.utils.FileUtils;
import mahikariui.utils.OSUtils;
import mahikariui.utils.StringUtils;
import mahikariui.utils.TimeUtils;
import mahikariui.utils.TranslationUtils;

public class Constants {
    public static final String MOD_NAME = "MahikariUI";
    public static final String VERSION_ID = "v1.0";
    public static final String VERSION_TYPE = "release";
    public static final String MOD_ID = "mahikariui";
    public static final String APP_ID = "mahikariui";
    public static final String configDir = OSUtils.USER_DIR + File.separator + "config";
    public static final String backgroundDir = configDir + File.separator + "mahikariui" + File.separator + "backgrounds";
    public static final String modsDir = OSUtils.USER_DIR + File.separator + "mods";
    public static final String MOD_NAMESPACE = "mahikariui";
    public static final String MCVersion = "1.21.11";
    public static final int MCBuildProtocol = StringUtils.getValidInteger("340").getSecond();
    public static String GAME_LOADER = null;
    public static boolean IS_GAME_CLOSING = false;
    public static final LoggingImpl LOG = new ApacheLogger("mahikariui");
    public static Supplier<Integer> MOD_COUNT_SUPPLIER = null;
    public static ModScreenSupplier MOD_SCREEN_SUPPLIER = null;
    public static final TranslationUtils TRANSLATOR = new TranslationUtils("mahikariui", true).setDefaultLanguage(Constants.getDefaultLanguage()).build();
    private static int DETECTED_MOD_COUNT = -1;
    private static final Function<Integer, String> DEFAULT_LANGUAGE_SUPPLIER = protocol -> protocol >= 315 ? "en_us" : "en_US";
    private static final String DEFAULT_LANGUAGE = Constants.getDefaultLanguage(MCBuildProtocol);

    public static int getModCount() {
        if (DETECTED_MOD_COUNT <= 0) {
            DETECTED_MOD_COUNT = MOD_COUNT_SUPPLIER != null ? MOD_COUNT_SUPPLIER.get() : Constants.getRawModCount();
        }
        return DETECTED_MOD_COUNT;
    }

    private static int getRawModCount() {
        int modCount = 0;
        File[] mods = new File(modsDir).listFiles();
        if (mods != null) {
            for (File modFile : mods) {
                if (!FileUtils.getFileExtension(modFile).equals(".jar")) continue;
                ++modCount;
            }
        }
        return Math.max(1, modCount);
    }

    public static void scheduleTickEvent(String name, Runnable event) {
        if (!IS_GAME_CLOSING) {
            FileUtils.getThreadPool(name).scheduleAtFixedRate(event, 0L, 50L, TimeUtils.getTimeUnitFrom("MILLISECONDS"));
        }
    }

    public static ThreadFactory getThreadFactory() {
        return FileUtils.getThreadFactory(MOD_NAME);
    }

    public static String getDefaultLanguage(int protocol) {
        return DEFAULT_LANGUAGE_SUPPLIER.apply(protocol);
    }

    public static String getDefaultLanguage() {
        return DEFAULT_LANGUAGE;
    }

    public static boolean isDebugging() {
        return Boolean.getBoolean("debug");
    }

    public static interface ModScreenSupplier {
        public Screen getModListScreen(Screen var1);
    }
}

