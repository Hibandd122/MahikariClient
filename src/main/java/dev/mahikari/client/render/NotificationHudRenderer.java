package dev.mahikari.client.render;

import dev.mahikari.client.TeamViewConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import dev.mahikari.client.notification.NotificationManager;
import org.joml.Matrix3x2fStack;

import java.util.List;

public class NotificationHudRenderer {
    private static final int CARD_PAD_X = 12;
    private static final int CARD_PAD_Y = 8;
    private static final int CARD_GAP = 8;
    private static final int ACCENT_STRIP_W = 4;
    private static final int ICON_BOX_SIZE = 20;
    private static final float SUBTITLE_SCALE = 0.85f;

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            NotificationManager.tick();
            renderNotifications(ctx);
        });
    }

    private static net.minecraft.text.Text styled(String s) {
        return net.minecraft.text.Text.literal(s);
    }

    private static void renderNotifications(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof dev.mahikari.client.screen.HudEditorScreen) return;
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.notificationsEnabled) return;

        List<NotificationManager.Notification> notifications = NotificationManager.getActiveNotifications();
        if (notifications.isEmpty()) return;

        float scale = Math.max(0.5f, Math.min(2.5f, cfg.notificationScale));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        if (scale != 1.0f) {
            m.scale(scale, scale);
        }

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int maxBoxW = 0;
        for (NotificationManager.Notification notif : notifications) {
            int[] size = measureCard(mc, notif.title, notif.subtitle);
            if (size[0] > maxBoxW) maxBoxW = size[0];
        }

        float actualW = maxBoxW * scale;
        int clampedX;
        if (cfg.notificationOffsetX == 9999 || cfg.notificationOffsetX < 0) {
            clampedX = (int) (sw - actualW - 10);
        } else {
            clampedX = (int) Math.max(0, Math.min(sw - actualW, cfg.notificationOffsetX));
        }
        int anchorY = cfg.notificationOffsetY;

        boolean fromRight = (clampedX + actualW / 2) > sw / 2;
        float rightEdgeUnscaled = (clampedX + actualW) / scale;
        float clampedXUnscaled = clampedX / scale;

        float yCursor = anchorY / scale;
        for (NotificationManager.Notification notif : notifications) {
            float alpha = notif.getAlpha();
            if (alpha <= 0.01f) continue;

            int[] size = measureCard(mc, notif.title, notif.subtitle);
            int boxW = size[0];
            int boxH = size[1];

            float finalX = fromRight ? (rightEdgeUnscaled - boxW) : clampedXUnscaled;
            float slideDist = boxW + 40;
            float sp = notif.getSlideProgress();
            
            // Premium Bounce Animation (Enhanced Back Ease Out)
            float f = sp - 1.0f;
            float easeOut = f * f * (3.0f * f + 2.0f) + 1.0f;
            float slide = sp < 1.0f ? ((1.0f - easeOut) * slideDist) : 0;
            
            float sx = fromRight ? finalX + slide : finalX - slide;

            int alphaInt = (int) (alpha * 255.0f);
            drawCard(ctx, mc, notif.title, notif.subtitle, notif.color, notif.getProgress(), (int)sx, (int)yCursor, boxW, boxH, alphaInt);
            yCursor += boxH + CARD_GAP;
        }

        m.popMatrix();
    }

    public static int[] measureCard(MinecraftClient mc, String title, String subtitle) {
        String icon = iconForTitle(title);
        int iconW = icon.isEmpty() ? 0 : ICON_BOX_SIZE + 6;
        int titleW = mc.textRenderer.getWidth(styled(title));
        int subW = (int)(mc.textRenderer.getWidth(styled(subtitle)) * SUBTITLE_SCALE);
        int textW = Math.max(titleW, subW);
        int boxW = CARD_PAD_X + iconW + textW + CARD_PAD_X;
        int boxH = 9 + 3 + (int)(9 * SUBTITLE_SCALE) + CARD_PAD_Y * 2;
        return new int[] { boxW, boxH };
    }

    public static void drawCard(DrawContext ctx, MinecraftClient mc, String title, String subtitle,
                                int color, float progress, int x, int y, int w, int h, int alphaInt) {
        TeamViewConfig cfg = TeamViewConfig.get();
        int titleColor = (alphaInt << 24) | 0xFFFFFF;
        int subtitleColor = (alphaInt << 24) | 0xE8ECF4;
        int bgAlpha = alphaInt * 220 / 255;

        // Modern Dark Glassmorphism with enhanced depth
        int cardBg1 = (bgAlpha << 24) | 0x1A1D2E;
        int cardBg2 = (bgAlpha << 24) | 0x0F1117;
        int borderColor = (alphaInt << 24) | 0x2D3250;

        int accentColor = (alphaInt << 24) | color;
        int accentGlow = ((alphaInt * 120 / 255) << 24) | color;
        int shadow1 = ((alphaInt * 120 / 255) << 24) | 0x000000;
        int shadow2 = ((alphaInt * 60 / 255) << 24) | 0x000000;
        int shadow3 = ((alphaInt * 30 / 255) << 24) | 0x000000;

        int R = 6; // Enhanced rounding

        if (cfg.notificationShowBackground) {
            // Multi-layer diffuse drop shadow for depth
            fillRounded(ctx, x - 3, y + 3, x + w + 3, y + h + 4, shadow3, R + 2);
            fillRounded(ctx, x - 2, y + 2, x + w + 2, y + h + 3, shadow2, R + 1);
            fillRounded(ctx, x - 1, y + 1, x + w + 1, y + h + 2, shadow1, R);

            // Modern gradient fill
            fillRoundedGradient(ctx, x, y, x + w, y + h, cardBg1, cardBg2, R);

            // Top highlight for glass effect
            ctx.fillGradient(x + 2, y + 2, x + w - 2, y + 6, ((alphaInt * 40 / 255) << 24) | 0xFFFFFF, 0x00FFFFFF);

            // Subtle border
            int borderAlpha = alphaInt * 80 / 255;
            int border = (borderAlpha << 24) | (borderColor & 0xFFFFFF);
            fillRounded(ctx, x, y, x + w, y + h, border, R);
            fillRounded(ctx, x + 1, y + 1, x + w - 1, y + h - 1, cardBg1, R - 1);
        }

        // Enhanced Floating Accent Pill with glow
        int pillPad = 6;
        int pillW = 4;

        // Outer glow layers
        int gAlpha1 = alphaInt * 80 / 255;
        int gAlpha2 = alphaInt * 50 / 255;
        fillRounded(ctx, x + 2, y + pillPad - 3, x + 12, y + h - pillPad + 3, (gAlpha1 << 24) | (color & 0xFFFFFF), 4);
        fillRounded(ctx, x + 3, y + pillPad - 2, x + 11, y + h - pillPad + 2, (gAlpha2 << 24) | (color & 0xFFFFFF), 3);

        // Main accent pill with gradient
        int accentBot = ((alphaInt * 180 / 255) << 24) | darken(color, 0.7f);
        fillRoundedGradient(ctx, x + 6, y + pillPad, x + 6 + pillW, y + h - pillPad, accentColor, accentBot, 2);

        // Pill highlight
        ctx.fillGradient(x + 7, y + pillPad + 1, x + 6 + pillW - 1, y + pillPad + 3, ((alphaInt * 50 / 255) << 24) | 0xFFFFFF, 0x00FFFFFF);

        int textX = x + CARD_PAD_X;
        String icon = iconForTitle(title);

        if (!icon.isEmpty()) {
            int boxSize = ICON_BOX_SIZE;
            int boxY = y + (h - boxSize) / 2;

            if (cfg.notificationShowIconBox) {
                // Modern icon box with glow
                int iconBg = ((alphaInt * 50 / 255) << 24) | (color & 0xFFFFFF);
                int iconBorder = ((alphaInt * 100 / 255) << 24) | (color & 0xFFFFFF);
                int iconGlow = ((alphaInt * 60 / 255) << 24) | (color & 0xFFFFFF);

                // Glow
                fillRounded(ctx, textX - 2, boxY - 2, textX + boxSize + 2, boxY + boxSize + 2, iconGlow, 5);

                // Box
                fillRounded(ctx, textX, boxY, textX + boxSize, boxY + boxSize, iconBorder, 4);
                fillRounded(ctx, textX + 1, boxY + 1, textX + boxSize - 1, boxY + boxSize - 1, iconBg, 3);

                // Highlight
                ctx.fillGradient(textX + 2, boxY + 2, textX + boxSize - 2, boxY + 5, ((alphaInt * 40 / 255) << 24) | 0xFFFFFF, 0x00FFFFFF);
            }

            ctx.drawText(mc.textRenderer, icon, textX + (boxSize - mc.textRenderer.getWidth(icon)) / 2 + 1, boxY + 6, titleColor, true);

            textX += boxSize + 6;
        }

        int textY = y + CARD_PAD_Y;

        // Title with subtle glow
        ctx.drawText(mc.textRenderer, styled(title), textX + 1, textY + 1, ((alphaInt * 30 / 255) << 24) | (color & 0xFFFFFF), false);
        ctx.drawText(mc.textRenderer, styled(title), textX, textY, titleColor, true);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(textX, textY + 11);
        m.scale(SUBTITLE_SCALE, SUBTITLE_SCALE);
        ctx.drawText(mc.textRenderer, styled(subtitle), 0, 0, subtitleColor, true);
        m.popMatrix();

        // Enhanced Sweeping Shimmer effect
        if (cfg.notificationShowShimmer && progress > 0.7f) {
            float shimmerProgress = 1.0f - ((progress - 0.7f) / 0.3f);
            int shineX = x + (int)(w * shimmerProgress * 1.8f) - (w / 3);
            int shineW = 40;

            int cEdge = 0x00FFFFFF;
            int cMid = (alphaInt / 3 << 24) | 0xFFFFFF;

            int sX1 = Math.max(x + 4, shineX);
            int sX2 = Math.min(x + w - 4, shineX + shineW);
            int sX3 = Math.min(x + w - 4, shineX + shineW * 2);

            if (sX2 > sX1) ctx.fillGradient(sX1, y + 3, sX2, y + h - 3, cEdge, cMid);
            if (sX3 > sX2) ctx.fillGradient(sX2, y + 3, sX3, y + h - 3, cMid, cEdge);
        }
    }

    private static int darken(int argb, float factor) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.max(0, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.max(0, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.max(0, Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int brighten(int argb, float factor) {
        int r = Math.min(255, (int)(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((argb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static String iconForTitle(String title) {
        if (title == null) return "";
        return switch (title) {
            case "CHẾ TẠO HUYỀN THOẠI", "CHẾ TẠO TRANG BỊ" -> "⚔";
            case "THÍNH RƠI", "THÍNH ĐÃ RƠI" -> "✦";
            case "SẴN SÀNG CHẾ TẠO", "ĐỦ NGUYÊN LIỆU" -> "✓";
            default -> "⚡";
        };
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

    private static void fillRoundedLeft(DrawContext ctx, int x0, int y0, int x1, int y1, int color, int r) {
        if (r < 1 || x1 - x0 < r || y1 - y0 < 2 * r) {
            ctx.fill(x0, y0, x1, y1, color);
            return;
        }
        ctx.fill(x0, y0 + r, x1, y1 - r, color);
        for (int i = 0; i < r; i++) {
            int inset = r - i;
            ctx.fill(x0 + inset, y0 + i, x1, y0 + i + 1, color);
            ctx.fill(x0 + inset, y1 - 1 - i, x1, y1 - i, color);
        }
    }
}
