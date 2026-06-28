/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.textures.RenderPipelines
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.client.gui.Click
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.util.math.ColorHelper
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.config.screen;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;


import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.Constants;
import mahikariui.core.background.Background;
import mahikariui.core.config.Config;
import mahikariui.core.gui.button.ImageButton;
import mahikariui.core.gui.button.TextButton;
import mahikariui.core.gui.slider.ConfigSlider;
import mahikariui.core.gui.widget.AnimatedWidget;

public class ConfigScreen
extends Screen {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private final Screen parent;
    private int selectedBackgroundIndex = 0;
    private List<String> animatedBackgrounds;
    private final java.util.Map<String, net.minecraft.util.Identifier[]> backgroundFramesCache = new java.util.HashMap<>();
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 10;
    private static final float PREVIEW_WIDTH_RATIO = 0.4f;
    private static final float PREVIEW_HEIGHT_RATIO = 0.4f;
    private static final float CAROUSEL_RADIUS = 400.0f;
    private float currentRotation = 0.0f;
    private float targetRotation = 0.0f;
    private final long startTime;
    Identifier BACK_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/back_icon.png");
    Identifier NEXT_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/next_icon.png");
    Identifier PREV_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/prev_icon.png");

    public ConfigScreen(Screen parent) {
        super(net.minecraft.text.Text.literal((String)"Background Settings"));
        this.parent = parent;
        this.startTime = System.currentTimeMillis();
    }

    protected void init() {
        this.animatedBackgrounds = this.getAvailableAnimatedBackgrounds();
        if (!this.animatedBackgrounds.contains("default")) {
            this.animatedBackgrounds.addFirst("default");
        }
        if (!this.animatedBackgrounds.contains("none")) {
            this.animatedBackgrounds.addFirst("none");
        }
        int backSize = Math.min(16, this.height / 20);
        this.addDrawableChild(new AnimatedWidget((ClickableWidget)new ImageButton(10, 10, backSize, backSize, this.BACK_ICON_TEXTURE, button -> client.setScreen(this.parent), net.minecraft.text.Text.translatable((String)"mahikariui.config.back")), AnimatedWidget.Direction.LEFT));
        int previewWidth = (int)((float)this.width * 0.4f);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int navButtonSize = Math.min(16, this.width / 15);
        this.addDrawableChild(new AnimatedWidget((ClickableWidget)new ImageButton(centerX - previewWidth / 2 - navButtonSize - 20, centerY - navButtonSize / 4, navButtonSize, navButtonSize, this.PREV_ICON_TEXTURE, button -> this.switchBackground(-1), net.minecraft.text.Text.translatable((String)"mahikariui.config.prev")), AnimatedWidget.Direction.LEFT));
        this.addDrawableChild(new AnimatedWidget((ClickableWidget)new ImageButton(centerX + previewWidth / 2 + 20, centerY - navButtonSize / 4, navButtonSize, navButtonSize, this.NEXT_ICON_TEXTURE, button -> this.switchBackground(1), net.minecraft.text.Text.translatable((String)"mahikariui.config.next")), AnimatedWidget.Direction.RIGHT));
        this.addDrawableChild(new ConfigSlider(this.width / 2 - 100, this.height - 40 - 30, 200, 20, 1, 60, Config.getInstance().getBackgroundFrame(), val -> {
            Config.getInstance().setBackgroundFrame((int)val);
            Config.getInstance().save();
        }));
        
        net.minecraft.text.Text lowQualityText = net.minecraft.text.Text.translatable("mahikariui.config.low.quality.mode").append(net.minecraft.text.Text.translatable(Config.getInstance().isLowQualityMode() ? "mahikariui.config.low.quality.mode.enable" : "mahikariui.config.low.quality.mode.disable"));
        TextButton lowQualityBtn = new TextButton(this.width / 2 - 100, this.height - 100, 200, 20, lowQualityText, button -> {
            boolean current = Config.getInstance().isLowQualityMode();
            Config.getInstance().setLowQualityMode(!current);
            Config.getInstance().save(); // ADDED: Save setting when clicked
            ((TextButton)button).setDynamicMessage(net.minecraft.text.Text.translatable("mahikariui.config.low.quality.mode").append(net.minecraft.text.Text.translatable(!current ? "mahikariui.config.low.quality.mode.enable" : "mahikariui.config.low.quality.mode.disable")));
        }, TextButton.TextAlign.CENTER);
        this.addDrawableChild(new AnimatedWidget((ClickableWidget)lowQualityBtn, AnimatedWidget.Direction.LEFT));
        int buttonWidth = Math.min(200, this.width - 40);
        int halfBtnWidth = buttonWidth / 2 - 5;
        
        // Import Button (image or MP4 video)
        this.addDrawableChild(new TextButton(this.width / 2 - buttonWidth / 2, this.height - 20 - 20, halfBtnWidth, 20, net.minecraft.text.Text.literal((String)"Import Image / MP4"), button -> {
            java.io.File bgDir = new java.io.File(Constants.backgroundDir);
            if (!bgDir.exists()) bgDir.mkdirs();
            net.minecraft.util.Util.getOperatingSystem().open(bgDir.toURI()); // Util.getOperatingSystem().open(URI)
        }, TextButton.TextAlign.CENTER));
        
        // Apply Button
        this.addDrawableChild(new TextButton(this.width / 2 + 5, this.height - 20 - 20, halfBtnWidth, 20, net.minecraft.text.Text.translatable((String)"mahikariui.config.background.apply"), button -> {
            String selected = this.animatedBackgrounds.get(this.selectedBackgroundIndex);
            Config.getInstance().setBackground(selected);
            Config.getInstance().save();
            client.setScreen(this.parent);
        }, TextButton.TextAlign.CENTER));
        String currentBg = Config.getInstance().getBackground();
        this.selectedBackgroundIndex = Math.max(0, this.animatedBackgrounds.indexOf(currentBg));
        this.currentRotation = this.targetRotation = (float)this.selectedBackgroundIndex * (360.0f / (float)Math.max(1, this.animatedBackgrounds.size()));
    }

    private void switchBackground(int delta) {
        if (this.animatedBackgrounds.isEmpty()) {
            return;
        }
        this.selectedBackgroundIndex = (this.selectedBackgroundIndex + delta + this.animatedBackgrounds.size()) % this.animatedBackgrounds.size();
        this.targetRotation += (float)delta * (360.0f / (float)this.animatedBackgrounds.size());
    }

    /** One shared worker for MP4 thumbnail decode; daemon so it doesn't block JVM shutdown. */
    private static final java.util.concurrent.ExecutorService VIDEO_THUMB_EXECUTOR =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "MahikariUI-VideoThumb");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });

    /** Names of videos whose thumbnail decode is already in-flight or done — avoids duplicate work. */
    private static final java.util.Set<String> thumbsScheduled = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private List<String> getAvailableAnimatedBackgrounds() {
        String[] backgrounds;
        ArrayList<String> validBackgrounds = new ArrayList<String>();
        for (String name : backgrounds = Background.getAvailableBackgrounds()) {
            Identifier[] frames = Background.loadAnimatedFramePaths(name);
            if (frames == null || frames.length <= 0) continue;
            validBackgrounds.add(name);
            this.backgroundFramesCache.put(name, frames);

            // For MP4 backgrounds: decode the first frame on a low-priority background thread,
            // then register it as a texture from the MC main thread. Keeps ConfigScreen open instantly
            // even when the user has many videos.
            java.io.File videoFile = Background.findVideoFile(name);
            if (videoFile != null && frames.length == 1 && thumbsScheduled.add(name)) {
                final Identifier thumbId = frames[0];
                VIDEO_THUMB_EXECUTOR.submit(() -> {
                    net.minecraft.client.texture.NativeImage firstFrame = mahikariui.core.background.VideoFrameDecoder.decodeFirstFrame(videoFile);
                    if (firstFrame == null) return;
                    client.execute(() -> {
                        net.minecraft.client.texture.TextureManager tm = client.getTextureManager();
                        net.minecraft.client.texture.AbstractTexture old = tm.getTexture(thumbId);
                        if (old != null) old.close();
                        tm.registerTexture(thumbId, new NativeImageBackedTexture(() -> "mahikariui_video_thumb_" + name, firstFrame));
                    });
                });
            }
        }
        return validBackgrounds;
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        if (!this.animatedBackgrounds.isEmpty()) {
            float angleStep = 360.0f / (float)this.animatedBackgrounds.size();
            for (int i = 0; i < this.animatedBackgrounds.size(); ++i) {
                float angle = (float)Math.toRadians((float)i * angleStep - this.currentRotation);
                float x = (float)centerX + (float)Math.sin(angle) * 400.0f;
                float z = (float)Math.cos(angle) * 400.0f;
                float scale = 0.5f + 0.5f * (z + 400.0f) / 800.0f;
                if (!(z > 0.0f)) continue;
                int previewWidth = (int)((float)this.width * 0.4f * scale);
                int previewHeight = (int)((float)this.height * 0.4f * scale);
                int previewX = (int)x - previewWidth / 2;
                int previewY = centerY - previewHeight / 2;
                if (!(mouseX >= (double)previewX) || !(mouseX <= (double)(previewX + previewWidth)) || !(mouseY >= (double)previewY) || !(mouseY <= (double)(previewY + previewHeight))) continue;
                this.selectedBackgroundIndex = i;
                this.targetRotation = (float)i * angleStep;
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    public void tick() {
        super.tick();
    }

    public void renderBackground(@NotNull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        // Trigger ScreenMixin to draw the Global Animated Background first!
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);
        
        float rotationDiff = this.targetRotation - this.currentRotation;
        if (Math.abs(rotationDiff) > 0.01f) {
            this.currentRotation += rotationDiff * 0.1f;
        } else {
            this.currentRotation = this.targetRotation;
        }
        
        // Removed deep modern gradient background overlay to keep the animated background 100% visible
        
        if (this.animatedBackgrounds.isEmpty()) {
            return;
        }
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        float angleStep = 360.0f / (float)this.animatedBackgrounds.size();
        ArrayList<PreviewItem> previewItems = new ArrayList<PreviewItem>();
        for (int i = 0; i < this.animatedBackgrounds.size(); ++i) {
            float angle = (float)Math.toRadians((float)i * angleStep - this.currentRotation);
            float x = (float)centerX + (float)Math.sin(angle) * 400.0f;
            float z = (float)Math.cos(angle) * 400.0f;
            previewItems.add(new PreviewItem(i, x, z));
        }
        previewItems.sort((a, b) -> Float.compare(a.z, b.z));
        
        for (PreviewItem item : previewItems) {
            String currentBg = this.animatedBackgrounds.get(item.index);
            Identifier[] frames;
            float scale = 0.5f + 0.5f * (item.z + 400.0f) / 800.0f;
            float previewScaleFactor = 0.6f;
            int previewWidth = (int)((float)this.width * 0.4f * scale * previewScaleFactor);
            int previewHeight = (int)((float)this.height * 0.4f * scale * previewScaleFactor);
            int previewX = (int)item.x - previewWidth / 2;
            int previewY = centerY - previewHeight / 2 - 60;
            
            float alpha = (item.z + 400.0f) / 800.0f;
            boolean isSelected = (item.index == this.selectedBackgroundIndex);
            
            // Draw clean border for selected item
            if (isSelected) {
                // Elegant white border
                graphics.fill(previewX - 2, previewY - 2, previewX + previewWidth + 2, previewY + previewHeight + 2, 0xFFFFFFFF);
            } else {
                // Subtle dark border for unselected
                graphics.fill(previewX - 1, previewY - 1, previewX + previewWidth + 1, previewY + previewHeight + 1, 0x88111111);
            }
            
            // Background fill
            graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0xFF1A1A1A);
            
            if (!currentBg.equalsIgnoreCase("none")) {
                frames = this.backgroundFramesCache.get(currentBg);
                if (frames != null && frames.length > 0) {
                    graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, frames[0], previewX, previewY, 0.0f, 0.0f, previewWidth, previewHeight, previewWidth, previewHeight);
                }
            } else {
                int noneWidth = this.textRenderer.getWidth("VANILLA");
                graphics.drawTextWithShadow(this.textRenderer, "VANILLA", previewX + (previewWidth - noneWidth) / 2, previewY + previewHeight / 2 - 4, 0xFFAAAAAA);
            }
            
            // Dim unselected items
            if (!isSelected) {
                int dimAlpha = (int)((1.0f - alpha) * 200.0f);
                graphics.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, (dimAlpha << 24) | 0x000000);
                continue;
            }
            
            // Name badge for selected item
            String displayName = currentBg.toUpperCase().replace("_", " ");
            int nameWidth = this.textRenderer.getWidth(displayName);
            int textX = previewX + (previewWidth - nameWidth) / 2;
            int textY = previewY + previewHeight + 14;
            
            graphics.fill(textX - 8, textY - 4, textX + nameWidth + 8, textY + 12, 0xAA000000); // Badge background
            graphics.drawTextWithShadow(this.textRenderer, displayName, textX, textY, 0xFF00E5FF); // Cyan premium text
        }
        
        String title = Constants.TRANSLATOR.translate("mahikariui.config.title", new Object[0]);
        int titleWidth = this.textRenderer.getWidth(title);
        graphics.drawTextWithShadow(this.textRenderer, title, (this.width - titleWidth) / 2, 20, 0xFFFFD700); // Gold Title
    }

    public void render(@NotNull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private record PreviewItem(int index, float x, float z) {
    }
}

