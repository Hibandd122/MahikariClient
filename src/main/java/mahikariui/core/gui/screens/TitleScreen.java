/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.gui.screen.option.OptionsScreen
 *  net.minecraft.client.realms.gui.screen.RealmsMainScreen
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
 *  net.minecraft.client.gui.screen.world.SelectWorldScreen
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.screens;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.Constants;
import mahikariui.core.config.Config;
import mahikariui.core.gui.button.ImageButton;
import mahikariui.core.gui.button.TextIconButton;
import mahikariui.core.gui.layout.FlexLayout;
import mahikariui.core.gui.layout.LayoutBuilder;
import mahikariui.core.gui.layout.Spacing;
import mahikariui.core.gui.layout.UIAnchor;
import mahikariui.core.gui.screens.AgreementPopupScreen;
import mahikariui.core.gui.widget.AnimatedWidget;
import mahikariui.core.gui.widget.VersionInfoWidget;

public class TitleScreen
extends Screen {
    private boolean cleanMode = false;
    private float gradientProgress = 1.0f;
    private float versionAlphaProgress = 1.0f;
    private final java.util.List<mahikariui.core.gui.widget.AnimatedWidget> animatedWidgets = new java.util.ArrayList<>();
    private static boolean hasInitialized = false;
    private boolean isReturningFromSubscreen = false;
    private static long introStartTime = -1;
    private static final long INTRO_DURATION_MS = 3500;
    Identifier VERSION_INFO_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/version_info.png");
    Identifier SINGLEPLAYER_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/sword_icon.png");
    Identifier MULTIPLAYER_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/signal_icon.png");
    Identifier OPTIONS_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/settings_icon.png");
    Identifier REALMS_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/realms_icon.png");
    Identifier EXIT_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/exit_icon.png");
    Identifier MODS_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/cube_icon.png");
    Identifier CLEAN_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/clean_icon.png");
    Identifier MAHIKARI_ICON_TEXTURE = Identifier.of("mahikariui", "textures/gui/mahikari_icon.png");
    
    private static final String LOGO_TEXT = "MAHIKARI UI";
    private int logoTextWidth = -1;

    public TitleScreen() {
        super(net.minecraft.text.Text.literal((String)"Title Screen"));
        this.isReturningFromSubscreen = hasInitialized;
    }

    protected void init() {
        if (!hasInitialized && introStartTime == -1) {
            introStartTime = System.currentTimeMillis();
            mahikariui.core.background.BackgroundBuilder.selectBackground(Config.getInstance().getBackground());
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (!Config.getInstance().hasAgreed()) {
            client.setScreen((Screen)new AgreementPopupScreen(this));
            return;
        }
        this.animatedWidgets.clear();
        FlexLayout mainMenu = new FlexLayout(FlexLayout.Direction.COLUMN, UIAnchor.CENTER_LEFT, 4, FlexLayout.Alignment.START, FlexLayout.Alignment.START).padding(Spacing.horizontal(20));
        mainMenu.add(new VersionInfoWidget(0, 0, 120, 50, this.VERSION_INFO_TEXTURE, 0.8f));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 45, net.minecraft.text.Text.translatable((String)"mahikariui.menu.play"), button -> client.setScreen((Screen)new SelectWorldScreen((Screen)this)), TextIconButton.TextAlign.LEFT, this.SINGLEPLAYER_ICON_TEXTURE, 1.8f, 35));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 20, net.minecraft.text.Text.translatable((String)"mahikariui.menu.multiplayer"), button -> client.setScreen((Screen)new MultiplayerScreen((Screen)this)), TextIconButton.TextAlign.LEFT, this.MULTIPLAYER_ICON_TEXTURE, 1.0f, 12));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 20, net.minecraft.text.Text.translatable((String)"mahikariui.menu.options"), button -> client.setScreen((Screen)new OptionsScreen((Screen)this, client.options)), TextIconButton.TextAlign.LEFT, this.OPTIONS_ICON_TEXTURE, 1.0f, 12));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 20, net.minecraft.text.Text.translatable((String)"mahikariui.menu.realms"), button -> client.setScreen((Screen)new RealmsMainScreen((Screen)this)), TextIconButton.TextAlign.LEFT, this.REALMS_ICON_TEXTURE, 1.0f, 12));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 20, net.minecraft.text.Text.translatable((String)"mahikariui.menu.mods"), button -> {
            if (Constants.MOD_SCREEN_SUPPLIER != null) {
                client.setScreen(Constants.MOD_SCREEN_SUPPLIER.getModListScreen(this));
            }
        }, TextIconButton.TextAlign.LEFT, this.MODS_ICON_TEXTURE, 1.0f, 12));
        mainMenu.add((ClickableWidget)new TextIconButton(0, 0, 120, 20, net.minecraft.text.Text.literal((String)"UI Settings"), button -> client.setScreen((Screen)new mahikariui.core.config.screen.ConfigScreen((Screen)this)), TextIconButton.TextAlign.LEFT, this.OPTIONS_ICON_TEXTURE, 1.0f, 12));
        for (ClickableWidget widget : mainMenu.build(this.width, this.height)) {
            AnimatedWidget animated = new AnimatedWidget(widget, AnimatedWidget.Direction.LEFT);
            if (this.isReturningFromSubscreen) {
                animated.opacity = 1.0f;
                animated.slideIn();
            }
            this.addDrawableChild(animated);
            this.animatedWidgets.add(animated);
        }
        LayoutBuilder utilityButtons = new LayoutBuilder();
        int cleanModeSize = 16;
        int cleanX = UIAnchor.BOTTOM_RIGHT.resolveX(this.width, cleanModeSize);
        int cleanY = UIAnchor.BOTTOM_RIGHT.resolveY(this.height, cleanModeSize);
        AnimatedWidget cleanModeButton = new AnimatedWidget((ClickableWidget)new ImageButton(cleanX, cleanY, cleanModeSize, cleanModeSize, this.CLEAN_ICON_TEXTURE, button -> this.toggleCleanMode(), net.minecraft.text.Text.translatable((String)"mahikariui.menu.clean_mode")), AnimatedWidget.Direction.RIGHT).cleanModeIgnore();
        if (this.isReturningFromSubscreen) {
            cleanModeButton.opacity = 1.0f;
            cleanModeButton.slideIn();
        }
        this.addDrawableChild(cleanModeButton);
        ImageButton exitButton = new ImageButton(0, 0, 16, 16, this.EXIT_ICON_TEXTURE, button -> client.scheduleStop(), net.minecraft.text.Text.translatable((String)"mahikariui.menu.close"));
        utilityButtons.addWidget((ClickableWidget)exitButton).alignHorizontal(LayoutBuilder.WidgetConfig.Alignment.END).alignVertical(LayoutBuilder.WidgetConfig.Alignment.START).margin(Spacing.all(8)).add();
        for (ClickableWidget class_3392 : utilityButtons.build(this.width, this.height)) {
            AnimatedWidget animated = new AnimatedWidget(class_3392, AnimatedWidget.Direction.RIGHT);
            if (this.isReturningFromSubscreen) {
                animated.opacity = 1.0f;
                animated.slideIn();
            }
            this.addDrawableChild(animated);
            this.animatedWidgets.add(animated);
        }
        if (this.cleanMode) {
            for (AnimatedWidget animatedWidget : this.animatedWidgets) {
                animatedWidget.slideOut();
            }
        }
        hasInitialized = true;
    }

    public void renderBackground(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        // Trigger ScreenMixin to draw the Global Animated Background (with proper scaling)
        super.renderBackground(graphics, mouseX, mouseY, delta);
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return (float)(1.0 + c3 * Math.pow(x - 1.0, 3.0) + c1 * Math.pow(x - 1.0, 2.0));
    }
    
    private float easeOutExpo(float x) {
        return x == 1.0f ? 1.0f : (float)(1.0 - Math.pow(2.0, -10.0 * x));
    }

    public void render(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        long timeSinceIntro = System.currentTimeMillis() - introStartTime;
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (timeSinceIntro < INTRO_DURATION_MS && introStartTime != -1) {
            this.renderBackground(graphics, mouseX, mouseY, delta);
            
            float progress = (float)timeSinceIntro / INTRO_DURATION_MS;
            float bgAlpha = progress > 0.8f ? 1.0f - (progress - 0.8f) / 0.2f : 1.0f;
            int bgColor = ((int)(bgAlpha * 255.0f) << 24) | 0x000000;
            graphics.fill(0, 0, this.width, this.height, bgColor);
            
            if (progress < 0.8f) {
                float introPhase = progress / 0.8f; // 0.0 to 1.0
                
                // Pop-in scale with easeOutBack
                float popProgress = Math.min(1.0f, introPhase / 0.4f);
                float logoScale = 2.0f * (popProgress < 1.0f ? easeOutBack(popProgress) : 1.0f);
                
                // Slow drift after pop
                float driftY = introPhase * -10.0f;
                
                graphics.getMatrices().pushMatrix();
                graphics.getMatrices().translate((float)this.width / 2.0f, ((float)this.height / 2.0f) + driftY);
                graphics.getMatrices().scale(logoScale, logoScale);
                
                graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, MAHIKARI_ICON_TEXTURE, -16, -24, 0.0f, 0.0f, 32, 32, 32, 32);
                
                if (this.logoTextWidth == -1) {
                    this.logoTextWidth = client.textRenderer.getWidth(LOGO_TEXT);
                }
                
                // net.minecraft.text.Text sliding up and fading in
                float textProgress = Math.max(0.0f, Math.min(1.0f, (introPhase - 0.2f) / 0.3f));
                if (textProgress > 0.0f) {
                    float textEase = easeOutExpo(textProgress);
                    float textYOffset = 20.0f - (textEase * 8.0f); // Slide from 20 to 12
                    int textAlpha = (int)(textEase * 255.0f);
                    int textColor = (textAlpha << 24) | 0x00FFFF;
                    
                    graphics.drawTextWithShadow(client.textRenderer, LOGO_TEXT, -this.logoTextWidth / 2, (int)textYOffset, textColor);
                }
                
                graphics.getMatrices().popMatrix();
                
                // Fade overlay over the logo elements
                float fadeOverlayAlpha = 0.0f;
                if (introPhase < 0.1f) {
                    fadeOverlayAlpha = 1.0f - (introPhase / 0.1f);
                } else if (introPhase > 0.8f) {
                    fadeOverlayAlpha = (introPhase - 0.8f) / 0.2f;
                }
                
                if (fadeOverlayAlpha > 0.0f) {
                    int fadeColor = ((int)(fadeOverlayAlpha * 255.0f) << 24) | 0x000000;
                    graphics.fill(0, 0, this.width, this.height, fadeColor);
                }
            }
            return;
        }

        float alphaSpeed = 0.1f;
        this.renderBackground(graphics, mouseX, mouseY, delta);
        this.versionAlphaProgress = this.cleanMode ? Math.max(0.0f, this.versionAlphaProgress - alphaSpeed * delta) : Math.min(1.0f, this.versionAlphaProgress + alphaSpeed * delta);
        int mcVersionX = UIAnchor.BOTTOM_LEFT.resolveX(this.width, 10);
        int mcVersionY = UIAnchor.BOTTOM_LEFT.resolveY(this.height, 10);
        float scale = 0.75f;
        if (this.versionAlphaProgress > 0.0f) {
            int alpha = (int)(this.versionAlphaProgress * 255.0f);
            int whiteWithAlpha = 0xFFFFFF | alpha << 24;
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().scale(scale, scale);
            graphics.drawTextWithShadow(client.textRenderer, "Minecraft " + Constants.MCVersion + " \u00a9 Mojang AB", (int)((float)mcVersionX / scale), (int)((float)mcVersionY / scale), whiteWithAlpha);
            graphics.getMatrices().popMatrix();
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    private void toggleCleanMode() {
        this.cleanMode = !this.cleanMode;
        for (AnimatedWidget widget : this.animatedWidgets) {
            if (this.cleanMode) {
                widget.slideOut();
                continue;
            }
            widget.slideIn();
        }
    }

    public void removed() {
        super.removed();
    }

    public static void resetInitializationState() {
        hasInitialized = false;
    }
}

