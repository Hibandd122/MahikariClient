package dev.mahikari.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.consume.UseAction;

/**
 * Auto-sprint via "fake keybind" approach: instead of forcing
 * {@code player.setSprinting(true)} every tick (which fights vanilla's
 * own logic and can flicker), we report the sprint keybind as held
 * through {@code KeyBindingSprintMixin}. Vanilla then runs its real
 * sprint state machine on top of that — including the parts we'd
 * otherwise have to reimplement (double-tap detection, packet sync,
 * speed attribute, stop-on-hit, etc.).
 *
 * Edge cases handled by gating the fake key-hold:
 *  - Only while moving forward (sprint keybind without W does nothing
 *    anyway, but skipping the call saves cycles).
 *  - Not while eating / drinking / blocking — those use
 *    {@link UseAction#EAT}, {@link UseAction#DRINK}, {@link UseAction#BLOCK}
 *    and actually slow the player. Charging a spear / trident / bow /
 *    crossbow does NOT slow the player, so we leave sprint on.
 *  - Not while wading (touching water but not swimming) to avoid the
 *    surface "bobbing" glitch. Active swimming keeps sprint on so a
 *    jump above the surface doesn't drop you back to walking speed.
 *  - Skipped under blindness, freezing, sleeping, spectator, crawling,
 *    gliding, mounted, dead, or near-empty hunger — same gates vanilla
 *    uses in {@code LivingEntity.canStartSprinting}.
 */
public final class AutoSprint {
    private AutoSprint() {}

    /** Called from {@code KeyBindingSprintMixin#isPressed} to decide if we
     *  should pretend the sprint keybind is held this frame. */
    public static boolean shouldHoldSprintKey(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return false;
        if (!client.options.forwardKey.isPressed()) return false;
        return canSprint(player);
    }

    private static boolean canSprint(ClientPlayerEntity player) {
        if (!player.isAlive()) return false;
        if (player.isSpectator()) return false;
        if (player.isSleeping()) return false;
        if (player.isSneaking()) return false;
        if (player.isCrawling()) return false;
        if (player.isGliding()) return false;
        if (player.hasVehicle()) return false;
        if (player.horizontalCollision) return false;
        if (player.hasStatusEffect(StatusEffects.BLINDNESS)) return false;
        if (player.isFrozen()) return false;
        if (player.getHungerManager().getFoodLevel() <= 6) return false;

        if (player.isUsingItem()) {
            UseAction action = player.getActiveItem().getUseAction();
            if (action == UseAction.EAT || action == UseAction.DRINK || action == UseAction.BLOCK) {
                return false;
            }
        }

        if (player.isTouchingWater() && !player.isSwimming()) return false;

        return true;
    }
}
