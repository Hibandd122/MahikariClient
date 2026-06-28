/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package mahikariui.mixin.common.client;



import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import mahikariui.core.background.BackgroundBuilder;
import mahikariui.core.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={TitleScreen.class})
public abstract class TitleScreenMixin {
    @Inject(method={"render(Lnet/minecraft/client/gui/DrawContext;IIF)V"}, at={@At(value="HEAD")})
    private void onRenderTitleScreen(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        Identifier currentFrame = BackgroundBuilder.getCurrentFrame();
        if (currentFrame != null) {
            graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, currentFrame, 0, 0, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight);
        }
    }
}

