package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @ModifyArgs(
        method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private void onApplyFogArgs(Args args) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.visualsEnabled) {
            return;
        }
        
        float density = 1.0f;

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        // Return early if client or player is null (removed blindness/darkness check as requested)
        if (mc == null || mc.player == null) {
            return;
        }

        // Check if we're in fluid (water or lava)
        boolean inFluid = false;
        if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            net.minecraft.block.enums.CameraSubmersionType submersion = mc.gameRenderer.getCamera().getSubmersionType();
            if (submersion == net.minecraft.block.enums.CameraSubmersionType.WATER || submersion == net.minecraft.block.enums.CameraSubmersionType.LAVA) {
                inFluid = true;
            }
        }

        // Make nofog take precedence over clearFluids as requested
        if (cfg.noFog) {
            // Clamp noFogDensity to valid range [0.0, 1.0]
            density = Math.max(0.0f, Math.min(1.0f, cfg.noFogDensity));
        } else if (cfg.clearFluids && inFluid) {
            density = 0.0f; // Force 0 density in fluids for clear vision (only when nofog is disabled)
        }

        if (density >= 0.99f) return; // Vanilla fog, don't touch
        
        float maxDist = 1000000f;
        float vanilla3 = (float) args.get(3);
        float vanilla4 = (float) args.get(4);
        float vanilla6 = (float) args.get(6);
        float vanilla7 = (float) args.get(7);
        float vanilla8 = (float) args.get(8);
        
        args.set(3, lerp(maxDist, vanilla3, density));
        args.set(4, lerp(maxDist * 2, vanilla4, density));
        args.set(6, lerp(maxDist * 2, vanilla6, density));
        args.set(7, lerp(maxDist, vanilla7, density));
        args.set(8, lerp(maxDist * 2, vanilla8, density));
    }
    
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
