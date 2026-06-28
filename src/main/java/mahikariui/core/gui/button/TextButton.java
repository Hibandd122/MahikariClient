/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.text.Text
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ButtonWidget
 *  net.minecraft.client.gui.widget.ButtonWidget.PressAction
 *  net.minecraft.util.math.ColorHelper
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.button;

import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.jetbrains.annotations.NotNull;

public class TextButton
extends ButtonWidget {
    private final TextAlign alignment;
    private float externalOpacity = 1.0f;
    private net.minecraft.text.Text dynamicMessage = null;

    public TextButton(int x, int y, int width, int height, net.minecraft.text.Text message, ButtonWidget.PressAction onPress, TextAlign align) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.alignment = align;
    }
    
    public void setDynamicMessage(net.minecraft.text.Text message) {
        this.dynamicMessage = message;
    }
    
    @Override
    public net.minecraft.text.Text getMessage() {
        return this.dynamicMessage != null ? this.dynamicMessage : super.getMessage();
    }

    public void drawIcon(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        if (this.isSelected()) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, Integer.MIN_VALUE);
        }
        int maxTextWidth = this.getWidth() - 8;
        String rawText = this.getMessage().getString();
        Object trimmedText = font.trimToWidth(rawText, maxTextWidth);
        int textWidth = font.getWidth((String)trimmedText);
        int textX = switch (this.alignment.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.getX() + 4;
            case 2 -> this.getX() + this.getWidth() - textWidth - 4;
            case 1 -> this.getX() + (this.getWidth() - textWidth) / 2;
        };
        int textY = this.getY() + (this.getHeight() - 8) / 2;
        if (!((String)trimmedText).equals(rawText) && ((String)trimmedText).length() > 3) {
            trimmedText = ((String)trimmedText).substring(0, ((String)trimmedText).length() - 3) + "...";
        }
        graphics.drawTextWithShadow(font, (String)trimmedText, textX, textY, 0xFFFFFFFF);
    }

    public void setExternalOpacity(float opacity) {
        this.externalOpacity = opacity;
    }

    public static enum TextAlign {
        LEFT,
        CENTER,
        RIGHT;

    }
}

