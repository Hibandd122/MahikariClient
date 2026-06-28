/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.gui.screen.Screen
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.screens.credit;



import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.gui.button.ImageButton;
import mahikariui.core.gui.widget.AnimatedWidget;

public class CreditScreen
extends Screen {
    private final Screen parent;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final int PADDING = 10;
    Identifier BANNER = Identifier.of((String)"mahikariui", (String)"textures/gui/version_info.png");
    Identifier BACK_ICON_TEXTURE = Identifier.of((String)"mahikariui", (String)"textures/gui/back_icon.png");

    public CreditScreen(Screen parent) {
        super(net.minecraft.text.Text.literal((String)"MahikariUI Credits"));
        this.parent = parent;
    }

    protected void init() {
        int backSize = Math.min(16, this.height / 20);
        this.addDrawableChild(new AnimatedWidget((ClickableWidget)new ImageButton(10, 10, backSize, backSize, this.BACK_ICON_TEXTURE, button -> client.setScreen(this.parent), net.minecraft.text.Text.translatable((String)"mahikariui.config.back")), AnimatedWidget.Direction.LEFT));
    }

    public void render(@NotNull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        int bannerW = 1500;
        int bannerH = 480;
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        // graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, this.BANNER, this.width / 2, this.height / 2, 0.0f, 0.0f, bannerW, bannerH, bannerW, bannerH);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    public boolean shouldPause() {
        return false;
    }
}

