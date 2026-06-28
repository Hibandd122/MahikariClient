package mahikariui.mixin.common.client;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Util;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceReload;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow @Final private ResourceReload reload; // reload
    @Shadow @Final private boolean reloading; // reloading

    private float smoothProgress = 0.0f;
    private float fadeAlpha = 1.0f;

    @org.spongepowered.asm.mixin.Unique
    private static final Identifier MAHIKARI_ICON = Identifier.of("mahikariui", "textures/gui/mahikari_icon.png");

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void renderMahikariSplash(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // ONLY use the custom Mahikari splash screen on initial game startup.
        // If it's a resource reload (F3+T or Server Resource Pack), let Vanilla handle it safely!
        if (this.reloading) {
            return;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // Update smooth progress manually
        float targetProgress = this.reload.getProgress();
        this.smoothProgress = net.minecraft.util.math.MathHelper.clamp(this.smoothProgress * 0.95f + targetProgress * 0.05f, 0.0f, 1.0f);

        // Frame-based fade out after loading is complete
        if (this.reload.isComplete() && this.smoothProgress >= 0.99f) {
            this.fadeAlpha -= 0.03f; // ~33 frames to fade out perfectly
        }

        if (this.fadeAlpha <= 0.0f) {
            client.setOverlay(null);
            ci.cancel();
            return;
        }

        // 1. Draw Opaque Black Background (Base)
        graphics.fill(0, 0, width, height, 0xFF000000);

        // 2. Draw Premium Breathing Logo
        long time = System.currentTimeMillis();
        float breath = (float)Math.sin(time / 300.0) * 0.1f;
        float scale = 2.0f + (this.smoothProgress * 0.8f) + breath;

        graphics.getMatrices().pushMatrix();
        graphics.getMatrices().translate((float)width / 2.0f, (float)height / 2.0f);
        graphics.getMatrices().scale(scale, scale);
        graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, MAHIKARI_ICON, -16, -24, 0.0f, 0.0f, 32, 32, 32, 32);
        graphics.getMatrices().popMatrix();

        // 3. Cinematic Neon Progress Bar
        int barWidth = (int)(width * 0.6f);
        int barHeight = 2;
        int barX = (width - barWidth) / 2;
        int barY = height - (int)(height * 0.15f);

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x44FFFFFF);

        int progressWidth = (int)(barWidth * this.smoothProgress);
        if (progressWidth > 0) {
            graphics.fill(barX, barY - 1, barX + progressWidth, barY + barHeight + 1, 0x6600FFFF); // Glow
            graphics.fill(barX, barY, barX + progressWidth, barY + barHeight, 0xFFFFFFFF); // Core

            if (progressWidth > 20) {
                float scanFloat = (time % 1200L) / 1200.0f;
                int scanPos = (int)(scanFloat * progressWidth);
                int scanWidth = 20;

                int scanStartX = Math.max(barX, barX + scanPos - scanWidth / 2);
                int scanEndX = Math.min(barX + progressWidth, barX + scanPos + scanWidth / 2);

                if (scanStartX < scanEndX) {
                    graphics.fill(scanStartX, barY, scanEndX, barY + barHeight, 0xFF00FFFF); // Highlight
                }
            }
        }

        // 4. Draw Fade-to-Black Overlay
        if (this.fadeAlpha < 1.0f) {
            float blackoutAlpha = 1.0f - this.fadeAlpha;
            int blackoutColor = ((int)(blackoutAlpha * 255.0f) << 24) | 0x000000;
            graphics.fill(0, 0, width, height, blackoutColor);
        }

        ci.cancel();
    }
}
