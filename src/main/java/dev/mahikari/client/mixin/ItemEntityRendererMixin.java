package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/Box;)V")
    )
    private void applyCustomPhysics(ItemEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, CameraRenderState cameraRenderState, CallbackInfo ci) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.itemPhysicsEnabled) return;

        // Reset the current matrix entry in-place to discard vanilla bobbing/rotation
        // This avoids pop/push which can corrupt the stack if other mixins alter depth
        Matrix4f pos = matrices.peek().getPositionMatrix();
        Matrix3f norm = matrices.peek().getNormalMatrix();
        pos.identity();
        norm.identity();

        if ("2D".equals(cfg.itemPhysicsMode)) {
            matrices.translate(0, 0.2f, 0);
            matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().camera.getRotation());
            matrices.scale(1.0f, 1.0f, 0.001f);
        } else {
            matrices.translate(0, 0.05f, 0);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
            float rotation = state.seed * 360f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }
    }
}
