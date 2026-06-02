package dev.mahikari.client.mixin;

import dev.mahikari.client.AutoSprint;
import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes vanilla think the sprint keybind is held whenever AutoSprint's
 * conditions are met. Vanilla then runs its own sprint logic, so we
 * don't have to call setSprinting() ourselves and we inherit every
 * edge-case fix vanilla has.
 *
 * Note: this fakes the *sprint keybind*, not the physical Ctrl key.
 * Code that reads Ctrl directly (Screen.hasControlDown — drop-stack,
 * Ctrl+click inventory shortcuts) still requires a real Ctrl press.
 */
@Mixin(KeyBinding.class)
public abstract class KeyBindingSprintMixin {
    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void mahikari$forceSprintWhenAuto(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return; // already pressed for real — let it through

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        KeyBinding self = (KeyBinding)(Object)this;
        if (self != mc.options.sprintKey) return;

        if (!TeamViewConfig.get().autoSprint) return;
        if (!AutoSprint.shouldHoldSprintKey(mc)) return;

        cir.setReturnValue(true);
    }
}
