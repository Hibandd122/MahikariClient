/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.screen.Screen
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package mahikariui.mixin.common.client;



import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import mahikariui.core.background.BackgroundBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Screen.class})
public abstract class ScreenMixin {
    @Inject(method="renderBackground", at=@At("HEAD"), cancellable=true)
    private void renderGlobalBackground(DrawContext graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        
        // Disable Animated Background for non-pausing screens when in-game (Inventory, Chat, Sign, etc.)
        // This keeps the animated background on the ESC Pause Menu, but returns Inventory to NORMAL so you can see outside!
        if (mc.gameRenderer != null && !((net.minecraft.client.gui.screen.Screen)(Object)this).shouldPause()) {
            return; // Do NOT cancel, let Vanilla render its transparent in-game blur!
        }
        
        // Skip animated background for Book screens (reading & editing) — they have their own background texture
        if ((Object)this instanceof net.minecraft.client.gui.screen.ingame.BookScreen || (Object)this instanceof net.minecraft.client.gui.screen.ingame.BookEditScreen) {
            return; // Let Vanilla render the book background normally
        }
        
        // Draw Animated Background EVERYWHERE ELSE (Main Menu, Options, Multiplayer, ESC Menu, etc.)
        Identifier currentFrame = BackgroundBuilder.getCurrentFrame();
        if (currentFrame != null) {
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            
            float screenAspect = (float)width / (float)height;
            float imageAspect = 16.0f / 9.0f; // Standard 16:9 Wallpaper
            int drawWidth = width;
            int drawHeight = height;
            int offsetX = 0;
            int offsetY = 0;
            
            if (screenAspect > imageAspect) {
                drawHeight = (int)(width / imageAspect);
                offsetY = (height - drawHeight) / 2;
            } else {
                drawWidth = (int)(height * imageAspect);
                offsetX = (width - drawWidth) / 2;
            }
            
            // FORCED WHITE TINT (0xFFFFFFFF) to prevent Minecraft from darkening the texture using leftover shader colors
            graphics.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, currentFrame, offsetX, offsetY, 0.0f, 0.0f, drawWidth, drawHeight, drawWidth, drawHeight, 0xFFFFFFFF);
        }
        
        ci.cancel();
    }

    // KILL THE VANILLA DARKENING OVERLAY GLOBALLY
    @Inject(method="renderDarkening", at=@At("HEAD"), cancellable=true)
    private void disableVanillaDarkening(DrawContext graphics, CallbackInfo ci) {
        ci.cancel();
    }
}

