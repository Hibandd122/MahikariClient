package dev.mahikari.client.screen;

import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.animation.AnimTime;
import dev.mahikari.client.animation.AnimatedFloat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.Click;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class MahikariClickGui extends Screen {
    private static final int ROW_H = 22;
    private static final int LEFT_MARGIN = 22;
    private static final int RIGHT_MARGIN = 22;
    private static final int VALUE_W = 90;
    private static final int RESET_W = 48;
    private static final int SEARCH_Y = 20;
    private static final int SEARCH_H = 18;
    private static final int VIEWPORT_TOP = 44;
    private static final int VIEWPORT_BOT = 36;

    private static final int TAB_HUD = 0;
    private static final int TAB_ARROWS = 1;
    private static final int TAB_NOTIFY = 2;
    private static final int TAB_TEST = 3;
    private static final int TAB_EFFECTS = 4;
    private static final int TAB_SPRINT = 5;
    private static final int TAB_CHAT = 6;
    private static final int TAB_VISUALS = 7;
    private static final int TAB_HIDE_ARMOR = 8;
    private static final int TAB_LOW_FIRE = 9;
    private static final int TAB_LOW_SHIELD = 10;
    private static final int TAB_LOW_TOTEM = 11;
    private static final int TAB_SMALL_ITEMS = 12;
    private static final int TAB_PERFORMANCE = 13;

    private static final int ACCENT = 0xFF00D9FF;
    private static final int ACCENT_DARK = 0xFF0099CC;
    private static final int ACCENT_GLOW = 0x4400D9FF;
    private static final int PANEL_BG = 0xF5141820;
    private static final int PANEL_BORDER = 0xFF1E2530;
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFB0B8CC;
    private static final int TEXT_MUTED = 0xFF70788C;
    private static final int CARD_BG_START = 0xFF1A1D2E;
    private static final int CARD_BG_END = 0xFF0F1220;
    private static final int CARD_BORDER = 0xFF2D3250;
    private static final int CARD_BORDER_HOVER = ACCENT;
    private static final int ENVY_GREEN = 0xFF22C55E;
    private static final int SUNSET_RED = 0xFFDC2626;
    private static final int BUTTON_BLUE = 0xFF3B82F6;

    private enum ViewMode { GRID, SETTINGS }
    private static ViewMode currentView = ViewMode.GRID;

    private static int activeTab = TAB_HUD;
    private static AnimatedFloat scrollAnim = new AnimatedFloat(0f, 0.08f);
    private AnimatedFloat openAnim = new AnimatedFloat(0f, 0.05f);
    private boolean isClosing = false;
    private double maxScroll = 0.0;
    private TextFieldWidget searchField;
    private static String searchQuery = "";

    private float lastRenderedScroll = 0f;
    private float lastRenderedScale = 1f;

    private final List<Row> rows = new ArrayList<>();
    private final List<ModuleCard> cards = new ArrayList<>();

    private Screen parent;

    private record GridLayout(int cardW, int cardH, int gap, int cols, int rowCount,
                              int totalW, int contentH, int boxX, int boxY, int boxW, int boxH,
                              int startX, int startY, int viewportTop, int viewportBottom) {}

    private GridLayout layout;

    private GridLayout computeGridLayout(int cardCount) {
        int cardH = 122;
        int gap = 8;
        int targetCardW = 114;
        int maxCols = 6;
        int minCardW = 100;

        int sidePad = 14;
        int headerPad = 22;
        int footerPad = 14;

        int availableW = Math.max(targetCardW, this.width - 60 - 2 * sidePad);

        int cols = Math.max(1, Math.min(maxCols, (availableW + gap) / (targetCardW + gap)));
        int cardW = targetCardW;

        int needed = cardW * cols + gap * Math.max(0, cols - 1);
        if (needed > availableW) {
            cardW = Math.max(minCardW, (availableW - gap * Math.max(0, cols - 1)) / cols);
        }

        int totalW = cardW * cols + gap * Math.max(0, cols - 1);
        int rowCount = (int) Math.ceil((float) Math.max(1, cardCount) / cols);

        int contentH = rowCount * (cardH + gap) - gap + headerPad + footerPad;
        int boxH = Math.min(contentH, this.height - 60);
        int boxW = totalW + 2 * sidePad;
        int boxX = (this.width - boxW) / 2;
        int boxY = (this.height - boxH) / 2 - 10;

        int startX = boxX + sidePad;
        int startY = boxY + headerPad;

        int viewportTop = boxY + 20;
        int viewportBottom = boxY + boxH - 4;

        return new GridLayout(cardW, cardH, gap, cols, rowCount, totalW, contentH,
                              boxX, boxY, boxW, boxH, startX, startY, viewportTop, viewportBottom);
    }

    public MahikariClickGui(Screen parent) {
        super(Text.literal("Mahikari Client Settings"));
        this.parent = parent;
    }

    public static net.minecraft.text.Text styled(String s) {
        return net.minecraft.text.Text.literal(s);
    }

    @Override
    protected void init() {
        this.rows.clear();
        boolean firstOpen = this.openAnim.target() == 0f;
        if (firstOpen) {
            this.openAnim.snap(0f);
            scrollAnim.snap(0f);
        }
        this.isClosing = false;
        this.openAnim.setTarget(1f);
        if (this.currentView == ViewMode.GRID) {
            initGrid();
        } else {
            initSettings();
        }
    }

    private void openGrid() {
        this.currentView = ViewMode.GRID;
        this.clearAndInit();
    }

    private void openSettings(int tab) {
        this.activeTab = tab;
        this.currentView = ViewMode.SETTINGS;
        this.scrollAnim.snap(0f);
        this.clearAndInit();
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.scrollAnim.snap(0f);
        this.clearAndInit();
    }

    private void initGrid() {
        cards.clear();
        TeamViewConfig cfg = TeamViewConfig.get();

        int tmpW = 200, tmpH = 70;

        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Team HUD", "Health bars & Status", net.minecraft.item.Items.DIAMOND_HELMET.getDefaultStack(), () -> openSettings(TAB_HUD), () -> cfg.teamHudEnabled, v -> cfg.teamHudEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Indicators", "Player Arrows", net.minecraft.item.Items.COMPASS.getDefaultStack(), () -> openSettings(TAB_ARROWS), () -> cfg.onScreenEnabled, v -> cfg.onScreenEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Notifications", "Smart Alerts", net.minecraft.item.Items.ENDER_CHEST.getDefaultStack(), () -> openSettings(TAB_NOTIFY), () -> cfg.notificationsEnabled, v -> cfg.notificationsEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Tests & Tools", "Debug Commands", net.minecraft.item.Items.COMMAND_BLOCK.getDefaultStack(), () -> openSettings(TAB_TEST)));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Effect HUD", "Potion Display", net.minecraft.item.Items.POTION.getDefaultStack(), () -> openSettings(TAB_EFFECTS), () -> cfg.effectHudEnabled, v -> cfg.effectHudEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Auto Sprint", "Sprint Toggle", net.minecraft.item.Items.FEATHER.getDefaultStack(), () -> openSettings(TAB_SPRINT), () -> cfg.autoSprint, v -> cfg.autoSprint = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Chat Utils", "Enhanced Chat", net.minecraft.item.Items.PAPER.getDefaultStack(), () -> openSettings(TAB_CHAT)));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Visuals", "Brightness & Fog", net.minecraft.item.Items.ENDER_EYE.getDefaultStack(), () -> openSettings(TAB_VISUALS), () -> cfg.visualsEnabled, v -> cfg.visualsEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Hide Armor", "Invisible Armor", net.minecraft.item.Items.IRON_CHESTPLATE.getDefaultStack(), () -> openSettings(TAB_HIDE_ARMOR), () -> cfg.hideArmor, v -> cfg.hideArmor = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Low Fire", "Reduce Fire Overlay", net.minecraft.item.Items.FLINT_AND_STEEL.getDefaultStack(), () -> openSettings(TAB_LOW_FIRE), () -> cfg.lowFire, v -> cfg.lowFire = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Low Shield", "Lower Shield View", net.minecraft.item.Items.SHIELD.getDefaultStack(), () -> openSettings(TAB_LOW_SHIELD), () -> cfg.lowShield, v -> cfg.lowShield = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Low Totem", "Lower Totem View", net.minecraft.item.Items.TOTEM_OF_UNDYING.getDefaultStack(), () -> openSettings(TAB_LOW_TOTEM), () -> cfg.lowTotem, v -> cfg.lowTotem = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Small Items", "Resize Items", net.minecraft.item.Items.IRON_SWORD.getDefaultStack(), () -> openSettings(TAB_SMALL_ITEMS), () -> cfg.smallItems, v -> cfg.smallItems = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Performance", "Optimize FPS", net.minecraft.item.Items.REDSTONE.getDefaultStack(), () -> openSettings(TAB_PERFORMANCE)));

        this.layout = computeGridLayout(cards.size());

        for (int i = 0; i < cards.size(); i++) {
            ModuleCard c = cards.get(i);
            int col = i % layout.cols();
            int row = i / layout.cols();
            c.width = layout.cardW();
            c.height = layout.cardH();
            c.x = layout.startX() + col * (layout.cardW() + layout.gap());
            c.y = layout.startY() + row * (layout.cardH() + layout.gap());
        }

        this.maxScroll = Math.max(0, layout.contentH() - layout.boxH());

        addDrawableChild(ButtonWidget.builder(styled("Back"), b -> {
            if (this.client != null) this.client.setScreen(this.parent);
        }).dimensions(this.width / 2 - 50, layout.boxY() + layout.boxH() + 10, 100, 20).build());
    }

    private void initSettings() {
        TeamViewConfig cfg = TeamViewConfig.get();
        TeamViewConfig def = new TeamViewConfig();

        int panelW = Math.min(420, this.width - 40);
        int panelX = (this.width - panelW) / 2;
        int panelY = 32;

        int searchW = 100;
        this.searchField = new TextFieldWidget(this.textRenderer, panelX + panelW - searchW - 14, panelY + 8, searchW, 14, styled("Search..."));
        this.searchField.setPlaceholder(styled("§7Search settings..."));
        this.searchField.setChangedListener(s -> {
            this.searchQuery = s == null ? "" : s.toLowerCase(Locale.ROOT);
            this.scrollAnim.setTarget(0f);
        });
        if (!searchQuery.isEmpty()) {
            this.searchField.setText(searchQuery);
        }
        addDrawableChild(this.searchField);

        addDrawableChild(ButtonWidget.builder(styled("Back"), b -> openGrid())
            .dimensions(panelX + 12, panelY + 6, 52, 18).build());

        // HUD TAB
        addBool(TAB_HUD, "Enable Mahikari", () -> cfg.enabled, v -> cfg.enabled = v, def.enabled);
        Supplier<Boolean> tvEnabled = () -> cfg.enabled;

        addCycle(TAB_HUD, "View mode", () -> cfg.viewMode, v -> cfg.viewMode = v, def.viewMode, new String[]{"ALL", "PARTY_ONLY", "ALL_PARTY_OFFSCREEN"}, this::filterLabel).setCondition(tvEnabled);
        addBool(TAB_HUD, "Enable Team HUD", () -> cfg.teamHudEnabled, v -> cfg.teamHudEnabled = v, def.teamHudEnabled).setCondition(tvEnabled);

        Supplier<Boolean> hudEnabled = () -> cfg.enabled && cfg.teamHudEnabled;

        Supplier<Boolean> hudAdv = addCategory(TAB_HUD, "Advanced Settings", hudEnabled);
        Supplier<Boolean> hudAdvCond = () -> hudEnabled.get() && hudAdv.get();

        addBool(TAB_HUD, "Show Background", () -> cfg.teamHudShowBackground, v -> cfg.teamHudShowBackground = v, def.teamHudShowBackground).setCondition(hudAdvCond).indent();
        addBool(TAB_HUD, "Show HP Text", () -> cfg.teamHudShowHpText, v -> cfg.teamHudShowHpText = v, def.teamHudShowHpText).setCondition(hudAdvCond).indent();
        addBool(TAB_HUD, "Show Effects", () -> cfg.teamHudShowEffects, v -> cfg.teamHudShowEffects = v, def.teamHudShowEffects).setCondition(hudAdvCond).indent();
        addBool(TAB_HUD, "Death Animation", () -> cfg.teamHudDeathAnim, v -> cfg.teamHudDeathAnim = v, def.teamHudDeathAnim).setCondition(hudAdvCond).indent();
        addCycle(TAB_HUD, "Show which teammates", () -> cfg.teamHudFilter, v -> cfg.teamHudFilter = v, def.teamHudFilter, new String[]{"AUTO", "PARTY_ONLY", "TEAM_ONLY", "ALL"}, this::filterLabel).setCondition(hudAdvCond).indent();
        addIntSlider(TAB_HUD, "Max players shown", () -> cfg.teamHudMaxCards, v -> cfg.teamHudMaxCards = v, def.teamHudMaxCards, 1, 8, "%d").setCondition(hudAdvCond).indent();

        // ARROWS TAB
        addBool(TAB_ARROWS, "On-screen arrows", () -> cfg.onScreenEnabled, v -> cfg.onScreenEnabled = v, def.onScreenEnabled);
        addBool(TAB_ARROWS, "Off-screen arrows", () -> cfg.offScreenEnabled, v -> cfg.offScreenEnabled = v, def.offScreenEnabled);

        Supplier<Boolean> anyArrow = () -> cfg.onScreenEnabled || cfg.offScreenEnabled;
        Supplier<Boolean> offArrow = () -> cfg.offScreenEnabled;

        Supplier<Boolean> arrAdv = addCategory(TAB_ARROWS, "Advanced Settings", anyArrow);
        Supplier<Boolean> anyArrAdvCond = () -> anyArrow.get() && arrAdv.get();
        Supplier<Boolean> offArrAdvCond = () -> offArrow.get() && arrAdv.get();

        addBool(TAB_ARROWS, "Show near off-screen", () -> cfg.offScreenNear, v -> cfg.offScreenNear = v, def.offScreenNear).setCondition(offArrAdvCond).indent();
        addFloatSlider(TAB_ARROWS, "Near range", () -> cfg.nearRange, v -> cfg.nearRange = (float)v, def.nearRange, 32.0, 128.0, "%.0f blocks").setCondition(offArrAdvCond).indent();
        addFloatSlider(TAB_ARROWS, "Off-screen scale", () -> cfg.offScreenScale, v -> cfg.offScreenScale = (float)v, def.offScreenScale, 0.25, 2.0, "%.2fx").setCondition(offArrAdvCond).indent();
        addBool(TAB_ARROWS, "Show biome icon", () -> cfg.showBiome, v -> cfg.showBiome = v, def.showBiome).setCondition(anyArrAdvCond).indent();
        addBool(TAB_ARROWS, "Show distance", () -> cfg.showDistance, v -> cfg.showDistance = v, def.showDistance).setCondition(anyArrAdvCond).indent();
        addFloatSlider(TAB_ARROWS, "Indicator scale", () -> cfg.scaleMultiplier, v -> cfg.scaleMultiplier = (float)v, def.scaleMultiplier, 0.25, 3.0, "%.2fx").setCondition(anyArrAdvCond).indent();

        // NOTIFY TAB
        addBool(TAB_NOTIFY, "Enable Notifications", () -> cfg.notificationsEnabled, v -> cfg.notificationsEnabled = v, def.notificationsEnabled);
        Supplier<Boolean> notifEnabled = () -> cfg.notificationsEnabled;

        addBool(TAB_NOTIFY, "Legendary craft alerts", () -> cfg.notifyLegendary, v -> cfg.notifyLegendary = v, def.notifyLegendary).setCondition(notifEnabled).indent();
        addBool(TAB_NOTIFY, "Airdrop alerts", () -> cfg.notifyAirdrop, v -> cfg.notifyAirdrop = v, def.notifyAirdrop).setCondition(notifEnabled).indent();
        addBool(TAB_NOTIFY, "Craftable item alerts", () -> cfg.notifyCraftable, v -> cfg.notifyCraftable = v, def.notifyCraftable).setCondition(notifEnabled).indent();
        addBool(TAB_NOTIFY, "Play notification sound", () -> cfg.notificationSound, v -> cfg.notificationSound = v, def.notificationSound).setCondition(notifEnabled).indent();

        Supplier<Boolean> notifAdv = addCategory(TAB_NOTIFY, "Visual Settings", notifEnabled);
        Supplier<Boolean> notifAdvCond = () -> notifEnabled.get() && notifAdv.get();

        addBool(TAB_NOTIFY, "Show Background", () -> cfg.notificationShowBackground, v -> cfg.notificationShowBackground = v, def.notificationShowBackground).setCondition(notifAdvCond).indent();
        addBool(TAB_NOTIFY, "Show Glass Border", () -> cfg.notificationShowBorder, v -> cfg.notificationShowBorder = v, def.notificationShowBorder).setCondition(notifAdvCond).indent();
        addBool(TAB_NOTIFY, "Show Icon Box", () -> cfg.notificationShowIconBox, v -> cfg.notificationShowIconBox = v, def.notificationShowIconBox).setCondition(notifAdvCond).indent();
        addBool(TAB_NOTIFY, "Show Shimmer Effect", () -> cfg.notificationShowShimmer, v -> cfg.notificationShowShimmer = v, def.notificationShowShimmer).setCondition(notifAdvCond).indent();
        addFloatSlider(TAB_NOTIFY, "Duration multiplier", () -> cfg.notificationDurationMul, v -> cfg.notificationDurationMul = (float)v, def.notificationDurationMul, 0.25, 3.0, "%.2fx").setCondition(notifAdvCond).indent();
        addIntSlider(TAB_NOTIFY, "Max notifications shown", () -> cfg.notificationMaxStack, v -> cfg.notificationMaxStack = v, def.notificationMaxStack, 1, 8, "%d").setCondition(notifAdvCond).indent();

        // PERFORMANCE TAB
        addCycle(TAB_PERFORMANCE, "UI Quality", () -> cfg.uiQuality, v -> {
            cfg.uiQuality = v;
            if (FabricLoader.getInstance().isModLoaded("mahikariui")) {
                try {
                    Class<?> configClass = Class.forName("mahikariui.core.config.Config");
                    Object instance = configClass.getMethod("getInstance").invoke(null);
                    configClass.getMethod("setLowQualityMode", boolean.class).invoke(instance, "LOW".equals(v));
                } catch (Exception e) {}
            }
        }, def.uiQuality, new String[]{"MEDIUM", "LOW"}, this::filterLabel);
        addBool(TAB_PERFORMANCE, "No Background (ClickGui)", () -> cfg.clickGuiNoBackground, v -> cfg.clickGuiNoBackground = v, def.clickGuiNoBackground);

        // VISUALS TAB
        addBool(TAB_VISUALS, "Enable Visuals Module", () -> cfg.visualsEnabled, v -> cfg.visualsEnabled = v, def.visualsEnabled);
        Supplier<Boolean> visualsEnabled = () -> cfg.visualsEnabled;

        addBool(TAB_VISUALS, "FullBright (Nhin xuyen dem)", () -> cfg.fullBright, v -> cfg.fullBright = v, def.fullBright).setCondition(visualsEnabled);
        Supplier<Boolean> fullBrightEnabled = () -> cfg.visualsEnabled && cfg.fullBright;
        addFloatSlider(TAB_VISUALS, "Brightness Level", () -> cfg.fullBrightLevel, v -> cfg.fullBrightLevel = (float)v, def.fullBrightLevel, 0.0, 1.0, "%.2f").setCondition(fullBrightEnabled).indent();

        addBool(TAB_VISUALS, "NoFog (Xoa suong mu)", () -> cfg.noFog, v -> cfg.noFog = v, def.noFog).setCondition(visualsEnabled);
        Supplier<Boolean> noFogEnabled = () -> cfg.visualsEnabled && cfg.noFog;
        addFloatSlider(TAB_VISUALS, "Fog Density", () -> cfg.noFogDensity, v -> cfg.noFogDensity = (float)v, def.noFogDensity, 0.0, 1.0, "%.2f").setCondition(noFogEnabled).indent();

        addBool(TAB_VISUALS, "Clear Water / Lava (Nhin xuyen chat long)", () -> cfg.clearFluids, v -> cfg.clearFluids = v, def.clearFluids).setCondition(visualsEnabled);

        // HIDE ARMOR TAB
        addBool(TAB_HIDE_ARMOR, "Hide Player Armor", () -> cfg.hideArmor, v -> cfg.hideArmor = v, def.hideArmor);
        Supplier<Boolean> hideArmorEnabled = () -> cfg.hideArmor;
        addCycle(TAB_HIDE_ARMOR, "Apply to", () -> cfg.hideArmorMode, v -> cfg.hideArmorMode = v, def.hideArmorMode,
            new String[]{"ALL", "SELF_ONLY", "OTHERS_ONLY"}, this::filterLabel).setCondition(hideArmorEnabled).indent();

        // LOW FIRE TAB
        addBool(TAB_LOW_FIRE, "Low Fire (Giam lua che mat)", () -> cfg.lowFire, v -> cfg.lowFire = v, def.lowFire);
        Supplier<Boolean> fireEnabled = () -> cfg.lowFire;
        addFloatSlider(TAB_LOW_FIRE, "Fire Offset", () -> cfg.fireHeight, v -> cfg.fireHeight = (float)v, def.fireHeight, 0.0, 1.0, "%.2f").setCondition(fireEnabled).indent();

        // LOW SHIELD TAB
        addBool(TAB_LOW_SHIELD, "Low Shield (Ha thap khien)", () -> cfg.lowShield, v -> cfg.lowShield = v, def.lowShield);
        Supplier<Boolean> shieldEnabled = () -> cfg.lowShield;
        addFloatSlider(TAB_LOW_SHIELD, "Shield Offset", () -> cfg.shieldHeight, v -> cfg.shieldHeight = (float)v, def.shieldHeight, -1.0, 0.0, "%.2f").setCondition(shieldEnabled).indent();

        // LOW TOTEM TAB
        addBool(TAB_LOW_TOTEM, "Low Totem (Ha thap Totem)", () -> cfg.lowTotem, v -> cfg.lowTotem = v, def.lowTotem);
        Supplier<Boolean> totemEnabled = () -> cfg.lowTotem;
        addFloatSlider(TAB_LOW_TOTEM, "Totem Offset", () -> cfg.totemHeight, v -> cfg.totemHeight = (float)v, def.totemHeight, -1.0, 0.0, "%.2f").setCondition(totemEnabled).indent();
        addFloatSlider(TAB_LOW_TOTEM, "Totem Scale", () -> cfg.totemScale, v -> cfg.totemScale = (float)v, def.totemScale, 0.1, 1.5, "%.2fx").setCondition(totemEnabled).indent();

        // SMALL ITEMS TAB
        addBool(TAB_SMALL_ITEMS, "Small Items (Do cam tay nho)", () -> cfg.smallItems, v -> cfg.smallItems = v, def.smallItems);
        Supplier<Boolean> itemsEnabled = () -> cfg.smallItems;
        addFloatSlider(TAB_SMALL_ITEMS, "Item Scale", () -> cfg.itemScale, v -> cfg.itemScale = (float)v, def.itemScale, 0.1, 1.5, "%.2fx").setCondition(itemsEnabled).indent();
        addFloatSlider(TAB_SMALL_ITEMS, "Item Offset X", () -> cfg.itemOffsetX, v -> cfg.itemOffsetX = (float)v, def.itemOffsetX, -1.0, 1.0, "%.2f").setCondition(itemsEnabled).indent();
        addFloatSlider(TAB_SMALL_ITEMS, "Item Offset Y", () -> cfg.itemOffsetY, v -> cfg.itemOffsetY = (float)v, def.itemOffsetY, -1.0, 1.0, "%.2f").setCondition(itemsEnabled).indent();

        Supplier<Boolean> physicsEnabled = addCategory(TAB_SMALL_ITEMS, "Item Physics", null);
        addBool(TAB_SMALL_ITEMS, "Enable ItemPhysics", () -> cfg.itemPhysicsEnabled, v -> cfg.itemPhysicsEnabled = v, def.itemPhysicsEnabled).setCondition(physicsEnabled).indent();
        addCycle(TAB_SMALL_ITEMS, "Physics Mode", () -> cfg.itemPhysicsMode, v -> cfg.itemPhysicsMode = v, def.itemPhysicsMode, new String[]{"PHYSICS", "2D"}, String::toString).setCondition(physicsEnabled).indent();

        // EFFECTS TAB
        addBool(TAB_EFFECTS, "Enable Effect HUD", () -> cfg.effectHudEnabled, v -> cfg.effectHudEnabled = v, def.effectHudEnabled);
        Supplier<Boolean> effectEnabled = () -> cfg.effectHudEnabled;
        addBool(TAB_EFFECTS, "Show Background", () -> cfg.effectHudShowBackground, v -> cfg.effectHudShowBackground = v, def.effectHudShowBackground).setCondition(effectEnabled).indent();
        addBool(TAB_EFFECTS, "Show Colored Border", () -> cfg.effectHudShowBorder, v -> cfg.effectHudShowBorder = v, def.effectHudShowBorder).setCondition(effectEnabled).indent();
        addCycle(TAB_EFFECTS, "Layout Style", () -> cfg.effectHudLayout, v -> cfg.effectHudLayout = v, def.effectHudLayout, new String[]{"VERTICAL", "HORIZONTAL"}, this::filterLabel).setCondition(effectEnabled).indent();
        addBool(TAB_EFFECTS, "Show Amplifier Level", () -> cfg.effectHudShowAmplifier, v -> cfg.effectHudShowAmplifier = v, def.effectHudShowAmplifier).setCondition(effectEnabled).indent();
        addCycle(TAB_EFFECTS, "Sort By", () -> cfg.effectHudSortMode, v -> cfg.effectHudSortMode = v, def.effectHudSortMode, new String[]{"DURATION", "ALPHABETICAL"}, this::filterLabel).setCondition(effectEnabled).indent();

        // SPRINT TAB
        addBool(TAB_SPRINT, "AutoSprint (Tu dong chay nhanh)", () -> cfg.autoSprint, v -> cfg.autoSprint = v, def.autoSprint);
        Supplier<Boolean> sprintEnabled = () -> cfg.autoSprint;

        addBool(TAB_SPRINT, "Show HUD Indicator", () -> cfg.sprintingShowHud, v -> cfg.sprintingShowHud = v, def.sprintingShowHud).setCondition(sprintEnabled).indent();
        Supplier<Boolean> sprintHudEnabled = () -> cfg.autoSprint && cfg.sprintingShowHud;
        addBool(TAB_SPRINT, "Animated HUD", () -> cfg.sprintingAnimated, v -> cfg.sprintingAnimated = v, def.sprintingAnimated).setCondition(sprintHudEnabled).indent();
        addBool(TAB_SPRINT, "Show Background", () -> cfg.sprintingShowBackground, v -> cfg.sprintingShowBackground = v, def.sprintingShowBackground).setCondition(sprintHudEnabled).indent();
        addBool(TAB_SPRINT, "Show Bracket Border", () -> cfg.sprintingShowBorder, v -> cfg.sprintingShowBorder = v, def.sprintingShowBorder).setCondition(sprintHudEnabled).indent();
        addCycle(TAB_SPRINT, "Text Color", () -> cfg.sprintingTextColor, v -> cfg.sprintingTextColor = v, def.sprintingTextColor, new String[]{"WHITE", "GREEN", "RED", "BLUE", "YELLOW", "GOLD", "AQUA", "PINK"}, this::filterLabel).setCondition(sprintHudEnabled).indent();
        addFloatSlider(TAB_SPRINT, "Scale", () -> cfg.sprintingScale, v -> cfg.sprintingScale = (float)v, def.sprintingScale, 0.5, 3.0, "%.2fx").setCondition(sprintHudEnabled).indent();


        // CHAT TAB
        addBool(TAB_CHAT, "Smooth Chat", () -> cfg.smoothChat, v -> cfg.smoothChat = v, def.smoothChat);
        Supplier<Boolean> smoothChatEnabled = () -> cfg.smoothChat;
        addFloatSlider(TAB_CHAT, "Animation Speed", () -> cfg.smoothChatSpeed, v -> cfg.smoothChatSpeed = (float)v, def.smoothChatSpeed, 0.1, 3.0, "%.1fx").setCondition(smoothChatEnabled).indent();
        addBool(TAB_CHAT, "Infinite Chat (Chat vo han)", () -> cfg.infiniteChat, v -> cfg.infiniteChat = v, def.infiniteChat);

        // TEST TAB
        addAction(TAB_TEST, "HUD Layout Editor", "Open Editor", () -> {
            if (this.client != null) this.client.setScreen(new HudEditorScreen(this));
        });

        addAction(TAB_TEST, "Test Notifications", "Send Test", () -> {
            dev.mahikari.client.notification.NotificationManager.addNotification("�6�lLEGENDARY CO THE CRAFT", "Da craft thanh cong Dragon Armor!", 0xFFCC00, 8000);
            dev.mahikari.client.notification.NotificationManager.addNotification("�b�lAIRDROP", "Airdrop da roi tai 100, 200", 0x00CCFF, 8000);
            dev.mahikari.client.notification.NotificationManager.addNotification("�c�lWARNING", "Sap thu hep vong bo!", 0xFF3333, 5000);
        });

        addAction(TAB_TEST, "Test Team HUD", "Spawn Fake Team", () -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null && mc.world != null) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                String world = mc.world.getRegistryKey().getValue().getPath();
                dev.mahikari.client.TeamViewManager mgr = dev.mahikari.client.MahikariClient.MANAGER;
                mgr.updatePosition("TestPlayer1", world, x + 5, y, z + 5, "plains", "king");
                mgr.updatePosition("TestPlayer2", world, x - 5, y, z - 5, "forest", "vip");
                mgr.updatePosition("TestPlayer3", world, x + 10, y, z, "desert", "party");
                mgr.updatePosition("TestPlayer4", world, x - 10, y, z, "taiga", "team");
                mgr.updateHealth("TestPlayer1", 20.0f, 20.0f, 0.0f);
                mgr.updateHealth("TestPlayer2", 10.0f, 20.0f, 0.0f);
                mgr.updateHealth("TestPlayer3", 1.0f, 20.0f, 0.0f);
                mgr.updateHealth("TestPlayer4", 16.0f, 20.0f, 0.0f);
            }
        });

        addAction(TAB_TEST, "Test Team Damage & Heal", "Simulate", () -> {
            dev.mahikari.client.TeamViewManager mgr = dev.mahikari.client.MahikariClient.MANAGER;
            mgr.updateHealth("TestPlayer1", 5.0f, 20.0f, 0.0f);
            mgr.updateHealth("TestPlayer2", 20.0f, 20.0f, 0.0f);
        });

        addAction(TAB_TEST, "Test Effect HUD", "Give Effects", () -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) {
                mc.player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH, 1200, 1));
                mc.player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SPEED, 2400, 0));
                mc.player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.REGENERATION, 600, 2));
            }
        });

        addAction(TAB_TEST, "Clear All Tests", "Clear", () -> {
            dev.mahikari.client.MahikariClient.MANAGER.clearAll();
            dev.mahikari.client.notification.NotificationManager.clear();
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) mc.player.clearStatusEffects();
        });

        for (Row r : this.rows) {
            if (r.valBtn != null) r.valBtn.visible = false;
            if (r.resBtn != null) r.resBtn.visible = false;
        }
    }

    @Override
    public void removed() {
        TeamViewConfig.save();
        super.removed();
    }

    private Supplier<Boolean> addCategory(int section, String label, Supplier<Boolean> parentCond) {
        boolean[] expanded = {false};
        ButtonWidget btn = ButtonWidget.builder(styled("\u00a7a+"), b -> {
            expanded[0] = !expanded[0];
            b.setMessage(styled(expanded[0] ? "\u00a7c-" : "\u00a7a+"));
        }).dimensions(0, 0, 22, 22).build();
        Row r = new Row(section, "\u00a7e" + label, btn, null);
        r.visibilityCond = parentCond;
        r.isCategory = true;
        rows.add(r);
        addDrawableChild(btn);
        return () -> expanded[0];
    }

    private String filterLabel(String v) {
        return v.replace("_", " ");
    }

    private Row addBool(int section, String label, Supplier<Boolean> get, Consumer<Boolean> set, boolean defVal) {
        ConfigToggle toggle = new ConfigToggle(0, 0, 28, 20, get, set);
        ButtonWidget reset = ButtonWidget.builder(styled("\u00a77Reset"), b -> {
            set.accept(defVal);
            TeamViewConfig.save();
        }).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, toggle, reset);
        rows.add(r);
        addDrawableChild(toggle);
        addDrawableChild(reset);
        return r;
    }

    private Row addCycle(int section, String label, Supplier<String> get, Consumer<String> set, String defVal, String[] values, Function<String, String> display) {
        ButtonWidget[] holder = new ButtonWidget[1];
        holder[0] = ButtonWidget.builder(Text.literal(display.apply(get.get())), b -> {
            String cur = get.get();
            int idx = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(cur)) idx = i;
            }
            String next = values[(idx + 1) % values.length];
            set.accept(next);
            TeamViewConfig.save();
            holder[0].setMessage(Text.literal(display.apply(next)));
        }).dimensions(0, 0, VALUE_W, 20).build();
        ButtonWidget reset = ButtonWidget.builder(Text.literal("\u00a77Reset"), b -> {
            set.accept(defVal);
            TeamViewConfig.save();
            holder[0].setMessage(Text.literal(display.apply(defVal)));
        }).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, holder[0], reset);
        rows.add(r);
        addDrawableChild(holder[0]);
        addDrawableChild(reset);
        return r;
    }

    private Row addIntSlider(int section, String label, IntSupplier get, IntConsumer set, int defVal, int min, int max, String fmt) {
        ConfigIntSlider slider = new ConfigIntSlider(0, 0, VALUE_W, 20, get.getAsInt(), min, max, fmt, set);
        ButtonWidget reset = ButtonWidget.builder(styled("\u00a77Reset"), b -> {
            slider.setIntValue(defVal);
            TeamViewConfig.save();
        }).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, slider, reset);
        rows.add(r);
        addDrawableChild(slider);
        addDrawableChild(reset);
        return r;
    }

    private Row addFloatSlider(int section, String label, DoubleSupplier get, DoubleConsumer set, double defVal, double min, double max, String fmt) {
        ConfigFloatSlider slider = new ConfigFloatSlider(0, 0, VALUE_W, 20, get.getAsDouble(), min, max, fmt, set);
        ButtonWidget reset = ButtonWidget.builder(styled("\u00a77Reset"), b -> {
            slider.setDoubleValue(defVal);
            TeamViewConfig.save();
        }).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, slider, reset);
        rows.add(r);
        addDrawableChild(slider);
        addDrawableChild(reset);
        return r;
    }

    private Row addAction(int section, String label, String btnText, Runnable action, Runnable resetAction) {
        ButtonWidget act = ButtonWidget.builder(styled("\u00a7f" + btnText), b -> action.run()).dimensions(0, 0, VALUE_W, 20).build();
        ButtonWidget reset = ButtonWidget.builder(styled("\u00a77Reset"), b -> {
            if (resetAction != null) resetAction.run();
        }).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, act, reset);
        rows.add(r);
        addDrawableChild(act);
        addDrawableChild(reset);
        return r;
    }

    private Row addAction(int section, String label, String btnText, Runnable action) {
        ButtonWidget act = ButtonWidget.builder(styled("\u00a7f" + btnText), b -> action.run()).dimensions(0, 0, VALUE_W, 20).build();
        Row r = new Row(section, label, act, null);
        rows.add(r);
        addDrawableChild(act);
        return r;
    }

    @Override
    public void close() {
        if (!this.isClosing) {
            this.isClosing = true;
            this.openAnim.setTarget(0f);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
        boolean noBg = TeamViewConfig.get().clickGuiNoBackground;

        float animRaw = isLowQuality ? 1.0f : this.openAnim.tick();
        float animProgress = Math.max(0f, Math.min(1f, animRaw));

        if (this.isClosing && (animRaw <= 0.01f || isLowQuality)) {
            super.close();
            return;
        }

        float scrollOffset = isLowQuality ? this.scrollAnim.target() : this.scrollAnim.tick();

        float scale = isLowQuality ? 1.0f : (0.90f + 0.10f * animProgress);
        this.lastRenderedScroll = scrollOffset;
        this.lastRenderedScale = scale;

        if (!noBg) {
            int bgAlpha1 = (int)(0xF5 * animProgress);
            int bgAlpha2 = (int)(0xEE * animProgress);
            int bgColor1 = (bgAlpha1 << 24) | 0x0A0B10;
            int bgColor2 = (bgAlpha2 << 24) | 0x050508;
            ctx.fillGradient(0, 0, this.width, this.height, bgColor1, bgColor2);

            if (!isLowQuality) {
                int accentGlowAlpha = (int)(30 * animProgress);
                int accentGlow = (accentGlowAlpha << 24) | 0x00D9FF;
                ctx.fillGradient(0, 0, this.width, 1, accentGlow, 0x00000000);
                ctx.fillGradient(this.width - 1, 0, this.width, this.height, accentGlow, 0x00000000);
            }
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(this.width / 2.0f, this.height / 2.0f);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-this.width / 2.0f, -this.height / 2.0f);

        if (!noBg) {
            int headerH = 40;
            ctx.fillGradient(0, 0, this.width, headerH, applyAlpha(0xCC0F1419, animProgress), applyAlpha(0x880A0B10, animProgress));
            ctx.fill(0, headerH - 1, this.width, headerH, applyAlpha(ACCENT, animProgress * 0.25f));

            int logoGlowAlpha = (int)(80 * animProgress);
            int logoGlowColor = (logoGlowAlpha << 24) | 0x00D9FF;
            fillRounded(ctx, 6, 6, 36, 36, logoGlowColor, 6);
            fillRounded(ctx, 8, 8, 34, 34, applyAlpha(0x22000000, animProgress), 5);

            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, net.minecraft.util.Identifier.of("mahikari-client", "textures/gui/logo.png"), 10, 10, 0.0f, 0.0f, 22, 22, 22, 22);

            ctx.drawTextWithShadow(this.textRenderer, "\u00a7b\u00a7lMahikari", 42, 11, applyAlpha(ACCENT, animProgress));
            ctx.drawTextWithShadow(this.textRenderer, "\u00a7f\u00a7lClient", 42 + this.textRenderer.getWidth("Mahikari "), 11, applyAlpha(TEXT_PRIMARY, animProgress));

            String version = "v2.0";
            int versionW = this.textRenderer.getWidth(version);
            int badgeX = 42 + this.textRenderer.getWidth("Mahikari Client ") + 4;
            int badgeY = 13;
            fillRounded(ctx, badgeX, badgeY, badgeX + versionW + 10, badgeY + 12, applyAlpha(0x9900D9FF, animProgress), 6);
            ctx.fillGradient(badgeX, badgeY, badgeX + versionW + 10, badgeY + 12, applyAlpha(0x2200D9FF, animProgress), 0x00000000);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(badgeX + 5, badgeY + 2);
            ctx.getMatrices().scale(0.7f, 0.7f);
            ctx.drawTextWithShadow(this.textRenderer, version, 0, 0, applyAlpha(TEXT_PRIMARY, animProgress));
            ctx.getMatrices().popMatrix();
        }

        super.render(ctx, mouseX, mouseY, delta);

        if (this.currentView == ViewMode.GRID) {
            GridLayout l = this.layout;
            if (l == null) {
                ctx.getMatrices().popMatrix();
                return;
            }

            ctx.enableScissor(l.boxX(), l.viewportTop(), l.boxX() + l.boxW(), l.viewportBottom());
            for (ModuleCard c : cards) {
                c.render(ctx, mouseX, mouseY, delta);
            }
            ctx.disableScissor();

            if (maxScroll > 0) {
                int trackX = l.boxX() + l.boxW() - 10;
                int trackW = 4;
                int thumbH = Math.max(24, (int)((float)l.boxH() * l.boxH() / l.contentH()));
                int thumbY = l.boxY() + (int)((l.boxH() - thumbH) * (scrollOffset / maxScroll));

                fillRounded(ctx, trackX, l.boxY() + 20, trackX + trackW, l.boxY() + l.boxH() - 2, 0x22FFFFFF, 2);
                fillRoundedGradient(ctx, trackX, thumbY, trackX + trackW, thumbY + thumbH, applyAlpha(ACCENT, 0.7f), applyAlpha(ACCENT_DARK, 0.7f), 2);
                fillRounded(ctx, trackX - 1, thumbY - 1, trackX + trackW + 1, thumbY + thumbH + 1, applyAlpha(ACCENT, 0.15f), 3);
            }

            ctx.getMatrices().popMatrix();
            return;
        }

        int panelW = Math.min(420, this.width - 40);
        int panelH = this.height - 66;
        int panelX = (this.width - panelW) / 2;
        int panelY = 32;
        int vpTop = panelY + 36;
        int vpBot = panelY + panelH - 2;

        int curY = vpTop + 4;
        int visibleCount = 0;
        int rowIndex = 0;

        for (Row r : this.rows) {
            boolean matchSearch = searchQuery.isEmpty() || r.label.toLowerCase(Locale.ROOT).contains(searchQuery);
            boolean condPassed = r.visibilityCond == null || r.visibilityCond.get();
            boolean visible = r.section == activeTab && matchSearch && condPassed;

            if (visible) {
                int displayY = curY - (int)scrollOffset;
                boolean fullyIn = displayY >= vpTop && displayY + ROW_H <= vpBot;

                r.valBtn.visible = fullyIn;
                if (r.resBtn != null) r.resBtn.visible = fullyIn;

                if (fullyIn) {
                    if (!r.isCategory && rowIndex % 2 == 0) {
                        ctx.fill(panelX + 10, displayY, panelX + panelW - 10, displayY + ROW_H, applyAlpha(0x08FFFFFF, animProgress));
                    }

                    int maxValRight = panelX + panelW - 20;
                    if (r.resBtn != null) {
                        r.resBtn.setX(maxValRight - RESET_W);
                        r.resBtn.setY(displayY + 2);
                        maxValRight = r.resBtn.getX() - 8;
                    }
                    r.valBtn.setX(maxValRight - r.valBtn.getWidth());
                    r.valBtn.setY(displayY + 2);

                    int textX = panelX + 22 + r.indentLevel;
                    int textColor;
                    if (r.isCategory) {
                        textColor = 0xFFFFD700;
                    } else if (r.indentLevel > 0) {
                        textColor = TEXT_SECONDARY;
                    } else {
                        textColor = TEXT_PRIMARY;
                    }
                    ctx.drawText(this.textRenderer, styled(r.label), textX, displayY + 7, textColor, true);
                }
                curY += ROW_H;
                visibleCount++;
                rowIndex++;
            } else {
                r.valBtn.visible = false;
                if (r.resBtn != null) r.resBtn.visible = false;
            }
        }

        int totalH = curY - (vpTop + 4);
        int viewH = vpBot - vpTop;
        this.maxScroll = Math.max(0, totalH - viewH);
        this.scrollAnim.setTarget((float)Math.max(0, Math.min(this.maxScroll, this.scrollAnim.target())));

        if (visibleCount == 0) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("\u00a77No settings found"), this.width / 2, vpTop + 30, TEXT_PRIMARY);
        }

        if (maxScroll > 0) {
            int trackX = panelX + panelW - 10;
            int trackW = 4;
            int thumbH = Math.max(24, (int)((float)viewH * viewH / totalH));
            int thumbY = vpTop + (int)((viewH - thumbH) * (scrollOffset / maxScroll));

            fillRounded(ctx, trackX, vpTop, trackX + trackW, vpBot, 0x22FFFFFF, 2);
            fillRoundedGradient(ctx, trackX, thumbY, trackX + trackW, thumbY + thumbH, applyAlpha(ACCENT, 0.7f), applyAlpha(ACCENT_DARK, 0.7f), 2);
            fillRounded(ctx, trackX - 1, thumbY - 1, trackX + trackW + 1, thumbY + thumbH + 1, applyAlpha(ACCENT, 0.15f), 3);
        }

        ctx.getMatrices().popMatrix();
    }

    /**
     * Inverse-transform raw screen-space mouse coordinates into the scaled
     * coordinate space that render() uses. The render method applies:
     *   translate(cx, cy) → scale(s, s) → translate(-cx, -cy)
     * which maps a point (px, py) to:
     *   screenX = (px - cx) * s + cx
     *   screenY = (py - cy) * s + cy
     * Inverting: px = (screenX - cx) / s + cx
     */
    private double toScaledX(double screenX) {
        float s = this.lastRenderedScale;
        if (s <= 0.001f || s >= 0.999f) return screenX;
        double cx = this.width / 2.0;
        return (screenX - cx) / s + cx;
    }

    private double toScaledY(double screenY) {
        float s = this.lastRenderedScale;
        if (s <= 0.001f || s >= 0.999f) return screenY;
        double cy = this.height / 2.0;
        return (screenY - cy) / s + cy;
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        if (this.currentView == ViewMode.GRID && click.buttonInfo().button() == 0) {
            GridLayout l = this.layout;
            if (l == null) return super.mouseClicked(click, bl);

            float scrollOffset = this.lastRenderedScroll;

            double mx = toScaledX(click.x());
            double my = toScaledY(click.y());

            if (my >= l.viewportTop() && my <= l.viewportBottom() && mx >= l.boxX() && mx <= l.boxX() + l.boxW() - 16) {
                for (ModuleCard c : cards) {
                    int drawY = c.y - (int)scrollOffset;

                    if (drawY + c.height < l.viewportTop() || drawY > l.viewportBottom()) continue;

                    if (mx >= c.x && mx <= c.x + c.width && my >= drawY && my <= drawY + c.height) {

                        int btnH = 20;
                        int btnX = c.x + 7;
                        int btnY = drawY + c.height - btnH - 7;
                        int btnW = c.width - 14;
                        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                            if (c.getState != null && c.toggleState != null) {
                                c.toggleState.accept(!c.getState.get());
                                TeamViewConfig.save();
                            } else {
                                c.onClick.run();
                            }
                            net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal(""), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                            return true;
                        }

                        int optS = 18;
                        int optX = c.x + c.width - optS - 6;
                        int optY = drawY + 6;
                        if (mx >= optX && mx <= optX + optS && my >= optY && my <= optY + optS) {
                            c.onClick.run();
                            net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal(""), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                            return true;
                        }

                        c.onClick.run();
                        net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.literal(""), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (maxScroll > 0) {
            float target = this.scrollAnim.target() - (float)vertical * 30f;
            this.scrollAnim.setTarget(Math.max(0f, Math.min((float)maxScroll, target)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        super.renderBackground(ctx, mx, my, delta);

        if (this.currentView == ViewMode.GRID) {
            GridLayout l = this.layout;
            if (l == null) return;
            int boxX = l.boxX();
            int boxY = l.boxY();
            int boxW = l.boxW();
            int boxH = l.boxH();

            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, this.openAnim.get()));

            int bgCol = applyAlpha(PANEL_BG, animProgress);
            int borderCol = applyAlpha(PANEL_BORDER, animProgress);
            int accentCol = applyAlpha(ACCENT_GLOW, animProgress);

            // Shadow layers with enhanced depth
            fillRounded(ctx, boxX - 4, boxY + 3, boxX + boxW + 4, boxY + boxH + 3, applyAlpha(0x44000000, animProgress), 14);
            fillRounded(ctx, boxX - 2, boxY + 2, boxX + boxW + 2, boxY + boxH + 2, applyAlpha(0x66000000, animProgress), 12);
            fillRounded(ctx, boxX - 1, boxY + 1, boxX + boxW + 1, boxY + boxH + 1, applyAlpha(0x33000000, animProgress), 10);

            // Main panel - glassmorphism effect
            fillRounded(ctx, boxX, boxY, boxX + boxW, boxY + boxH, bgCol, 10);
            ctx.fillGradient(boxX + 2, boxY + 2, boxX + boxW - 2, boxY + 8, applyAlpha(0x15FFFFFF, animProgress), 0x00FFFFFF);

            // Top accent line - gradient glow
            fillRounded(ctx, boxX + 4, boxY + 4, boxX + boxW - 4, boxY + 6, applyAlpha(0x55FFFFFF, animProgress), 1);
            fillRoundedGradient(ctx, boxX + 4, boxY + 4, boxX + boxW - 4, boxY + 6, applyAlpha(ACCENT, 0.4f), applyAlpha(ACCENT, 0.1f), 1);

            // Border
            ctx.fill(boxX, boxY, boxX + boxW, boxY + 1, borderCol);
            ctx.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, borderCol);
            ctx.fill(boxX, boxY, boxX + 1, boxY + boxH, borderCol);
            ctx.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, borderCol);

            // Title
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("\u00a7l\u00a7nMODULES"), this.width / 2, boxY + 14, applyAlpha(ACCENT, animProgress));

            return;
        }

        if (this.currentView == ViewMode.SETTINGS) {
            int panelW = Math.min(420, this.width - 40);
            int panelH = this.height - 66;
            int panelX = (this.width - panelW) / 2;
            int panelY = 32;

            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, this.openAnim.get()));

            int bgCol = applyAlpha(PANEL_BG, animProgress);
            int borderCol = applyAlpha(PANEL_BORDER, animProgress);
            int accentCol = applyAlpha(ACCENT_GLOW, animProgress);

            fillRounded(ctx, panelX - 4, panelY + 3, panelX + panelW + 4, panelY + panelH + 3, applyAlpha(0x44000000, animProgress), 14);
            fillRounded(ctx, panelX - 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, applyAlpha(0x66000000, animProgress), 12);
            fillRounded(ctx, panelX - 1, panelY + 1, panelX + panelW + 1, panelY + panelH + 1, applyAlpha(0x33000000, animProgress), 10);

            fillRounded(ctx, panelX, panelY, panelX + panelW, panelY + panelH, bgCol, 10);
            ctx.fillGradient(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 8, applyAlpha(0x15FFFFFF, animProgress), 0x00FFFFFF);

            fillRounded(ctx, panelX + 4, panelY + 4, panelX + panelW - 4, panelY + 6, applyAlpha(0x55FFFFFF, animProgress), 1);
            fillRoundedGradient(ctx, panelX + 4, panelY + 4, panelX + panelW - 4, panelY + 6, applyAlpha(ACCENT, 0.4f), applyAlpha(ACCENT, 0.1f), 1);

            ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, borderCol);
            ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderCol);
            ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, borderCol);
            ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderCol);

            String title = activeTab == TAB_HUD ? "Team HUD" :
                           activeTab == TAB_ARROWS ? "Indicators" :
                           activeTab == TAB_NOTIFY ? "Notifications" :
                           activeTab == TAB_EFFECTS ? "Effect HUD" :
                           activeTab == TAB_SPRINT ? "Auto Sprint" :
                           activeTab == TAB_CHAT ? "Chat" :
                           activeTab == TAB_VISUALS ? "Visuals" :
                           activeTab == TAB_HIDE_ARMOR ? "Hide Armor" :
                           activeTab == TAB_LOW_FIRE ? "Low Fire" :
                           activeTab == TAB_LOW_SHIELD ? "Low Shield" :
                           activeTab == TAB_LOW_TOTEM ? "Low Totem" :
                           activeTab == TAB_PERFORMANCE ? "Performance" :
                             activeTab == TAB_SMALL_ITEMS ? "Small Items" : "Tests & Tools";
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("\u00a7l" + title), panelX + panelW / 2, panelY + 12, applyAlpha(ACCENT, animProgress));

            ctx.fill(panelX + 8, panelY + 28, panelX + panelW - 8, panelY + 29, applyAlpha(0x22FFFFFF, animProgress));
        }
    }

    @Override
    public void renderDarkening(DrawContext ctx) {}

    private static void fillRounded(DrawContext ctx, int x0, int y0, int x1, int y1, int color, int r) {
        if (r < 1 || x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        ctx.fill(x0, y0 + r, x1, y1 - r, color);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, color);
            ctx.fill(x0 + inset, y1 - 1 - i, x1 - inset, y1 - i, color);
        }
    }

    private static void fillRoundedGradient(DrawContext ctx, int x0, int y0, int x1, int y1, int colorStart, int colorEnd, int r) {
        if (r < 1 || x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            ctx.fillGradient(x0, y0, x1, y1, colorStart, colorEnd);
            return;
        }
        ctx.fillGradient(x0, y0 + r, x1, y1 - r, colorStart, colorEnd);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, colorStart);
            ctx.fill(x0 + inset, y1 - 1 - i, x1 - inset, y1 - i, colorEnd);
        }
    }

    private static class Row {
        int section;
        String label;
        ClickableWidget valBtn;
        ClickableWidget resBtn;
        Supplier<Boolean> visibilityCond;
        int indentLevel = 0;
        boolean isCategory = false;

        Row(int s, String l, ClickableWidget v, ClickableWidget r) {
            this.section = s; this.label = l; this.valBtn = v; this.resBtn = r;
        }

        Row indent() {
            this.indentLevel = 12;
            this.label = "\u00a77\u21aa \u00a7f" + this.label;
            return this;
        }

        Row setCondition(Supplier<Boolean> cond) {
            this.visibilityCond = cond;
            return this;
        }
    }

    private static class ConfigIntSlider extends SliderWidget {
        private final int min, max;
        private final String fmt;
        private final IntConsumer set;

        ConfigIntSlider(int x, int y, int w, int h, int val, int min, int max, String fmt, IntConsumer set) {
            super(x, y, w, h, Text.literal(""), max == min ? 0 : (double)(val - min)/(max - min));
            this.min = min; this.max = max; this.fmt = fmt; this.set = set;
            updateMessage();
        }

        void setIntValue(int v) {
            this.value = max == min ? 0 : (double)(v - min)/(max - min);
            set.accept(v);
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Text.literal(String.format(fmt, min + (int)(value * (max - min)))));
        }

        @Override protected void applyValue() {
            set.accept(min + (int)(value * (max - min)));
            TeamViewConfig.save();
        }

    }

    private static class ConfigFloatSlider extends SliderWidget {
        private final double min, max;
        private final String fmt;
        private final DoubleConsumer set;

        ConfigFloatSlider(int x, int y, int w, int h, double val, double min, double max, String fmt, DoubleConsumer set) {
            super(x, y, w, h, Text.literal(""), max == min ? 0 : (val - min)/(max - min));
            this.min = min; this.max = max; this.fmt = fmt; this.set = set;
            updateMessage();
        }

        void setDoubleValue(double v) {
            this.value = max == min ? 0 : (v - min)/(max - min);
            set.accept(v);
            updateMessage();
        }

        @Override protected void updateMessage() {
            setMessage(Text.literal(String.format(fmt, min + value * (max - min))));
        }

        @Override protected void applyValue() {
            set.accept(min + value * (max - min));
            TeamViewConfig.save();
        }

    }

    private class ModuleCard {
        int x, y, width, height;
        String title, desc;
        net.minecraft.item.ItemStack iconItem;
        net.minecraft.util.Identifier iconId;
        Runnable onClick;
        Supplier<Boolean> getState;
        Consumer<Boolean> toggleState;
        AnimatedFloat hoverAnim = new AnimatedFloat(0f, 0.05f);
        AnimatedFloat btnAnim = new AnimatedFloat(0f, 0.08f);

        public ModuleCard(int x, int y, int w, int h, String title, String desc, net.minecraft.item.ItemStack iconItem, Runnable onClick) {
            this(x, y, w, h, title, desc, iconItem, null, onClick, null, null);
        }

        public ModuleCard(int x, int y, int w, int h, String title, String desc, net.minecraft.item.ItemStack iconItem, Runnable onClick, Supplier<Boolean> getState, Consumer<Boolean> toggleState) {
            this(x, y, w, h, title, desc, iconItem, null, onClick, getState, toggleState);
        }

        public ModuleCard(int x, int y, int w, int h, String title, String desc, net.minecraft.item.ItemStack iconItem, net.minecraft.util.Identifier iconId, Runnable onClick, Supplier<Boolean> getState, Consumer<Boolean> toggleState) {
            this.x = x; this.y = y; this.width = w; this.height = h;
            this.title = title;
            this.desc = desc;
            this.iconItem = iconItem;
            this.iconId = iconId;
            this.onClick = onClick;
            this.getState = getState;
            this.toggleState = toggleState;
        }

        private int getIconBgColor() {
            if (this.iconId != null) return 0xFF5B6EE1;
            if (this.iconItem == null) return 0xFF4A5568;
            net.minecraft.item.Item item = this.iconItem.getItem();
            if (item == net.minecraft.item.Items.DIAMOND_HELMET) return 0xFFFF6B9D;
            if (item == net.minecraft.item.Items.COMPASS) return 0xFFFFA500;
            if (item == net.minecraft.item.Items.BELL) return 0xFFFFD700;
            if (item == net.minecraft.item.Items.COMMAND_BLOCK) return 0xFFB794F4;
            if (item == net.minecraft.item.Items.POTION) return 0xFFFF79C6;
            if (item == net.minecraft.item.Items.FEATHER) return 0xFF4FC3F7;
            if (item == net.minecraft.item.Items.PAPER) return 0xFF26C6DA;
            if (item == net.minecraft.item.Items.ENDER_EYE) return 0xFF26A69A;
            if (item == net.minecraft.item.Items.IRON_CHESTPLATE) return 0xFF90A4AE;
            if (item == net.minecraft.item.Items.FLINT_AND_STEEL) return 0xFFFF7043;
            if (item == net.minecraft.item.Items.SHIELD) return 0xFF546E7A;
            if (item == net.minecraft.item.Items.TOTEM_OF_UNDYING) return 0xFFFFB300;
            if (item == net.minecraft.item.Items.IRON_SWORD) return 0xFF66BB6A;
            if (item == net.minecraft.item.Items.REDSTONE) return 0xFFEF5350;
            if (item == net.minecraft.item.Items.PAINTING) return 0xFF5B6EE1;
            return 0xFF4A5568;
        }

        private int brighten(int color) {
            int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
            int g = Math.min(255, ((color >> 8) & 0xFF) + 30);
            int b = Math.min(255, (color & 0xFF) + 30);
            return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
        }

        private int darken(int color) {
            int r = Math.max(0, ((color >> 16) & 0xFF) - 30);
            int g = Math.max(0, ((color >> 8) & 0xFF) - 30);
            int b = Math.max(0, (color & 0xFF) - 30);
            return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
        }

        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            int drawY = y - (int)lastRenderedScroll;

            double scaledMX = toScaledX(mouseX);
            double scaledMY = toScaledY(mouseY);
            boolean hovered = scaledMX >= x && scaledMX <= x + width && scaledMY >= drawY && scaledMY <= drawY + height;

            this.hoverAnim.setTarget(hovered ? 1f : 0f);
            float hProgress = isLowQuality ? (hovered ? 1f : 0f) : this.hoverAnim.tick();
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, MahikariClickGui.this.openAnim.get()));

            int bgColor = applyAlpha(CARD_BG_START, animProgress);
            int borderColor = applyAlpha(CARD_BORDER, animProgress);
            boolean isActive = getState != null && getState.get();
            int cx = x + width / 2;

            // Shadow
            fillRounded(ctx, x, drawY + 4, x + width, drawY + height + 4, applyAlpha(0x55000000, animProgress), 8);
            fillRounded(ctx, x - 1, drawY + 3, x + width + 1, drawY + height + 3, applyAlpha(0x33000000, animProgress), 7);

            // Card background
            fillRoundedGradient(ctx, x, drawY, x + width, drawY + height, bgColor, applyAlpha(MahikariClickGui.darken(CARD_BG_END, 0.8f), animProgress), 8);
            fillRounded(ctx, x, drawY, x + width, drawY + height, borderColor, 8);

            // Top highlight
            ctx.fillGradient(x + 2, drawY + 2, x + width - 2, drawY + 5, applyAlpha(0x18FFFFFF, animProgress), 0x00FFFFFF);

            // Settings gear
            int optS = 18;
            int optX = x + width - optS - 6;
            int optY = drawY + 6;
            boolean optHovered = scaledMX >= optX && scaledMX <= optX + optS && scaledMY >= optY && scaledMY <= optY + optS;
            fillRounded(ctx, optX, optY, optX + optS, optY + optS, applyAlpha(optHovered ? 0xFF3D4860 : 0xFF252A40, animProgress), 4);
            String gear = "\u2699";
            ctx.drawTextWithShadow(client.textRenderer, styled(gear), optX + (optS - client.textRenderer.getWidth(gear)) / 2, optY + 3, applyAlpha(optHovered ? ACCENT : TEXT_SECONDARY, animProgress));

            // Icon centered at top
            int boxSize = 22;
            int boxX = cx - boxSize / 2;
            int boxY2 = drawY + 10;
            fillRounded(ctx, boxX + 1, boxY2 + 2, boxX + boxSize + 1, boxY2 + boxSize + 2, applyAlpha(0x88000000, animProgress), 6);
            fillRoundedGradient(ctx, boxX, boxY2, boxX + boxSize, boxY2 + boxSize, applyAlpha(0xFF2D3250, animProgress), applyAlpha(0xFF1A1F2E, animProgress), 6);

            ctx.getMatrices().pushMatrix();
            float iOff = (boxSize - 16f) / 2f;
            ctx.getMatrices().translate(boxX + iOff, boxY2 + iOff);
            if (this.iconId != null) {
                ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.iconId, 0, 0, 0.0f, 0.0f, 16, 16, 16, 16);
            } else if (this.iconItem != null) {
                ctx.drawItem(this.iconItem, 0, 0);
            }
            ctx.getMatrices().popMatrix();

            if (client != null) {
                // Title centered
                int titleY = boxY2 + boxSize + 7;
                String tt = this.title;
                int tw = client.textRenderer.getWidth(tt);
                ctx.drawTextWithShadow(client.textRenderer, styled("\u00a7l" + tt), cx - tw / 2, titleY, applyAlpha(TEXT_PRIMARY, animProgress));

                // Description centered, smaller
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(cx, titleY + 11f);
                ctx.getMatrices().scale(0.75f, 0.75f);
                int dw = client.textRenderer.getWidth(this.desc);
                int dc = isActive ? applyAlpha(TEXT_SECONDARY, animProgress) : applyAlpha(TEXT_MUTED, animProgress);
                ctx.drawTextWithShadow(client.textRenderer, styled(this.desc), -dw / 2, 0, dc);
                ctx.getMatrices().popMatrix();

                // Bottom button
                int btnH = 20;
                int btnX2 = x + 7;
                int btnY2 = drawY + height - btnH - 7;
                int btnW2 = width - 14;
                boolean btnHovered = scaledMX >= btnX2 && scaledMX <= btnX2 + btnW2 && scaledMY >= btnY2 && scaledMY <= btnY2 + btnH;

                float btnProgress;
                if (getState != null) {
                    float btnTarget = isActive ? 1f : 0f;
                    btnAnim.setTarget(btnTarget);
                    btnProgress = isLowQuality ? btnTarget : btnAnim.tick();
                } else {
                    btnProgress = 0f;
                }

                int btnC;
                if (getState != null) {
                    btnC = interpolateColor(0xFF4A5070, ENVY_GREEN, btnProgress);
                } else {
                    btnC = BUTTON_BLUE;
                }
                if (btnHovered) btnC = MahikariClickGui.brighten(btnC);

                fillRounded(ctx, btnX2, btnY2, btnX2 + btnW2, btnY2 + btnH, applyAlpha(btnC, animProgress), 4);
                ctx.fillGradient(btnX2 + 2, btnY2 + 2, btnX2 + btnW2 - 2, btnY2 + 4, applyAlpha(0x40FFFFFF, animProgress), 0x00FFFFFF);

                String lbl;
                if (getState != null) lbl = isActive ? "\u00a7lON" : "\u00a7lOFF";
                else lbl = "\u00a7lOPEN";
                int lw = client.textRenderer.getWidth(lbl);
                ctx.drawTextWithShadow(client.textRenderer, styled(lbl), btnX2 + (btnW2 - lw) / 2, btnY2 + 5, applyAlpha(TEXT_PRIMARY, animProgress));
            }
        }
    }

    private class ConfigToggle extends net.minecraft.client.gui.widget.ClickableWidget {
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private AnimatedFloat anim = new AnimatedFloat(0f, 0.06f);

        ConfigToggle(int x, int y, int w, int h, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(x, y, w, h, Text.literal(""));
            this.getter = get;
            this.setter = set;
            this.anim.setTarget(get.get() ? 1f : 0f);
        }

        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean state = getter.get();
            anim.setTarget(state ? 1f : 0f);
            float t = anim.tick();
            float open = Math.max(0f, Math.min(1f, MahikariClickGui.this.openAnim.get()));

            int dotColor = interpolateColor(0xFF4A5070, 0xFF22C55E, t);
            if (isHovered()) dotColor = interpolateColor(dotColor, 0xFFFFFFFF, 0.3f);
            int dotAlpha = applyAlpha(dotColor, open);
            ctx.drawTextWithShadow(client.textRenderer, styled("\u25cf"), getX(), getY() + 4, dotAlpha);

            String text = state ? "ON" : "OFF";
            int textColor = interpolateColor(0xFF6B7280, 0xFF22C55E, t);
            if (isHovered()) textColor = interpolateColor(textColor, 0xFFFFFFFF, 0.3f);
            ctx.drawTextWithShadow(client.textRenderer, styled("\u00a7l" + text), getX() + 10, getY() + 4, applyAlpha(textColor, open));
        }

        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            double mouseX = toScaledX(click.x());
            double mouseY = toScaledY(click.y());
            if (this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight() && click.buttonInfo().button() == 0) {
                setter.accept(!getter.get());
                TeamViewConfig.save();
                this.playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                return true;
            }
            return super.mouseClicked(click, bl);
        }

        @Override
        protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
            this.appendDefaultNarrations(builder);
        }
    }

    private static int interpolateColor(int c1, int c2, float t) {
        int a = (int)(((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int)(((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int)(((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int)((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int applyAlpha(int color, float alphaMult) {
        if (alphaMult >= 1.0f) return color;
        int a = (int)(((color >> 24) & 0xFF) * alphaMult);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.max(0, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.max(0, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.max(0, Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 30);
        int b = Math.min(255, (argb & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
