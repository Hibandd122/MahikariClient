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
    private static final int ROW_H = 20;
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
    private static final int TAB_NAMETAG = 14;
    
    private enum ViewMode { GRID, SETTINGS }
    private static ViewMode currentView = ViewMode.GRID;
    
    private static int activeTab = TAB_HUD;
    private static AnimatedFloat scrollAnim = new AnimatedFloat(0f, 0.08f);
    private AnimatedFloat openAnim = new AnimatedFloat(0f, 0.05f); // Faster for smoother feel
    private boolean isClosing = false;
    private double maxScroll = 0.0;
    private TextFieldWidget searchField;
    private static String searchQuery = "";

    /**
     * The exact scroll offset and scale factor used in the most recent render().
     * mouseClicked() must use these exact same values so click positions match
     * what was painted on screen. Previously, render() called tick() (advancing
     * the animation) while mouseClicked() called get() later — the two values
     * could differ, causing a positional drift of several pixels.
     */
    private float lastRenderedScroll = 0f;
    private float lastRenderedScale = 1f;
    
    private final List<Row> rows = new ArrayList<>();
    private final List<ModuleCard> cards = new ArrayList<>();

    private Screen parent;

    /**
     * Single source of truth for grid placement. Computed once per init from
     * {@code this.width / this.height} and reused by render, renderBackground
     * and mouseClicked so the visual cards, the background panel, the click
     * regions and the scroll math can never drift apart — that drift was the
     * "click module A → opens module B" bug at GUI scale 1, where 10–20 px of
     * misalignment between paths swallowed clicks aimed at a card edge.
     */
    private record GridLayout(int cardW, int cardH, int gap, int cols, int rowCount,
                              int totalW, int contentH, int boxX, int boxY, int boxW, int boxH,
                              int startX, int startY, int viewportTop, int viewportBottom) {}

    private GridLayout layout;

    private GridLayout computeGridLayout(int cardCount) {
        int cardH = 108;
        int gap = 10;
        int targetCardW = 150;
        int maxCols = 4;
        int minCardW = 130;

        // Panel padding: cards live inside the visible panel with sidePad on each
        // horizontal edge and headerPad / footerPad on top / bottom.
        int sidePad = 18;
        int headerPad = 24;
        int footerPad = 14;

        // Reserve 30 px of breathing room on each side of the panel.
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

        // Cards live between viewportTop and viewportBottom — clicks outside this
        // band must NOT hit a card even if the card's geometry happens to extend
        // there (e.g., a card scrolled out of the viewport).
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
        if (this.openAnim.target() == 0f) {
            this.openAnim.snap(0f);
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

        // Use a temporary card size for construction — real size is set after layout.
        int tmpW = 200, tmpH = 70;

        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Team HUD", "Health bars & Status", net.minecraft.item.Items.DIAMOND_HELMET.getDefaultStack(), () -> openSettings(TAB_HUD), () -> cfg.teamHudEnabled, v -> cfg.teamHudEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Indicators", "Player Arrows", net.minecraft.item.Items.COMPASS.getDefaultStack(), () -> openSettings(TAB_ARROWS), () -> cfg.onScreenEnabled, v -> cfg.onScreenEnabled = v));
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Notifications", "Smart Alerts", net.minecraft.item.Items.BELL.getDefaultStack(), () -> openSettings(TAB_NOTIFY), () -> cfg.notificationsEnabled, v -> cfg.notificationsEnabled = v));
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
        cards.add(new ModuleCard(0, 0, tmpW, tmpH, "Nametags", "Player Name Tags", net.minecraft.item.Items.NAME_TAG.getDefaultStack(), () -> openSettings(TAB_NAMETAG), () -> cfg.nametagEnabled, v -> cfg.nametagEnabled = v));

        if (FabricLoader.getInstance().isModLoaded("mahikariui")) {
            cards.add(new ModuleCard(0, 0, tmpW, tmpH, "UI Settings", "Animated BG", net.minecraft.item.Items.PAINTING.getDefaultStack(), () -> {
                if (this.client != null) {
                    try {
                        net.minecraft.client.gui.screen.Screen configScreen = (net.minecraft.client.gui.screen.Screen) Class.forName("mahikariui.core.config.screen.ConfigScreen").getConstructor(net.minecraft.client.gui.screen.Screen.class).newInstance(this);
                        this.client.setScreen(configScreen);
                    } catch (Exception e) {}
                }
            }, null, null));
        }

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
        
        int panelW = Math.min(380, this.width - 20);
        int panelH = this.height - 50;
        int panelX = (this.width - panelW) / 2;
        int panelY = 26;

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
        addBool(TAB_NOTIFY, "Show Progress Bar", () -> cfg.notificationShowProgress, v -> cfg.notificationShowProgress = v, def.notificationShowProgress).setCondition(notifAdvCond).indent();
        addBool(TAB_NOTIFY, "Show Icon Box", () -> cfg.notificationShowIconBox, v -> cfg.notificationShowIconBox = v, def.notificationShowIconBox).setCondition(notifAdvCond).indent();
        addBool(TAB_NOTIFY, "Show Shimmer Effect", () -> cfg.notificationShowShimmer, v -> cfg.notificationShowShimmer = v, def.notificationShowShimmer).setCondition(notifAdvCond).indent();
        addFloatSlider(TAB_NOTIFY, "Duration multiplier", () -> cfg.notificationDurationMul, v -> cfg.notificationDurationMul = (float)v, def.notificationDurationMul, 0.25, 3.0, "%.2fx").setCondition(notifAdvCond).indent();
        addIntSlider(TAB_NOTIFY, "Max notifications shown", () -> cfg.notificationMaxStack, v -> cfg.notificationMaxStack = v, def.notificationMaxStack, 1, 8, "%d").setCondition(notifAdvCond).indent();
        
        // PERFORMANCE TAB
        addCycle(TAB_PERFORMANCE, "UI Quality", () -> cfg.uiQuality, v -> cfg.uiQuality = v, def.uiQuality, new String[]{"MEDIUM", "LOW"}, this::filterLabel);
        addBool(TAB_PERFORMANCE, "No Background (ClickGui)", () -> cfg.clickGuiNoBackground, v -> cfg.clickGuiNoBackground = v, def.clickGuiNoBackground);
        
        // VISUALS TAB
        addBool(TAB_VISUALS, "Enable Visuals Module", () -> cfg.visualsEnabled, v -> cfg.visualsEnabled = v, def.visualsEnabled);
        Supplier<Boolean> visualsEnabled = () -> cfg.visualsEnabled;
        
        addBool(TAB_VISUALS, "FullBright (Nhìn xuyên đêm)", () -> cfg.fullBright, v -> cfg.fullBright = v, def.fullBright).setCondition(visualsEnabled);
        Supplier<Boolean> fullBrightEnabled = () -> cfg.visualsEnabled && cfg.fullBright;
        addFloatSlider(TAB_VISUALS, "Brightness Level", () -> cfg.fullBrightLevel, v -> cfg.fullBrightLevel = (float)v, def.fullBrightLevel, 0.0, 1.0, "%.2f").setCondition(fullBrightEnabled).indent();
        
        addBool(TAB_VISUALS, "NoFog (Xóa sương mù)", () -> cfg.noFog, v -> cfg.noFog = v, def.noFog).setCondition(visualsEnabled);
        Supplier<Boolean> noFogEnabled = () -> cfg.visualsEnabled && cfg.noFog;
        addFloatSlider(TAB_VISUALS, "Fog Density", () -> cfg.noFogDensity, v -> cfg.noFogDensity = (float)v, def.noFogDensity, 0.0, 1.0, "%.2f").setCondition(noFogEnabled).indent();

        addBool(TAB_VISUALS, "Clear Water / Lava (Nhìn xuyên chất lỏng)", () -> cfg.clearFluids, v -> cfg.clearFluids = v, def.clearFluids).setCondition(visualsEnabled);
        
        // HIDE ARMOR TAB
        addBool(TAB_HIDE_ARMOR, "Hide Player Armor", () -> cfg.hideArmor, v -> cfg.hideArmor = v, def.hideArmor);
        Supplier<Boolean> hideArmorEnabled = () -> cfg.hideArmor;
        addCycle(TAB_HIDE_ARMOR, "Apply to", () -> cfg.hideArmorMode, v -> cfg.hideArmorMode = v, def.hideArmorMode,
            new String[]{"ALL", "SELF_ONLY", "OTHERS_ONLY"}, this::filterLabel).setCondition(hideArmorEnabled).indent();

        // LOW FIRE TAB
        addBool(TAB_LOW_FIRE, "Low Fire (Giảm lửa che mắt)", () -> cfg.lowFire, v -> cfg.lowFire = v, def.lowFire);
        Supplier<Boolean> fireEnabled = () -> cfg.lowFire;
        addFloatSlider(TAB_LOW_FIRE, "Fire Offset", () -> cfg.fireHeight, v -> cfg.fireHeight = (float)v, def.fireHeight, 0.0, 1.0, "%.2f").setCondition(fireEnabled).indent();

        // LOW SHIELD TAB
        addBool(TAB_LOW_SHIELD, "Low Shield (Hạ thấp khiên)", () -> cfg.lowShield, v -> cfg.lowShield = v, def.lowShield);
        Supplier<Boolean> shieldEnabled = () -> cfg.lowShield;
        addFloatSlider(TAB_LOW_SHIELD, "Shield Offset", () -> cfg.shieldHeight, v -> cfg.shieldHeight = (float)v, def.shieldHeight, -1.0, 0.0, "%.2f").setCondition(shieldEnabled).indent();

        // LOW TOTEM TAB
        addBool(TAB_LOW_TOTEM, "Low Totem (Hạ thấp Totem)", () -> cfg.lowTotem, v -> cfg.lowTotem = v, def.lowTotem);
        Supplier<Boolean> totemEnabled = () -> cfg.lowTotem;
        addFloatSlider(TAB_LOW_TOTEM, "Totem Offset", () -> cfg.totemHeight, v -> cfg.totemHeight = (float)v, def.totemHeight, -1.0, 0.0, "%.2f").setCondition(totemEnabled).indent();
        addFloatSlider(TAB_LOW_TOTEM, "Totem Scale", () -> cfg.totemScale, v -> cfg.totemScale = (float)v, def.totemScale, 0.1, 1.5, "%.2fx").setCondition(totemEnabled).indent();

        // SMALL ITEMS TAB
        addBool(TAB_SMALL_ITEMS, "Small Items (Đồ cầm tay nhỏ)", () -> cfg.smallItems, v -> cfg.smallItems = v, def.smallItems);
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
        addBool(TAB_SPRINT, "AutoSprint (Tự động chạy nhanh)", () -> cfg.autoSprint, v -> cfg.autoSprint = v, def.autoSprint);
        Supplier<Boolean> sprintEnabled = () -> cfg.autoSprint;
        
        addBool(TAB_SPRINT, "Show HUD Indicator", () -> cfg.sprintingShowHud, v -> cfg.sprintingShowHud = v, def.sprintingShowHud).setCondition(sprintEnabled).indent();
        Supplier<Boolean> sprintHudEnabled = () -> cfg.autoSprint && cfg.sprintingShowHud;
        addBool(TAB_SPRINT, "Animated HUD", () -> cfg.sprintingAnimated, v -> cfg.sprintingAnimated = v, def.sprintingAnimated).setCondition(sprintHudEnabled).indent();
        addBool(TAB_SPRINT, "Show Background", () -> cfg.sprintingShowBackground, v -> cfg.sprintingShowBackground = v, def.sprintingShowBackground).setCondition(sprintHudEnabled).indent();
        addBool(TAB_SPRINT, "Show Bracket Border", () -> cfg.sprintingShowBorder, v -> cfg.sprintingShowBorder = v, def.sprintingShowBorder).setCondition(sprintHudEnabled).indent();
        addCycle(TAB_SPRINT, "Text Color", () -> cfg.sprintingTextColor, v -> cfg.sprintingTextColor = v, def.sprintingTextColor, new String[]{"WHITE", "GREEN", "RED", "BLUE", "YELLOW", "GOLD", "AQUA", "PINK"}, this::filterLabel).setCondition(sprintHudEnabled).indent();
        addFloatSlider(TAB_SPRINT, "Scale", () -> cfg.sprintingScale, v -> cfg.sprintingScale = (float)v, def.sprintingScale, 0.5, 3.0, "%.2fx").setCondition(sprintHudEnabled).indent();
        
        // NAMETAG TAB
        addBool(TAB_NAMETAG, "Enable Nametag", () -> cfg.nametagEnabled, v -> cfg.nametagEnabled = v, def.nametagEnabled);
        Supplier<Boolean> nametagEnabled = () -> cfg.nametagEnabled;
        addBool(TAB_NAMETAG, "No Background", () -> cfg.nametagNoBackground, v -> cfg.nametagNoBackground = v, def.nametagNoBackground).setCondition(nametagEnabled).indent();
        Supplier<Boolean> nametagBgEnabled = () -> cfg.nametagEnabled && !cfg.nametagNoBackground;
        addCycle(TAB_NAMETAG, "Background Color", () -> cfg.nametagBackgroundColor, v -> cfg.nametagBackgroundColor = v, def.nametagBackgroundColor,
            new String[]{"BLACK", "WHITE", "RED", "GREEN", "BLUE", "TRANSPARENT"}, this::filterLabel).setCondition(nametagBgEnabled).indent();
        addBool(TAB_NAMETAG, "Show Teammates Too", () -> cfg.nametagShowTeammates, v -> cfg.nametagShowTeammates = v, def.nametagShowTeammates).setCondition(nametagEnabled).indent();
        addBool(TAB_NAMETAG, "Show Own Nametag (3rd person)", () -> cfg.nametagShowSelf, v -> cfg.nametagShowSelf = v, def.nametagShowSelf).setCondition(nametagEnabled).indent();
        addFloatSlider(TAB_NAMETAG, "Scale", () -> cfg.nametagScale, v -> cfg.nametagScale = (float)v, def.nametagScale, 0.3, 3.0, "%.2fx").setCondition(nametagEnabled).indent();
        addFloatSlider(TAB_NAMETAG, "Max Distance", () -> cfg.nametagMaxDistance, v -> cfg.nametagMaxDistance = (float)v, def.nametagMaxDistance, 16.0, 512.0, "%.0f blocks").setCondition(nametagEnabled).indent();

        // CHAT TAB
        addBool(TAB_CHAT, "Smooth Chat", () -> cfg.smoothChat, v -> cfg.smoothChat = v, def.smoothChat);
        Supplier<Boolean> smoothChatEnabled = () -> cfg.smoothChat;
        addFloatSlider(TAB_CHAT, "Animation Speed", () -> cfg.smoothChatSpeed, v -> cfg.smoothChatSpeed = (float)v, def.smoothChatSpeed, 0.1, 3.0, "%.1fx").setCondition(smoothChatEnabled).indent();
        addBool(TAB_CHAT, "Infinite Chat (Chat vô hạn)", () -> cfg.infiniteChat, v -> cfg.infiniteChat = v, def.infiniteChat);
        
        // TEST TAB
        addAction(TAB_TEST, "HUD Layout Editor", "Open Editor", () -> {
            if (this.client != null) this.client.setScreen(new HudEditorScreen(this));
        });
        
        addAction(TAB_TEST, "Test Notifications", "Send Test", () -> {
            dev.mahikari.client.notification.NotificationManager.addNotification("§6§lLEGENDARY CÓ THỂ CRAFT", "Đã craft thành công Dragon Armor!", 0xFFCC00, 8000);
            dev.mahikari.client.notification.NotificationManager.addNotification("§b§lAIRDROP", "Airdrop đã rơi tại 100, 200", 0x00CCFF, 8000);
            dev.mahikari.client.notification.NotificationManager.addNotification("§c§lWARNING", "Sắp thu hẹp vòng bo!", 0xFF3333, 5000);
        });

        addAction(TAB_TEST, "Test Team HUD", "Spawn Fake Team", () -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.player != null) {
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                String world = mc.world.getRegistryKey().getValue().getPath();
                dev.mahikari.client.TeamViewManager mgr = dev.mahikari.client.MahikariClient.MANAGER;
                mgr.updatePosition("TestPlayer1", world, x + 5, y, z + 5, "king", "");
                mgr.updatePosition("TestPlayer2", world, x - 5, y, z - 5, "party", "");
                mgr.updatePosition("TestPlayer3", world, x + 10, y, z, "", "");
                mgr.updateHealth("TestPlayer1", 20.0f, 20.0f, 0.0f);
                mgr.updateHealth("TestPlayer2", 10.0f, 20.0f, 0.0f);
                mgr.updateHealth("TestPlayer3", 1.0f, 20.0f, 0.0f);
            }
        });

        addAction(TAB_TEST, "Test Team Damage & Heal", "Simulate", () -> {
            dev.mahikari.client.TeamViewManager mgr = dev.mahikari.client.MahikariClient.MANAGER;
            mgr.updateHealth("TestPlayer1", 5.0f, 20.0f, 0.0f); // Damage
            mgr.updateHealth("TestPlayer2", 20.0f, 20.0f, 0.0f); // Heal
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

        // Hide all rows initially to prevent them from flashing for 1 tick
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
        ButtonWidget btn = ButtonWidget.builder(styled("+"), b -> {
            expanded[0] = !expanded[0];
            b.setMessage(styled(expanded[0] ? "-" : "+"));
        }).dimensions(0, 0, 20, 20).build();
        Row r = new Row(section, "§e" + label, btn, null);
        r.visibilityCond = parentCond;
        rows.add(r);
        addDrawableChild(btn);
        return () -> expanded[0];
    }

    private String filterLabel(String v) {
        return v.replace("_", " ");
    }

    private Row addBool(int section, String label, Supplier<Boolean> get, Consumer<Boolean> set, boolean defVal) {
        ConfigToggle toggle = new ConfigToggle(0, 0, 48, 20, get, set);
        ButtonWidget reset = ButtonWidget.builder(styled("§7Reset"), b -> {
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
        ButtonWidget reset = ButtonWidget.builder(Text.literal("§7Reset"), b -> {
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
        ButtonWidget reset = ButtonWidget.builder(styled("§7Reset"), b -> {
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
        ButtonWidget reset = ButtonWidget.builder(styled("§7Reset"), b -> {
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
        ButtonWidget act = ButtonWidget.builder(styled("§f" + btnText), b -> action.run()).dimensions(0, 0, VALUE_W, 20).build();
        ButtonWidget reset = ButtonWidget.builder(styled("§7Reset"), b -> resetAction.run()).dimensions(0, 0, RESET_W, 20).build();
        Row r = new Row(section, label, act, reset);
        rows.add(r);
        addDrawableChild(act);
        addDrawableChild(reset);
        return r;
    }
    
    private Row addAction(int section, String label, String btnText, Runnable action) {
        ButtonWidget act = ButtonWidget.builder(styled("§f" + btnText), b -> action.run()).dimensions(0, 0, VALUE_W, 20).build();
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

        // Store the exact values used for rendering so mouseClicked can use
        // the same ones — eliminates positional drift between frames.
        float scale = isLowQuality ? 1.0f : (0.90f + 0.10f * animProgress);
        this.lastRenderedScroll = scrollOffset;
        this.lastRenderedScale = scale;

        if (!noBg) {
            // Modern gradient background with enhanced depth
            int bgAlpha1 = (int)(0xF5 * animProgress);
            int bgAlpha2 = (int)(0xEE * animProgress);
            int bgColor1 = (bgAlpha1 << 24) | 0x0A0B10;
            int bgColor2 = (bgAlpha2 << 24) | 0x050508;
            ctx.fillGradient(0, 0, this.width, this.height, bgColor1, bgColor2);
        }

        ctx.getMatrices().pushMatrix();
        // Enhanced open animation with smooth scale
        ctx.getMatrices().translate(this.width / 2.0f, this.height / 2.0f);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(-this.width / 2.0f, -this.height / 2.0f);

        if (!noBg) {
            // Draw modern header with gradient
            int headerH = 38;
            ctx.fillGradient(0, 0, this.width, headerH, applyAlpha(0xCC0F1419, animProgress), applyAlpha(0x880A0B10, animProgress));
            ctx.fill(0, headerH - 1, this.width, headerH, applyAlpha(0xFF00D9FF, animProgress * 0.3f));

            // Logo with glow effect
            int logoGlowAlpha = (int)(60 * animProgress);
            int logoGlow = (logoGlowAlpha << 24) | 0x00D9FF;
            ctx.fill(6, 6, 36, 36, logoGlow);

            ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, net.minecraft.util.Identifier.of("mahikari-client", "textures/gui/logo.png"), 9, 9, 0.0f, 0.0f, 24, 24, 24, 24);

            // Modern title with gradient text effect
            ctx.drawTextWithShadow(this.textRenderer, "§b§lMahikari", 40, 12, applyAlpha(0xFF00D9FF, animProgress));
            ctx.drawTextWithShadow(this.textRenderer, "§f§lClient", 40 + this.textRenderer.getWidth("Mahikari "), 12, applyAlpha(0xFFFFFFFF, animProgress));

            // Version badge
            String version = "v2.0";
            int versionW = this.textRenderer.getWidth(version);
            int badgeX = 40 + this.textRenderer.getWidth("Mahikari Client ") + 4;
            int badgeY = 14;
            fillRounded(ctx, badgeX, badgeY, badgeX + versionW + 8, badgeY + 12, applyAlpha(0x8800D9FF, animProgress), 6);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(badgeX + 4, badgeY + 2);
            ctx.getMatrices().scale(0.7f, 0.7f);
            ctx.drawTextWithShadow(this.textRenderer, version, 0, 0, applyAlpha(0xFFFFFFFF, animProgress));
            ctx.getMatrices().popMatrix();
        }

        super.render(ctx, mouseX, mouseY, delta);

        if (this.currentView == ViewMode.GRID) {
            GridLayout l = this.layout;
            if (l == null) {
                ctx.getMatrices().popMatrix();
                return;
            }

            // Scissor to the panel interior so cards scrolling past the edges
            // don't paint over the header/footer or the scrollbar.
            ctx.enableScissor(l.boxX(), l.viewportTop(), l.boxX() + l.boxW(), l.viewportBottom());
            for (ModuleCard c : cards) {
                c.render(ctx, mouseX, mouseY, delta);
            }
            ctx.disableScissor();

            // Modern scrollbar
            if (maxScroll > 0) {
                int trackX = l.boxX() + l.boxW() - 12;
                int trackW = 6;
                int thumbH = Math.max(24, (int)((float)l.boxH() * l.boxH() / l.contentH()));
                int thumbY = l.boxY() + (int)((l.boxH() - thumbH) * (scrollOffset / maxScroll));

                fillRounded(ctx, trackX, l.boxY() + 20, trackX + trackW, l.boxY() + l.boxH() - 2, 0x33FFFFFF, 3);
                fillRoundedGradient(ctx, trackX, thumbY, trackX + trackW, thumbY + thumbH, 0xFF00D9FF, 0xFF0099CC, 3);
                ctx.fillGradient(trackX + 1, thumbY + 1, trackX + trackW - 1, thumbY + 4, 0x44FFFFFF, 0x00FFFFFF);
            }

            ctx.getMatrices().popMatrix();
            return;
        }

        int panelW = Math.min(400, this.width - 40);
        int panelH = this.height - 66;
        int panelX = (this.width - panelW) / 2;
        int panelY = 32;
        int vpTop = panelY + 34;
        int vpBot = panelY + panelH - 2;

        int curY = vpTop + 4;
        int visibleCount = 0;

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
                    int maxValRight = panelX + panelW - 20;
                    if (r.resBtn != null) {
                        r.resBtn.setX(maxValRight - RESET_W);
                        r.resBtn.setY(displayY + 2);
                        maxValRight = r.resBtn.getX() - 8;
                    }
                    r.valBtn.setX(maxValRight - r.valBtn.getWidth());
                    r.valBtn.setY(displayY + 2);

                    int textX = panelX + 24 + r.indentLevel;
                    int textColor = r.indentLevel > 0 ? 0xFFB0B8CC : 0xFFFFFFFF;
                    ctx.drawText(this.textRenderer, styled(r.label), textX, displayY + 7, textColor, true);
                }
                curY += ROW_H;
                visibleCount++;
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
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("§7No settings found"), this.width / 2, vpTop + 30, 0xFFFFFFFF);
        }

        // Modern scrollbar for settings
        if (maxScroll > 0) {
            int trackX = panelX + panelW - 10;
            int trackW = 6;
            int thumbH = Math.max(24, (int)((float)viewH * viewH / totalH));
            int thumbY = vpTop + (int)((viewH - thumbH) * (scrollOffset / maxScroll));

            fillRounded(ctx, trackX, vpTop, trackX + trackW, vpBot, 0x33FFFFFF, 3);
            fillRoundedGradient(ctx, trackX, thumbY, trackX + trackW, thumbY + thumbH, 0xFF00D9FF, 0xFF0099CC, 3);
            ctx.fillGradient(trackX + 1, thumbY + 1, trackX + trackW - 1, thumbY + 4, 0x44FFFFFF, 0x00FFFFFF);
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
        if (s <= 0.001f || s >= 0.999f) return screenX; // No transform needed at scale ~1
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
        if (this.currentView == ViewMode.GRID && click.button() == 0) {
            GridLayout l = this.layout;
            if (l == null) return super.mouseClicked(click, bl);

            // Use the EXACT scroll offset that was used in the last render frame.
            // Previously this called scrollAnim.get() independently, which could
            // differ from the value used during render (render calls tick() which
            // advances the animation; get() called later returns a new value).
            float scrollOffset = this.lastRenderedScroll;

            // Inverse-transform the raw mouse coordinates to account for the
            // scale transform applied in render(). Without this, when scale < 1
            // the visual positions of cards are shifted inward but the click
            // detection used raw screen positions — causing a multi-pixel offset
            // that made clicks land on the wrong card or wrong button.
            double mx = toScaledX(click.x());
            double my = toScaledY(click.y());

            // NEW: Only check cards if the click is actually inside the visible viewport.
            // If it's outside (e.g. above/below the panel, or on the scrollbar),
            // we should let it fall through to super.mouseClicked so the Back button works.
            if (my >= l.viewportTop() && my <= l.viewportBottom() && mx >= l.boxX() && mx <= l.boxX() + l.boxW() - 16) {
                for (ModuleCard c : cards) {
                    int drawY = c.y - (int)scrollOffset;

                    // Skip cards scrolled out of the viewport.
                    if (drawY + c.height < l.viewportTop() || drawY > l.viewportBottom()) continue;

                    if (mx >= c.x && mx <= c.x + c.width && my >= drawY && my <= drawY + c.height) {

                        // Bottom full-width ENABLED/DISABLED (or OPEN) button — must match
                        // the visual button in ModuleCard.render (btnH=20, btnX=x+8, btnY=drawY+height-btnH-8, btnW=width-16).
                        int btnH = 20;
                        int btnX = c.x + 8;
                        int btnY = drawY + c.height - btnH - 8;
                        int btnW = c.width - 16;
                        if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                            if (c.getState != null && c.toggleState != null) {
                                c.toggleState.accept(!c.getState.get());
                                TeamViewConfig.save();
                            } else {
                                c.onClick.run();
                            }
                            net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.empty(), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                            return true;
                        }

                        // OPTIONS button top-right — must match ModuleCard.render (optW=48, optH=16, optX=x+width-optW-8, optY=drawY+10).
                        int optW = 48;
                        int optH = 16;
                        int optX = c.x + c.width - optW - 8;
                        int optY = drawY + 10;
                        if (mx >= optX && mx <= optX + optW && my >= optY && my <= optY + optH) {
                            c.onClick.run();
                            net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.empty(), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
                            return true;
                        }

                        // Click anywhere else on card → open settings
                        c.onClick.run();
                        net.minecraft.client.gui.widget.ButtonWidget.builder(net.minecraft.text.Text.empty(), b -> {}).build().playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
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

        if (TeamViewConfig.get().clickGuiNoBackground) return;

        if (this.currentView == ViewMode.GRID) {
            GridLayout l = this.layout;
            if (l == null) return;
            int boxX = l.boxX();
            int boxY = l.boxY();
            int boxW = l.boxW();
            int boxH = l.boxH();

            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, this.openAnim.get()));

            // Modern panel with enhanced depth
            int bgCol = applyAlpha(0xF5141820, animProgress);
            int borderCol = applyAlpha(0xFF1E2530, animProgress);
            int accentCol = applyAlpha(0x4400D9FF, animProgress);

            // Shadow layers
            fillRounded(ctx, boxX - 2, boxY + 2, boxX + boxW + 2, boxY + boxH + 2, applyAlpha(0x66000000, animProgress), 12);
            fillRounded(ctx, boxX - 1, boxY + 1, boxX + boxW + 1, boxY + boxH + 1, applyAlpha(0x44000000, animProgress), 12);

            // Main panel
            fillRounded(ctx, boxX, boxY, boxX + boxW, boxY + boxH, bgCol, 10);

            // Top accent line
            fillRounded(ctx, boxX + 4, boxY + 4, boxX + boxW - 4, boxY + 6, accentCol, 1);

            // Border
            ctx.fill(boxX, boxY, boxX + boxW, boxY + 1, borderCol);
            ctx.fill(boxX, boxY + boxH - 1, boxX + boxW, boxY + boxH, borderCol);
            ctx.fill(boxX, boxY, boxX + 1, boxY + boxH, borderCol);
            ctx.fill(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH, borderCol);

            // Title with modern styling
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("§l§nMODULES"), this.width / 2, boxY + 16, applyAlpha(0xFF00D9FF, animProgress));

            return;
        }

        if (this.currentView == ViewMode.SETTINGS) {
            int panelW = Math.min(400, this.width - 40);
            int panelH = this.height - 66;
            int panelX = (this.width - panelW) / 2;
            int panelY = 32;

            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, this.openAnim.get()));

            int bgCol = applyAlpha(0xF5141820, animProgress);
            int borderCol = applyAlpha(0xFF1E2530, animProgress);
            int accentCol = applyAlpha(0x4400D9FF, animProgress);

            // Shadow layers
            fillRounded(ctx, panelX - 2, panelY + 2, panelX + panelW + 2, panelY + panelH + 2, applyAlpha(0x66000000, animProgress), 12);
            fillRounded(ctx, panelX - 1, panelY + 1, panelX + panelW + 1, panelY + panelH + 1, applyAlpha(0x44000000, animProgress), 12);

            // Main panel
            fillRounded(ctx, panelX, panelY, panelX + panelW, panelY + panelH, bgCol, 10);

            // Top accent line
            fillRounded(ctx, panelX + 4, panelY + 4, panelX + panelW - 4, panelY + 6, accentCol, 1);

            // Border
            ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, borderCol);
            ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, borderCol);
            ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, borderCol);
            ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, borderCol);

            String title = activeTab == TAB_HUD ? "Team HUD Settings" :
                           activeTab == TAB_ARROWS ? "Indicators Settings" :
                           activeTab == TAB_NOTIFY ? "Notifications Settings" :
                           activeTab == TAB_EFFECTS ? "Effect HUD Settings" :
                           activeTab == TAB_SPRINT ? "Auto Sprint Settings" :
                           activeTab == TAB_CHAT ? "Chat Settings" :
                           activeTab == TAB_VISUALS ? "Visuals Settings" :
                           activeTab == TAB_HIDE_ARMOR ? "Hide Armor Settings" :
                           activeTab == TAB_LOW_FIRE ? "Low Fire Settings" :
                           activeTab == TAB_LOW_SHIELD ? "Low Shield Settings" :
                           activeTab == TAB_LOW_TOTEM ? "Low Totem Settings" :
                           activeTab == TAB_PERFORMANCE ? "Performance Settings" :
                           activeTab == TAB_SMALL_ITEMS ? "Small Items Settings" : "Tests & Tools";
            ctx.drawCenteredTextWithShadow(this.textRenderer, styled("§l" + title), panelX + panelW / 2, panelY + 12, applyAlpha(0xFF00D9FF, animProgress));

            ctx.fill(panelX + 8, panelY + 28, panelX + panelW - 8, panelY + 29, applyAlpha(0x33FFFFFF, animProgress));
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
        
        Row(int s, String l, ClickableWidget v, ClickableWidget r) {
            this.section = s; this.label = l; this.valBtn = v; this.resBtn = r;
        }
        
        Row indent() {
            this.indentLevel = 12;
            this.label = "§7↳ §f" + this.label;
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
            super(x, y, w, h, Text.empty(), max == min ? 0 : (double)(val - min)/(max - min));
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
            super(x, y, w, h, Text.empty(), max == min ? 0 : (val - min)/(max - min));
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
            if (this.iconId != null) return 0xFF5B6EE1; // Modern indigo for custom icons
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
            return 0xFF4A5568; // Modern gray
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

            // Use the same scroll offset that the main render() stored, so card
            // positions exactly match the click detection in mouseClicked().
            int drawY = y - (int)lastRenderedScroll;

            // Transform mouse coords to the scaled space for accurate hover detection
            double scaledMX = toScaledX(mouseX);
            double scaledMY = toScaledY(mouseY);
            boolean hovered = scaledMX >= x && scaledMX <= x + width && scaledMY >= drawY && scaledMY <= drawY + height;

            this.hoverAnim.setTarget(hovered ? 1f : 0f);
            float hProgress = isLowQuality ? (hovered ? 1f : 0f) : this.hoverAnim.tick();

            // Enhanced modern gradient colors
            int c1 = 0xFF1A1D2E;
            int c2 = 0xFF252A3F;
            int bgColor = isLowQuality ? (hovered ? c2 : c1) : interpolateColor(c1, c2, hProgress);
            int borderC1 = 0xFF2D3250;
            int borderC2 = 0xFF00D9FF; // Vibrant cyan accent on hover
            int borderColor = isLowQuality ? (hovered ? borderC2 : borderC1) : interpolateColor(borderC1, borderC2, hProgress);

            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, MahikariClickGui.this.openAnim.get()));
            bgColor = applyAlpha(bgColor, animProgress);
            borderColor = applyAlpha(borderColor, animProgress);

            // Modern card with enhanced shadow
            int shadowColor1 = applyAlpha(0x88000000, animProgress * 0.6f);
            int shadowColor2 = applyAlpha(0x44000000, animProgress * 0.4f);
            fillRounded(ctx, x - 2, drawY + 2, x + width + 2, drawY + height + 2, shadowColor1, isLowQuality ? 3 : 6);
            fillRounded(ctx, x - 1, drawY + 1, x + width + 1, drawY + height + 1, shadowColor2, isLowQuality ? 3 : 6);

            // Card background with gradient
            fillRoundedGradient(ctx, x, drawY, x + width, drawY + height, bgColor, darken(bgColor), isLowQuality ? 3 : 6);

            // Glowing border on hover
            if (hProgress > 0.01f) {
                int glowAlpha = (int)(hProgress * 100);
                int glowColor = (glowAlpha << 24) | (borderC2 & 0xFFFFFF);
                fillRounded(ctx, x - 1, drawY - 1, x + width + 1, drawY + height + 1, glowColor, isLowQuality ? 4 : 7);
            }

            // Border
            fillRounded(ctx, x, drawY, x + width, drawY + height, borderColor, isLowQuality ? 3 : 6);
            fillRounded(ctx, x + 1, drawY + 1, x + width - 1, drawY + height - 1, bgColor, isLowQuality ? 2 : 5);

            // Top highlight for depth
            int highlightColor = applyAlpha(0x33FFFFFF, animProgress);
            ctx.fillGradient(x + 2, drawY + 2, x + width - 2, drawY + 8, highlightColor, 0x00FFFFFF);

            // Lunar-style vertical layout:
            // - Icon top-left in colored square
            // - "OPTIONS" mini-button top-right
            // - Title below icon
            // - Full-width ENABLED/DISABLED button at bottom

            boolean isActive = getState != null && getState.get();

            // Icon box top-left
            int boxSize = 26;
            int boxX = x + 10;
            int boxY = drawY + 10;

            int baseColor = getIconBgColor();
            int topColor = isLowQuality ? baseColor : interpolateColor(baseColor, brighten(baseColor), hProgress * 0.3f);
            int botColor = darken(topColor);

            if (hProgress > 0.01f) {
                int iconGlowAlpha = (int)(hProgress * 80);
                int iconGlow = (iconGlowAlpha << 24) | (baseColor & 0xFFFFFF);
                fillRoundedGradient(ctx, boxX - 2, boxY - 2, boxX + boxSize + 2, boxY + boxSize + 2, iconGlow, 0x00000000, 8);
            }

            fillRoundedGradient(ctx, boxX, boxY, boxX + boxSize, boxY + boxSize, applyAlpha(topColor, animProgress), applyAlpha(botColor, animProgress), 6);
            ctx.fillGradient(boxX + 2, boxY + 2, boxX + boxSize - 2, boxY + 5, applyAlpha(0x44FFFFFF, animProgress), 0x00FFFFFF);

            // Render Icon (centered inside the box)
            ctx.getMatrices().pushMatrix();
            float iconScale = isLowQuality ? 0.9f : 0.9f + (0.08f * hProgress);
            float xOff = (boxSize - 16f * iconScale) / 2f;
            float yOff = (boxSize - 16f * iconScale) / 2f;
            ctx.getMatrices().translate(boxX + xOff, boxY + yOff);
            ctx.getMatrices().scale(iconScale, iconScale);

            if (this.iconId != null) {
                ctx.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.iconId, 0, 0, 0.0f, 0.0f, 16, 16, 16, 16);
            } else if (this.iconItem != null) {
                ctx.drawItem(this.iconItem, 0, 0);
            }
            ctx.getMatrices().popMatrix();

            if (client != null) {
                // OPTIONS mini-button top-right
                int optW = 48;
                int optH = 16;
                int optX = x + width - optW - 8;
                int optY = drawY + 10;
                int optBg = applyAlpha(0xFF2A3245, animProgress);
                int optBgDark = applyAlpha(MahikariClickGui.darken(0xFF2A3245, 0.85f), animProgress);
                fillRoundedGradient(ctx, optX, optY, optX + optW, optY + optH, optBg, optBgDark, 3);
                // Subtle border
                ctx.fill(optX, optY, optX + optW, optY + 1, applyAlpha(0xFF3D4860, animProgress));
                ctx.fill(optX, optY + optH - 1, optX + optW, optY + optH, applyAlpha(0xFF1A1F2E, animProgress));
                String optLabel = "OPTIONS";
                int optLabelW = client.textRenderer.getWidth(optLabel);
                ctx.drawTextWithShadow(client.textRenderer, styled(optLabel), optX + (optW - optLabelW) / 2, optY + 4, applyAlpha(0xFFFFFFFF, animProgress));

                // Title below icon
                String titleText = this.title;
                int titleY = boxY + boxSize + 8;
                ctx.drawTextWithShadow(client.textRenderer, styled("§l" + titleText), x + 10, titleY, applyAlpha(0xFFFFFFFF, animProgress));

                // Description (smaller, below title)
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(x + 10, titleY + 11f);
                ctx.getMatrices().scale(0.78f, 0.78f);
                ctx.drawTextWithShadow(client.textRenderer, styled(this.desc), 0, 0, applyAlpha(0xFFB0B8CC, animProgress));
                ctx.getMatrices().popMatrix();

                // Bottom full-width ENABLED/DISABLED button
                if (getState != null) {
                    int btnH = 20;
                    int btnX = x + 8;
                    int btnY = drawY + height - btnH - 8;
                    int btnW = width - 16;

                    int btnFill;
                    if (isActive) {
                        btnFill = 0xFF22C55E; // green
                    } else {
                        btnFill = 0xFFDC2626; // red
                    }
                    btnFill = applyAlpha(btnFill, animProgress);
                    fillRounded(ctx, btnX, btnY, btnX + btnW, btnY + btnH, btnFill, 3);

                    String label = isActive ? "ENABLED" : "DISABLED";
                    int lblW = client.textRenderer.getWidth(label);
                    ctx.drawTextWithShadow(client.textRenderer, styled("§l" + label), btnX + (btnW - lblW) / 2, btnY + 6, applyAlpha(0xFFFFFFFF, animProgress));
                } else {
                    // No toggle (e.g. action-only card like Tests & Tools) — show "OPEN" button instead
                    int btnH = 20;
                    int btnX = x + 8;
                    int btnY = drawY + height - btnH - 8;
                    int btnW = width - 16;

                    int btnFill = applyAlpha(0xFF3B82F6, animProgress);
                    fillRounded(ctx, btnX, btnY, btnX + btnW, btnY + btnH, btnFill, 3);

                    String label = "OPEN";
                    int lblW = client.textRenderer.getWidth(label);
                    ctx.drawTextWithShadow(client.textRenderer, styled("§l" + label), btnX + (btnW - lblW) / 2, btnY + 6, applyAlpha(0xFFFFFFFF, animProgress));
                }
            }
        }
    }
    
    private class ConfigToggle extends net.minecraft.client.gui.widget.ClickableWidget {
        private final Supplier<Boolean> getter;
        private final Consumer<Boolean> setter;
        private float animationProgress;
        
        ConfigToggle(int x, int y, int w, int h, Supplier<Boolean> get, Consumer<Boolean> set) {
            super(x, y, w, h, Text.empty());
            this.getter = get;
            this.setter = set;
            this.animationProgress = get.get() ? 1.0f : 0.0f;
        }
        
        @Override
        public void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            boolean isLowQuality = "LOW".equals(TeamViewConfig.get().uiQuality);
            boolean state = getter.get();
            float target = state ? 1.0f : 0.0f;

            if (isLowQuality) {
                animationProgress = target;
            } else {
                if (Math.abs(target - animationProgress) > 0.01f) {
                    animationProgress += (target - animationProgress) * 0.35f;
                } else {
                    animationProgress = target;
                }
            }

            // Compact colors
            int c1 = 0xFF3A4060;
            int c2 = 0xFF00D9FF;
            if (isHovered()) {
                c1 = 0xFF4A5070;
                c2 = 0xFF00EEFF;
            }

            int colorBg = isLowQuality ? (state ? c2 : c1) : interpolateColor(c1, c2, animationProgress);
            float animProgress = isLowQuality ? 1.0f : Math.max(0f, Math.min(1f, MahikariClickGui.this.openAnim.get()));
            colorBg = applyAlpha(colorBg, animProgress);

            // Compact toggle background - minimal rounding
            int bgDark = MahikariClickGui.darken(colorBg, 0.85f);
            fillRoundedGradient(ctx, getX(), getY(), getX() + getWidth(), getY() + getHeight(), colorBg, bgDark, 2);

            // Inner shadow
            ctx.fillGradient(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 2, applyAlpha(0x33000000, animProgress), 0x00000000);

            // Compact animated thumb
            int thumbW = 12;
            int maxTravel = getWidth() - thumbW - 4;
            int thumbX = getX() + 2 + (int)(maxTravel * animationProgress);

            // Thumb shadow
            fillRounded(ctx, thumbX + 1, getY() + 3, thumbX + thumbW + 1, getY() + getHeight() - 1, applyAlpha(0x44000000, animProgress), 2);

            // Thumb - minimal rounding
            fillRoundedGradient(ctx, thumbX, getY() + 2, thumbX + thumbW, getY() + getHeight() - 2, applyAlpha(0xFFFFFFFF, animProgress), applyAlpha(0xFFE8E8E8, animProgress), 2);

            // Thumb highlight
            ctx.fillGradient(thumbX + 1, getY() + 3, thumbX + thumbW - 1, getY() + 4, applyAlpha(0x44FFFFFF, animProgress), 0x00FFFFFF);

            // NO TEXT - just visual toggle
        }
        
        @Override
        public boolean mouseClicked(Click click, boolean bl) {
            double mouseX = click.x();
            double mouseY = click.y();
            if (this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.getWidth() && mouseY < this.getY() + this.getHeight() && click.button() == 0) {
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
}
