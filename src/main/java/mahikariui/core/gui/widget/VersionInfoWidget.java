/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.client.sound.SoundManager
 *  net.minecraft.client.gui.Click
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
 *  net.minecraft.util.math.ColorHelper
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.widget;

import java.util.Objects;

import net.minecraft.client.sound.SoundManager;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import org.jetbrains.annotations.NotNull;

public class VersionInfoWidget
extends ClickableWidget {
    private final Identifier texture;
    private final float scale;
    private float externalOpacity = 1.0f;

    public VersionInfoWidget(int x, int y, int width, int height, Identifier texture, float scale) {
        super(x, y, width, height, net.minecraft.text.Text.literal(""));
        this.texture = texture;
        this.scale = scale;
    }

    public void renderWidget(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(), 0.0f, 0.0f, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate((float)this.getX(), (float)this.getY());
        graphics.getMatrices().scale(this.scale, this.scale);
        String mahikariuiT = "MahikariUI: Java Edition";
        String versionT = "MahikariUI v1.0.3";
        int mahikariuiX = (int)(((float)this.getWidth() / 2.0f - (float)mc.textRenderer.getWidth(mahikariuiT) / 2.0f) / this.scale);
        float f = (float)this.getHeight() / 2.0f;
        Objects.requireNonNull(mc.textRenderer);
        int mahikariuiY = (int)((f - 9.0f / 2.0f) / this.scale);
        int versionX = (int)(((float)this.getWidth() / 2.0f - (float)mc.textRenderer.getWidth(versionT) / 2.0f) / this.scale);
        float f2 = (float)this.getHeight() / 2.0f;
        Objects.requireNonNull(mc.textRenderer);
        int versionY = (int)((f2 - 9.0f / 2.0f) / this.scale);
        graphics.drawTextWithShadow(mc.textRenderer, mahikariuiT, mahikariuiX - 8, mahikariuiY + 2, 0xFFFFFFFF);
        graphics.drawTextWithShadow(mc.textRenderer, versionT, versionX - 29, versionY + 16, 0xFFFFFFFF);
        graphics.getMatrices().popMatrix();
    }

    protected void appendClickableNarrations(@NotNull NarrationMessageBuilder narrationElementOutput) {
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        return false;
    }

    public void playDownSound(@NotNull SoundManager soundManager) {
    }

    public void setExternalOpacity(float opacity) {
        this.externalOpacity = opacity;
    }
}

