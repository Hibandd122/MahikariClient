/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
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
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.Constants;

public class TextIconButton
extends ButtonWidget {
    private final Identifier icon;
    private final TextAlign alignment;
    private final float fontScale;
    private final net.minecraft.text.Text message;
    private final int iconSize;
    private float hoverAlpha = 0.0f;
    private float externalOpacity = 1.0f;

    public TextIconButton(int x, int y, int width, int height, net.minecraft.text.Text message, ButtonWidget.PressAction onPress, TextAlign align, Identifier icon, float fontScale, int iconSize) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.message = message;
        this.alignment = align;
        this.icon = icon;
        this.fontScale = fontScale;
        this.iconSize = iconSize;
    }

    public TextIconButton(int x, int y, int width, int height, net.minecraft.text.Text message, ButtonWidget.PressAction onPress, TextAlign align, Identifier icon) {
        this(x, y, width, height, message, onPress, align, icon, 1.0f, 12);
    }

    public void drawIcon(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        float targetAlpha = this.isSelected() ? 0.6f : 0.0f;
        this.hoverAlpha += (targetAlpha - this.hoverAlpha) * 0.1f;
        int alphaInt = (int)(this.hoverAlpha * 255.0f);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, alphaInt << 24);
        int iconOffset = 0;
        if (this.icon != null) {
            iconOffset = this.iconSize + 8;
            int iconY = this.getY() + (this.getHeight() - this.iconSize) / 2;
            graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.icon, this.getX() + 4, iconY, 0.0f, 0.0f, this.iconSize, this.iconSize, this.iconSize, this.iconSize);
        }
        String rawText = this.getMessage().getString();
        int maxTextWidth = (int)((float)(this.getWidth() - 8 - iconOffset) / this.fontScale);
        Object trimmedText = font.trimToWidth(rawText, maxTextWidth);
        int textWidth = (int)((float)font.getWidth((String)trimmedText) * this.fontScale);
        int textHeight = (int)(8.0f * this.fontScale);
        int textX = switch (this.alignment.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> this.getX() + 4 + iconOffset;
            case 2 -> this.getX() + this.getWidth() - textWidth - 4;
            case 1 -> this.getX() + (this.getWidth() - textWidth) / 2;
        };
        int textY = this.getY() + (this.getHeight() - textHeight) / 2;
        if (!((String)trimmedText).equals(rawText) && ((String)trimmedText).length() > 3) {
            trimmedText = ((String)trimmedText).substring(0, ((String)trimmedText).length() - 3) + "...";
        }
        if (this.message.getString().equalsIgnoreCase("MODS")) {
            int badgeX = this.getX() + 55;
            int badgeY = this.getY() + 4;
            graphics.drawTextWithShadow(font, String.valueOf(Constants.getModCount()), badgeX + 8, badgeY + 2, 0xFFFFFFFF);
        }
        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate((float)textX, (float)textY);
        graphics.getMatrices().scale(this.fontScale, this.fontScale);
        graphics.drawTextWithShadow(font, (String)trimmedText, 0, 0, 0xFFFFFFFF);
        graphics.getMatrices().popMatrix();
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

