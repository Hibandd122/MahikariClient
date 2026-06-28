package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    // Tracks whether preRenderFireOverlay actually pushed a matrix this call.
    // Without this, a config toggle mid-frame (or thread-race between two render
    // passes) can leave the matrix stack imbalanced.
    private static final ThreadLocal<Boolean> firePushed = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true)
    private static void onRenderUnderwaterOverlay(MinecraftClient client, MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (TeamViewConfig.get().visualsEnabled && TeamViewConfig.get().clearFluids) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void preRenderFireOverlay(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
        if (TeamViewConfig.get().lowFire) {
            matrices.push();
            matrices.translate(0.0f, -TeamViewConfig.get().fireHeight, 0.0f);
            firePushed.set(Boolean.TRUE);
        } else {
            firePushed.set(Boolean.FALSE);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void postRenderFireOverlay(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
        if (firePushed.get()) {
            matrices.pop();
            firePushed.set(Boolean.FALSE);
        }
    }
}
