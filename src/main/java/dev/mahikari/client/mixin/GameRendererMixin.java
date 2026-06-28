package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.visualsEnabled || !cfg.clearFluids) {
            return;
        }

        CameraSubmersionType submersionType = camera.getSubmersionType();
        if (submersionType == CameraSubmersionType.WATER || submersionType == CameraSubmersionType.LAVA) {
            MinecraftClient mc = MinecraftClient.getInstance();
            double effectScale = mc.options.getFovEffectScale().getValue();
            float fluidFactor = MathHelper.lerp((float) effectScale, 1.0f, 0.85714287f);
            if (fluidFactor > 0.001f) {
                cir.setReturnValue(cir.getReturnValue() / fluidFactor);
            }
        }
    }
}
