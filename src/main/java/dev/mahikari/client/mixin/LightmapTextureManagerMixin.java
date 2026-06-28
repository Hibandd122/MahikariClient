package dev.mahikari.client.mixin;

import com.mojang.blaze3d.buffers.Std140Builder;
import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Smuggles a fullbright sentinel into the LightmapInfo UBO.
 *
 * Std140Builder.putFloat is called 7 times in update(), once per scalar field
 * of LightmapInfo, in declaration order:
 *   0 AmbientLightFactor, 1 SkyFactor, 2 BlockFactor,
 *   3 NightVisionFactor, 4 DarknessScale, 5 DarkenWorldFactor, 6 BrightnessFactor.
 *
 * Vanilla writes NightVisionFactor in [0,1]; we write 100 + fullBrightLevel
 * when the toggle is on. lightmap.fsh detects values > 50 as the marker and
 * blends toward white by (NightVisionFactor - 100). Darkness is untouched.
 */
@Mixin(LightmapTextureManager.class)
public class LightmapTextureManagerMixin {

    @ModifyArg(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",
            ordinal = 3
        )
    )
    private float newgenMahikari$injectFullbrightMarker(float original) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.visualsEnabled || !cfg.fullBright) {
            return original;
        }
        
        MinecraftClient client = MinecraftClient.getInstance();
        // Return original if client or player is null, or if player has blindness/darkness
        if (client == null || client.player == null || 
            client.player.hasStatusEffect(StatusEffects.BLINDNESS) || 
            client.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            return original;
        }
        
        // Clamp fullBrightLevel to valid range [0.0, 1.0]
        float level = Math.max(0.0f, Math.min(1.0f, cfg.fullBrightLevel));
        return 100.0f + level;
    }
}
