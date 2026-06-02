package dev.mahikari.client.mixin;

import dev.mahikari.client.TeamViewConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Smooth chat: new messages slide up from below the chat baseline.
 *
 * <p>The old version translated the whole HUD by {@code (-15, +9)} every time
 * {@code visibleMessages.size()} grew, which had two problems: the {@code -15}
 * horizontal slide made every line in chat skid across the screen (the "nhìn
 * đểu" feel), and the size-grew heuristic mis-fires when messages get pruned
 * and re-added on the same frame.
 *
 * <p>New version:
 * <ul>
 *   <li>Detects new top-of-chat by <i>reference identity</i> on the latest
 *       {@code Visible} line — survives prune/scroll and never animates twice
 *       for the same message.</li>
 *   <li>Slides on the Y axis only, by exactly one chat line height (accounting
 *       for chat scale + line-spacing options).</li>
 *   <li>Uses a quartic ease-out so the bulk of the motion is at the start and
 *       it settles cleanly instead of overshooting feel.</li>
 *   <li>Enables a scissor at the chat baseline (screen y = scaledHeight - 40)
 *       so the new line emerging from below doesn't render over the chat
 *       input bar / hotbar area — that gives the natural wipe-in fade effect
 *       without per-line alpha tracking.</li>
 * </ul>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Shadow private int scrolledLines;
    @Shadow @Final private List<ChatHudLine.Visible> visibleMessages;

    @Unique private ChatHudLine.Visible mahikari$lastTop;
    @Unique private long mahikari$arrivalMs = 0L;
    @Unique private boolean mahikari$pushed = false;
    @Unique private boolean mahikari$scissored = false;

    @ModifyConstant(method = "*", constant = @Constant(intValue = 100))
    private int mahikari$modifyMaxChatMessages(int original) {
        return TeamViewConfig.get().infiniteChat ? 999999 : original;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("HEAD"))
    private void mahikari$preRender(DrawContext ctx, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci) {
        this.mahikari$pushed = false;
        this.mahikari$scissored = false;

        if (this.visibleMessages == null || this.visibleMessages.isEmpty()) return;

        // Track new top by reference identity. Vanilla addFirst()s each new line,
        // so a different reference at index 0 means a fresh arrival.
        ChatHudLine.Visible top = this.visibleMessages.get(0);
        if (top != this.mahikari$lastTop) {
            this.mahikari$lastTop = top;
            this.mahikari$arrivalMs = Util.getMeasuringTimeMs();
        }

        if (!TeamViewConfig.get().smoothChat) return;
        if (this.scrolledLines != 0) return; // user scrolled back; don't fight them

        float speed = Math.max(0.1f, TeamViewConfig.get().smoothChatSpeed);
        long duration = (long) (180.0f / speed);
        long elapsed = Util.getMeasuringTimeMs() - this.mahikari$arrivalMs;
        if (elapsed >= duration) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        float t = (float) elapsed / (float) duration;
        float eased = 1.0f - (float) Math.pow(1.0f - t, 4); // quartic ease-out

        double chatScale = mc.options.getChatScale().getValue();
        double lineSpacing = mc.options.getChatLineSpacing().getValue();
        // Match vanilla's per-line spacing: n = (int)(9 * (lineSpacing + 1)) then * chatScale.
        float lineH = (float) (9.0 * chatScale * (1.0 + lineSpacing));
        float yOffset = lineH * (1.0f - eased);

        // Scissor at chat baseline so the line emerging from below stays hidden
        // until it crosses the baseline. Chat is anchored at scaledHeight - 40
        // (ChatHud.OFFSET_FROM_BOTTOM), independent of focus.
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        ctx.enableScissor(0, 0, sw, sh - 40);
        this.mahikari$scissored = true;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(0.0f, yOffset);
        this.mahikari$pushed = true;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V", at = @At("RETURN"))
    private void mahikari$postRender(DrawContext ctx, TextRenderer textRenderer, int currentTick, int mouseX, int mouseY, boolean focused, boolean bl, CallbackInfo ci) {
        if (this.mahikari$pushed) {
            ctx.getMatrices().popMatrix();
            this.mahikari$pushed = false;
        }
        if (this.mahikari$scissored) {
            ctx.disableScissor();
            this.mahikari$scissored = false;
        }
    }
}
