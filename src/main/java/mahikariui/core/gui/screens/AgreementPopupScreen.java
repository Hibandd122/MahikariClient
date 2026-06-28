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
 *  net.minecraft.util.Util
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.gui.screen.ConfirmLinkScreen
 *  net.minecraft.client.gui.screen.Screen
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.screens;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;


import net.minecraft.util.Util;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.background.BackgroundBuilder;
import mahikariui.core.config.Config;
import mahikariui.core.gui.button.TextButton;
import mahikariui.core.gui.layout.FlexLayout;
import mahikariui.core.gui.layout.UIAnchor;
import mahikariui.core.gui.widget.AnimatedWidget;

public class AgreementPopupScreen
extends Screen {
    private final Screen parent;

    public AgreementPopupScreen(Screen parent) {
        super(net.minecraft.text.Text.literal((String)"User Agreement"));
        this.parent = parent;
    }

    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        FlexLayout agreementLayout = new FlexLayout(FlexLayout.Direction.COLUMN, UIAnchor.CENTER, 4, FlexLayout.Alignment.CENTER, FlexLayout.Alignment.CENTER);
        agreementLayout.add((ClickableWidget)new TextButton(0, 0, 180, 20, net.minecraft.text.Text.literal((String)"I Agree"), btn -> {
            Config.getInstance().setAgreed();
            client.setScreen(this.parent);
        }, TextButton.TextAlign.CENTER));
        for (ClickableWidget widget : agreementLayout.build(this.width, this.height)) {
            this.addDrawableChild(new AnimatedWidget(widget, AnimatedWidget.Direction.BOTTOM));
        }
    }

    public void renderBackground(@NotNull DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
        Identifier[] frames = BackgroundBuilder.getBackground().getFramePaths();
        if (frames.length == 0) {
            return;
        }
        AbstractTexture abstractTexture = MinecraftClient.getInstance().getTextureManager().getTexture(frames[0]);
        if (abstractTexture instanceof NativeImageBackedTexture) {
            NativeImageBackedTexture dynamicTexture = (NativeImageBackedTexture)abstractTexture;
            
            
        }
        graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, frames[0], 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        graphics.fillGradient(0, 0, this.width, this.height, -1442840576, -1442840576);
    }

    public void render(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int startY = this.height / 2 - 90;
        graphics.drawCenteredTextWithShadow(this.textRenderer, "Welcome to MahikariUI", centerX, startY, 0xFFFFFF);
        graphics.drawCenteredTextWithShadow(this.textRenderer, "Before using this mod, please agree to the following terms:", centerX, startY + 15, 0xCCCCCC);
        graphics.drawCenteredTextWithShadow(this.textRenderer, "https://github.strivo.xyz/mahikariui-download/blob/main/LICENSE", centerX, startY + 30, 0x55AAFF);
        graphics.drawCenteredTextWithShadow(this.textRenderer, "By clicking 'I Agree', you accept responsibility for its usage.", centerX, startY + 50, 0xAAAAAA);
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int centerX = this.width / 2;
        int urlY = this.height / 2 - 90 + 30;
        String url = "https://github.strivo.xyz/mahikariui-download/blob/main/LICENSE";
        int textWidth = this.textRenderer.getWidth(url);
        int x1 = centerX - textWidth / 2;
        int x2 = centerX + textWidth / 2;
        if (mouseY >= (double)urlY && mouseY <= (double)(urlY + 10) && mouseX >= (double)x1 && mouseX <= (double)x2) {
            MinecraftClient.getInstance().keyboard.setClipboard(url);
            MinecraftClient.getInstance().setScreen((Screen)new ConfirmLinkScreen(open -> {
                if (open) {
                    Util.getOperatingSystem().open(url);
                }
                MinecraftClient.getInstance().setScreen((Screen)this);
            }, url, false));
            return true;
        }
        return super.mouseClicked(click, bl);
    }
}

