/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  edu.umd.cs.findbugs.annotations.SuppressFBWarnings
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.session.Session
 */
package mahikariui;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import mahikariui.core.Constants;
import mahikariui.core.config.Config;
import mahikariui.utils.GameUtils;
import mahikariui.utils.ModUtils;
import mahikariui.utils.OSUtils;


public final class MahikariUI {
    private static boolean initialized = false;
    private static Runnable initCallback;
    public static boolean isDataLoaded;
    public static MinecraftClient instance;
    public static Session session;
    public static PlayerEntity player;
    public static String username;
    public static String uuid;

    public MahikariUI(Runnable callback) {
        initCallback = callback;
        this.scheduleTick();
    }

    public MahikariUI() {
        this(null);
    }

    public static void init() {
        if (Constants.isDebugging()) {
            Constants.LOG.setDebugMode(true);
        }
        Constants.LOG.debugWarn("You are running in a debugging environment, some features may not function properly!", new Object[0]);
        Constants.LOG.debugInfo("Detected OS: %1$s (Architecture: %2$s, Is 64-Bit: %3$s)", OSUtils.OS_NAME, OSUtils.OS_ARCH, OSUtils.IS_64_BIT);
        if (initCallback != null) {
            initCallback.run();
        }
        isDataLoaded = true;
        Config.getInstance();
        initialized = true;
    }

    private void scheduleTick() {
        Constants.scheduleTickEvent("MahikariUI", this::clientTick);
    }

    private void clientTick() {
        if (!Constants.IS_GAME_CLOSING) {
            instance = ModUtils.getMinecraft();
            if (initialized || instance != null) {
                session = GameUtils.getSession(instance);
                if (initialized) {
                    player = GameUtils.getPlayer();
                    username = GameUtils.getUsername();
                    uuid = GameUtils.getUuid();
                } else if (session != null) {
                    MahikariUI.init();
                }
            }
        }
    }

    static {
        isDataLoaded = false;
    }
}

