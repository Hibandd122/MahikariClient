package dev.mahikari.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class TeamViewConfig {
    private static volatile TeamViewConfig INSTANCE;
    private static final Gson GSON;
    private static final Path PATH;

    public boolean enabled = true;
    public String viewMode = "ALL";
    public String uiQuality = "MEDIUM"; // MEDIUM (Default), LOW (Performance)
    public boolean clickGuiNoBackground = false;
    public String skipUpdateForVersion = "";
    public boolean onScreenEnabled = true;
    public boolean offScreenEnabled = true;
    public boolean offScreenNear = false;
    public boolean showBiome = true;
    public boolean showDistance = true;
    public float nearRange = 64.0f;
    public float scaleMultiplier = 1.0f;
    public float offScreenScale = 0.5f;
    public String arrowColor = "GREEN";
    public boolean teamHudEnabled = true;
    public int teamHudOffsetX = 8;
    public int teamHudOffsetY = 8;
    public float teamHudScale = 1.0f;
    public int teamHudMaxCards = 3;
    public boolean teamHudShowBackground = true;
    public boolean teamHudShowHpText = true;
    public boolean teamHudShowEffects = true;
    public boolean teamHudDeathAnim = true;
    public String teamHudTheme = "SAO_CLASSIC";
    public String teamHudFilter = "AUTO";

    public boolean espEnabled = true;
    public boolean espBoxes = true;
    public boolean espTracers = true;
    public boolean espCornerBoxes = false;
    public boolean espHealthBar = true;
    public boolean espNames = true;
    public boolean espDistance = true;
    public boolean espThroughWalls = true;

    public int sprintingOffsetX = -1;
    public int sprintingOffsetY = -1;
    public boolean sprintingShowHud = true;
    public boolean sprintingShowBackground = false;
    public boolean sprintingShowBorder = true;
    public String sprintingTextColor = "WHITE"; // WHITE, GREEN, RED, BLUE, YELLOW
    public float sprintingScale = 1.0f;

    public boolean effectHudEnabled = true;
    public boolean effectHudShowBackground = true;
    public boolean effectHudShowBorder = false;
    public String effectHudLayout = "VERTICAL";
    public int effectHudOffsetX = 10;
    public int effectHudOffsetY = 100;
    public float effectHudScale = 1.0f;

    public boolean notificationsEnabled = true;
    public boolean notifyLegendary = true;
    public boolean notifyAirdrop = true;
    public boolean notifyCraftable = true;
    public boolean notificationSound = true;
    public float notificationScale = 1.0f;
    public float notificationDurationMul = 1.0f;
    public int notificationOffsetX = 9999;
    public int notificationOffsetY = 10;
    public int notificationMaxStack = 5;
    
    public boolean notificationShowBackground = true;
    public boolean notificationShowBorder = true;
    public boolean notificationShowIconBox = true;
    public boolean notificationShowShimmer = true;

    public boolean visualsEnabled = true;
    public boolean fullBright = true;
    public float fullBrightLevel = 1.0f;

    public boolean noFog = true;
    public float noFogDensity = 0.0f;
    
    /**
     * Validates and clamps configuration values to ensure they are within valid ranges.
     * This method should be called after loading configuration to prevent invalid values.
     */
    public void validateAndClamp() {
        this.fullBrightLevel = clampFinite(this.fullBrightLevel, 0.0f, 1.0f, 1.0f);
        this.noFogDensity = clampFinite(this.noFogDensity, 0.0f, 1.0f, 0.0f);
        this.nearRange = clampFinite(this.nearRange, 0.0f, Float.MAX_VALUE, 0.0f);
        this.scaleMultiplier = clampFinite(this.scaleMultiplier, 0.1f, Float.MAX_VALUE, 1.0f);
        this.offScreenScale = clampFinite(this.offScreenScale, 0.1f, 2.0f, 0.5f);
        this.teamHudScale = clampFinite(this.teamHudScale, 0.1f, 5.0f, 1.0f);
        this.effectHudScale = clampFinite(this.effectHudScale, 0.1f, 5.0f, 1.0f);
        this.notificationScale = clampFinite(this.notificationScale, 0.1f, 5.0f, 1.0f);
        this.notificationDurationMul = clampFinite(this.notificationDurationMul, 0.1f, 10.0f, 1.0f);
        this.itemScale = clampFinite(this.itemScale, 0.1f, 5.0f, 1.0f);
        this.totemScale = clampFinite(this.totemScale, 0.1f, 5.0f, 1.0f);
        this.fireHeight = clampFinite(this.fireHeight, -2.0f, 2.0f, 0.0f);
        this.shieldHeight = clampFinite(this.shieldHeight, -2.0f, 2.0f, 0.0f);
        this.totemHeight = clampFinite(this.totemHeight, -2.0f, 2.0f, 0.0f);
        this.itemOffsetX = clampFinite(this.itemOffsetX, -5.0f, 5.0f, 0.0f);
        this.itemOffsetY = clampFinite(this.itemOffsetY, -5.0f, 5.0f, 0.0f);
        this.smoothChatSpeed = clampFinite(this.smoothChatSpeed, 0.1f, 10.0f, 1.0f);
        this.sprintingScale = clampFinite(this.sprintingScale, 0.1f, 5.0f, 1.0f);
    }

    private static float clampFinite(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(min, Math.min(max, value));
    }
    public boolean clearFluids = true;
    public boolean autoSprint = false;
    public boolean sprintingAnimated = true;
    public boolean infiniteChat = true;
    public boolean smoothChat = true;
    public float smoothChatSpeed = 1.0f;
    public boolean effectHudShowAmplifier = true;
    public String effectHudSortMode = "DURATION";

    // Visual Tweaks
    public boolean lowFire = false;
    public float fireHeight = 0.5f;
    public boolean lowShield = false;
    public float shieldHeight = -0.2f;
    public boolean lowTotem = false;
    public float totemHeight = -0.2f;
    public float totemScale = 0.8f;
    public boolean smallItems = false;
    public float itemScale = 0.8f;
    public float itemOffsetX = 0.0f;
    public float itemOffsetY = 0.0f;
    public boolean itemPhysicsEnabled = false;
    public String itemPhysicsMode = "PHYSICS"; // PHYSICS or 2D

    // Optimization Tweaks
    public boolean optNoParticles = false;
    public boolean optNoWeather = false;
    public boolean optNoHurtCam = false;

    public static TeamViewConfig get() {
        if (INSTANCE == null) {
            TeamViewConfig.load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                INSTANCE = (TeamViewConfig)GSON.fromJson(Files.readString(PATH), TeamViewConfig.class);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        if (INSTANCE == null) {
            INSTANCE = new TeamViewConfig();
        }
        // Validate and clamp configuration values after loading
        INSTANCE.validateAndClamp();
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(TeamViewConfig.get()));
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public int getArrowColorRGB() {
        return switch (this.arrowColor.toUpperCase()) {
            case "CYAN" -> 0x00CCCC;
            case "YELLOW" -> 0xCCCC00;
            case "RED" -> 0xCC3333;
            case "BLUE" -> 0x3366FF;
            case "ORANGE" -> 0xFF8800;
            case "PINK" -> 0xFF66CC;
            case "WHITE" -> 0xCCCCCC;
            default -> 0x00CC00;
        };
    }

    public int getArrowColorARGB() {
        return 0xDD000000 | this.getArrowColorRGB();
    }

    public int getHighlightARGB() {
        int rgb = this.getArrowColorRGB();
        int r = Math.min(255, (rgb >> 16 & 0xFF) + 50);
        int g = Math.min(255, (rgb >> 8 & 0xFF) + 50);
        int b = Math.min(255, (rgb & 0xFF) + 50);
        return 0xDD000000 | r << 16 | g << 8 | b;
    }

    public int getShadowARGB() {
        int rgb = this.getArrowColorRGB();
        return 0x80000000 | ((rgb >> 16 & 0xFF) / 3) << 16 | ((rgb >> 8 & 0xFF) / 3) << 8 | (rgb & 0xFF) / 3;
    }

    public int getTextARGB() {
        int rgb = this.getArrowColorRGB();
        int r = Math.min(255, (rgb >> 16 & 0xFF) + 80);
        int g = Math.min(255, (rgb >> 8 & 0xFF) + 80);
        int b = Math.min(255, (rgb & 0xFF) + 80);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    public int getSprintingColorARGB() {
        return switch (this.sprintingTextColor.toUpperCase()) {
            case "GREEN" -> 0xFF55FF55;
            case "RED" -> 0xFFFF5555;
            case "BLUE" -> 0xFF5555FF;
            case "YELLOW" -> 0xFFFFFF55;
            case "GOLD" -> 0xFFFFAA00;
            case "AQUA" -> 0xFF55FFFF;
            case "PINK" -> 0xFFFF55FF;
            default -> 0xFFFFFFFF; // WHITE
        };
    }



    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
        PATH = FabricLoader.getInstance().getConfigDir().resolve("mahikari-client.json");
    }
}
