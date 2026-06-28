package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void preRenderItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        TeamViewConfig cfg = TeamViewConfig.get();
        boolean isTotem = item.isOf(Items.TOTEM_OF_UNDYING);
        boolean isShield = item.isOf(Items.SHIELD);
        
        if (cfg.smallItems && !isTotem && !isShield) {
            float handMultiplier = (hand == Hand.MAIN_HAND) ? 1.0f : -1.0f;
            matrices.translate(cfg.itemOffsetX * handMultiplier, cfg.itemOffsetY, 0.0f);
            matrices.scale(cfg.itemScale, cfg.itemScale, cfg.itemScale);
        }
        
        if (isShield && cfg.lowShield) {
            matrices.translate(0.0f, cfg.shieldHeight, 0.0f);
        }
        
        if (isTotem && cfg.lowTotem) {
            matrices.translate(0.0f, cfg.totemHeight, 0.0f);
            matrices.scale(cfg.totemScale, cfg.totemScale, cfg.totemScale);
        }
    }
}
