package dev.mahikari.client.render;

import dev.mahikari.client.TeamViewConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EffectHudRenderer {
    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickDeltaManager) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;
            if (mc.currentScreen instanceof dev.mahikari.client.screen.HudEditorScreen) return;
            TeamViewConfig cfg = TeamViewConfig.get();
            if (!cfg.effectHudEnabled) return;
            if (mc.options.hudHidden) return;

            render(ctx, mc, cfg);
        });
    }

    public static void render(DrawContext ctx, MinecraftClient mc, TeamViewConfig cfg) {
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;

        List<StatusEffectInstance> sorted = new ArrayList<>(effects);
        if ("ALPHABETICAL".equalsIgnoreCase(cfg.effectHudSortMode)) {
            sorted.sort((e1, e2) -> {
                String n1 = net.minecraft.client.resource.language.I18n.translate(e1.getEffectType().value().getTranslationKey());
                String n2 = net.minecraft.client.resource.language.I18n.translate(e2.getEffectType().value().getTranslationKey());
                return n1.compareToIgnoreCase(n2);
            });
        } else {
            // Default: DURATION
            sorted.sort((e1, e2) -> Integer.compare(e1.getDuration(), e2.getDuration()));
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cfg.effectHudOffsetX, cfg.effectHudOffsetY);
        ctx.getMatrices().scale(cfg.effectHudScale, cfg.effectHudScale);

        int xOffset = 0;
        int yOffset = 0;
        boolean isHorizontal = "HORIZONTAL".equalsIgnoreCase(cfg.effectHudLayout);

        for (StatusEffectInstance effect : sorted) {
            RegistryEntry<StatusEffect> effectEntry = effect.getEffectType();
            StatusEffect type = effectEntry.value();
            
            int color = type.getColor() | 0xFF000000;
            String name = net.minecraft.client.resource.language.I18n.translate(type.getTranslationKey());
            if (cfg.effectHudShowAmplifier && effect.getAmplifier() > 0) {
                name += " " + toRoman(effect.getAmplifier() + 1);
            }

            int duration = effect.getDuration();
            String timeString = net.minecraft.util.StringHelper.formatTicks(duration, mc.world.getTickManager().getTickRate());
            if (effect.isInfinite()) {
                timeString = "**:**";
            }
            
            boolean isExpiring = !effect.isInfinite() && duration <= 200; // <= 10 seconds
            float alpha = 1.0f;
            if (isExpiring) {
                alpha = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 150.0);
            }
            
            int timeColor = 0xFFAAAAAA;
            if (!effect.isInfinite()) {
                if (duration <= 200) {
                    timeColor = 0xFFFF5555; // Always red when expiring, alpha takes care of blink
                } else if (duration <= 600) {
                    timeColor = 0xFFFFFF55;
                }
            }
            
            int alphaInt = (int) (255 * alpha) << 24;
            int renderNameColor = (color & 0x00FFFFFF) | alphaInt;
            int renderTimeColor = (timeColor & 0x00FFFFFF) | alphaInt;

            Identifier iconId = InGameHud.getEffectTexture(effectEntry);

            if (isHorizontal) {
                int boxWidth = mc.textRenderer.getWidth(name);
                int timeWidth = mc.textRenderer.getWidth(timeString);
                boxWidth = Math.max(boxWidth, timeWidth) + 36;

                if (cfg.effectHudShowBackground) {
                    // Modern gradient background
                    int bg1 = 0x991A1D2E;
                    int bg2 = 0x770F1117;
                    fillRoundedGradient(ctx, xOffset, 0, xOffset + boxWidth, 28, bg1, bg2, 6);

                    // Top highlight
                    ctx.fillGradient(xOffset + 2, 2, xOffset + boxWidth - 2, 5, 0x22FFFFFF, 0x00FFFFFF);
                }
                if (cfg.effectHudShowBorder) {
                    // Vibrant colored accent bar with glow
                    int glowColor = (color & 0x00FFFFFF) | 0x44000000;
                    fillRounded(ctx, xOffset - 1, 3, xOffset + 1, 25, glowColor, 1);
                    fillRounded(ctx, xOffset, 3, xOffset + 3, 25, color, 1);
                }

                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconId, xOffset + 8, 5, 18, 18);

                ctx.drawTextWithShadow(mc.textRenderer, name, xOffset + 30, 6, renderNameColor);
                ctx.drawTextWithShadow(mc.textRenderer, timeString, xOffset + 30, 16, renderTimeColor);

                xOffset += boxWidth + 6;
            } else {
                if (cfg.effectHudShowBackground) {
                    // Modern gradient background
                    int bg1 = 0x991A1D2E;
                    int bg2 = 0x770F1117;
                    fillRoundedGradient(ctx, 0, yOffset, 130, yOffset + 28, bg1, bg2, 6);

                    // Top highlight
                    ctx.fillGradient(2, yOffset + 2, 128, yOffset + 5, 0x22FFFFFF, 0x00FFFFFF);
                }
                if (cfg.effectHudShowBorder) {
                    // Vibrant colored accent bar with glow
                    int glowColor = (color & 0x00FFFFFF) | 0x44000000;
                    fillRounded(ctx, -1, yOffset + 3, 1, yOffset + 25, glowColor, 1);
                    fillRounded(ctx, 0, yOffset + 3, 3, yOffset + 25, color, 1);
                }

                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconId, 8, yOffset + 5, 18, 18);

                ctx.drawTextWithShadow(mc.textRenderer, name, 30, yOffset + 6, renderNameColor);
                ctx.drawTextWithShadow(mc.textRenderer, timeString, 30, yOffset + 16, renderTimeColor);

                yOffset += 32;
            }
        }

        ctx.getMatrices().popMatrix();
    }

    private static String toRoman(int level) {
        switch (level) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            case 6: return "VI";
            case 7: return "VII";
            case 8: return "VIII";
            case 9: return "IX";
            case 10: return "X";
            default: return String.valueOf(level);
        }
    }

    private static void fillRoundedGradient(DrawContext ctx, int x0, int y0, int x1, int y1, int colorStart, int colorEnd, int r) {
        if (r < 1 || x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            ctx.fillGradient(x0, y0, x1, y1, colorStart, colorEnd);
            return;
        }
        ctx.fillGradient(x0, y0 + r, x1, y1 - r, colorStart, colorEnd);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, colorStart);
            ctx.fill(x0 + inset, y1 - 1 - i, x1 - inset, y1 - i, colorEnd);
        }
    }

    private static void fillRounded(DrawContext ctx, int x0, int y0, int x1, int y1, int color, int r) {
        if (r < 1 || x1 - x0 < 2 * r || y1 - y0 < 2 * r) {
            ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        ctx.fill(x0, y0 + r, x1, y1 - r, color);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, color);
            ctx.fill(x0 + inset, y1 - 1 - i, x1 - inset, y1 - i, color);
        }
    }
}
