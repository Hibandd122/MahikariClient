package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hide armor on rendered players.
 *
 * <p>Previous version checked {@code state.getClass().getName().contains("PlayerEntityRenderState")}.
 * That worked in dev (deobf yarn classes) but silently failed on production servers
 * where Fabric remaps classes to intermediary names (e.g. {@code class_10031}) — the
 * string never matched, the inject never cancelled, armor stayed visible. The
 * {@code instanceof} check below is rewritten by the remapper alongside the import,
 * so it works in both environments.
 *
 * <p>Mode resolution uses {@link PlayerEntityRenderState#id} (entity network id)
 * against {@code client.player.getId()} to tell self from others — the only
 * stable identifier on the render state in 1.21+.
 */
@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void mahikari$hideArmor(MatrixStack matrixStack, OrderedRenderCommandQueue queue, int light, BipedEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.hideArmor) return;
        if (!(state instanceof PlayerEntityRenderState player)) return;

        String mode = cfg.hideArmorMode;
        if ("ALL".equals(mode)) {
            ci.cancel();
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            // Can't tell self from others — fall back to hiding all so the
            // setting at least does something rather than silently no-op.
            ci.cancel();
            return;
        }

        boolean isSelf = player.id == mc.player.getId();
        if ("SELF_ONLY".equals(mode) && isSelf) ci.cancel();
        else if ("OTHERS_ONLY".equals(mode) && !isSelf) ci.cancel();
    }
}
