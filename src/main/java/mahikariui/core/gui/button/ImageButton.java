/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ButtonWidget
 *  net.minecraft.client.gui.widget.ButtonWidget.PressAction
 *  net.minecraft.util.math.ColorHelper
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.button;

import java.util.List;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.jetbrains.annotations.NotNull;

public class ImageButton
extends ButtonWidget {
    private final Identifier texture;
    private final int iconWidth;
    private final int iconHeight;
    private final List<net.minecraft.text.Text> tooltip;
    private float externalOpacity = 1.0f;

    public ImageButton(int x, int y, int width, int height, Identifier texture, ButtonWidget.PressAction onClick, List<net.minecraft.text.Text> tooltip) {
        super(x, y, width, height, net.minecraft.text.Text.literal(""), onClick, DEFAULT_NARRATION_SUPPLIER);
        this.texture = texture;
        this.iconWidth = width;
        this.iconHeight = height;
        this.tooltip = tooltip;
    }

    public ImageButton(int x, int y, int width, int height, Identifier texture, ButtonWidget.PressAction onClick, net.minecraft.text.Text tooltip) {
        this(x, y, width, height, texture, onClick, List.of(tooltip));
    }

    protected void drawIcon(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        if (this.visible) {
            graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.texture, this.getX(), this.getY(), 0.0f, 0.0f, this.width, this.height, this.iconWidth, this.iconHeight);
            if (this.isSelected() && this.tooltip != null && !this.tooltip.isEmpty()) {
                graphics.drawTooltip(MinecraftClient.getInstance().textRenderer, this.tooltip, mouseX, mouseY);
            }
        }
    }

    public void setExternalOpacity(float opacity) {
        this.externalOpacity = opacity;
    }
}

