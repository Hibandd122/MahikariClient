package dev.mahikari.client.render;

import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import dev.mahikari.client.util.WorldFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class TeamHudRenderer {
    private static final int MAX_CARDS_HARD = 8;
    private static final float MIN_TEAM_HUD_SCALE = 0.5f;
    private static final float MAX_TEAM_HUD_SCALE = 2.0f;
    private static final int HEAD_SIZE = 16;
    private static final int HEAD_GAP = 4;
    private static final int CARD_GAP = 6;
    private static final int CARD_PAD_Y = 3; // panel extends this many px above/below yBase in drawCard
    private static final int HEART_SIZE = 9;
    private static final int EFFECT_SIZE = 9;
    private static final int MAX_EFFECTS = 6;
    private static final int ACCENT_W = 3;
    private static final int BAR_W = 130;
    private static final int CARD_W = ACCENT_W + 4 + HEAD_SIZE + HEAD_GAP + BAR_W + 6;
    private static final int BAR_H = 5;
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");

    private static final HashMap<String, AbstractClientPlayerEntity> playerCache = new HashMap<>();
    private static long lastCacheTick;

    public static void register() {
        HudRenderCallback.EVENT.register(TeamHudRenderer::onHudRender);
    }

    private static void onHudRender(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        if (mc.currentScreen instanceof dev.mahikari.client.screen.HudEditorScreen) {
            return;
        }
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.enabled || !cfg.teamHudEnabled) {
            return;
        }
        float scale = clampFloat(cfg.teamHudScale, MIN_TEAM_HUD_SCALE, MAX_TEAM_HUD_SCALE);
        
        if (cfg.autoSprint && cfg.sprintingShowHud) {
            boolean isSprinting = mc.player.isSprinting();
            String prefix = cfg.sprintingShowBorder ? "[SPRINTING: " : "SPRINTING: ";
            String suffix = cfg.sprintingShowBorder ? "]" : "";
            String stateStr = isSprinting ? "ON" : "OFF";
            String sprintText = prefix + stateStr + suffix;
            
            int textW = mc.textRenderer.getWidth(sprintText);
            int screenW = mc.getWindow().getScaledWidth();
            int screenH = mc.getWindow().getScaledHeight();
            
            int x = cfg.sprintingOffsetX;
            int y = cfg.sprintingOffsetY;
            
            if (x < 0) x = screenW - textW - 2;
            if (y < 0) y = screenH - 12;

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x, y);
            
            if (cfg.sprintingAnimated && isSprinting) {
                float pulse = 1.0f + 0.05f * (float) Math.sin(System.currentTimeMillis() / 150.0);
                ctx.getMatrices().translate(textW / 2.0f, mc.textRenderer.fontHeight / 2.0f);
                ctx.getMatrices().scale(cfg.sprintingScale * pulse, cfg.sprintingScale * pulse);
                ctx.getMatrices().translate(-textW / 2.0f, -mc.textRenderer.fontHeight / 2.0f);
            } else {
                ctx.getMatrices().scale(cfg.sprintingScale, cfg.sprintingScale);
            }

            if (cfg.sprintingShowBackground) {
                fillRoundedGradient(ctx, -2, -2, textW + 2, mc.textRenderer.fontHeight + 2, 0x881A202C, 0x550D1117, 2);
            }
            
            int color = isSprinting ? cfg.getSprintingColorARGB() : 0xFFAAAAAA;
            ctx.drawTextWithShadow(mc.textRenderer, sprintText, 0, 0, color);

            ctx.getMatrices().popMatrix();
        }

        refreshPlayerCache(mc);
        String mode = MahikariClient.MANAGER.getCurrentMode();
        Filter filter = resolveFilter(cfg.teamHudFilter, mode);
        if (filter == Filter.DISABLED) {
            return;
        }
        ArrayList<TeamViewManager.TeammateData> visible = new ArrayList<>();
        for (TeamViewManager.TeammateData d2 : MahikariClient.MANAGER.getAll()) {
            if (!matchesFilter(filter, d2.getRole())) continue;
            visible.add(d2);
        }
        if (visible.isEmpty()) {
            return;
        }
        visible.sort(Comparator.comparingInt((TeamViewManager.TeammateData d) -> rolePriority(d.getRole())).thenComparing(TeamViewManager.TeammateData::getName, String.CASE_INSENSITIVE_ORDER));
        int maxCards = Math.min(MAX_CARDS_HARD, Math.max(1, cfg.teamHudMaxCards));
        int totalMatch = visible.size();
        int estimatedH = estimateHudHeight(cfg, visible, maxCards, totalMatch);
        int hudX = clampToScreen(cfg.teamHudOffsetX, Math.round(CARD_W * scale), mc.getWindow().getScaledWidth());
        int hudY = clampToScreen(cfg.teamHudOffsetY, Math.round(estimatedH * scale), mc.getWindow().getScaledHeight());
        int shown = 0;
        // Shift first card down by CARD_PAD_Y so its panel top (yBase - CARD_PAD_Y) lands at y=0.
        int yCursor = CARD_PAD_Y;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float)hudX, (float)hudY);
        if (scale != 1.0f) {
            m.scale(scale, scale);
        }
        for (TeamViewManager.TeammateData data : visible) {
            if (shown >= maxCards) break;
            int cardH = drawCard(ctx, mc, cfg, data, yCursor);
            // Card visually occupies [yBase - CARD_PAD_Y, yBase + cardH + CARD_PAD_Y].
            // Advance past the bottom padding then add the inter-card gap.
            yCursor += cardH + CARD_PAD_Y * 2 + CARD_GAP;
            ++shown;
        }
        if (totalMatch - shown > 0) {
            drawMoreBadge(ctx, mc, totalMatch - shown, 6, yCursor + 1);
        }
        m.popMatrix();
    }

    private static Filter resolveFilter(String configFilter, String mode) {
        if (configFilter == null) configFilter = "AUTO";
        switch (configFilter) {
            case "ALL": return Filter.ALL;
            case "PARTY_ONLY": return Filter.PARTY_ONLY;
            case "TEAM_ONLY": return Filter.TEAM_ONLY;
        }
        switch (mode == null ? "" : mode) {
            case "tri": return Filter.TEAM_ONLY;
            case "civ": return Filter.PARTY_ONLY;
            case "supperciv": return Filter.DISABLED;
        }
        return Filter.ALL;
    }

    private static boolean matchesFilter(Filter filter, String role) {
        if (filter == Filter.ALL) return true;
        if (filter == Filter.DISABLED) return false;
        if (filter == Filter.PARTY_ONLY) return "party".equals(role) || "king".equals(role) || "apollo".equals(role);
        if (filter == Filter.TEAM_ONLY) return "team".equals(role) || "apollo".equals(role) || role == null || role.isEmpty();
        return true;
    }

    private static int estimateHudHeight(TeamViewConfig cfg, List<TeamViewManager.TeammateData> visible, int maxCards, int totalMatch) {
        int shown = Math.min(maxCards, visible.size());
        int height = 0;
        for (int i = 0; i < shown; i++) {
            if (i > 0) height += CARD_GAP;
            // Match drawCard layout: each card's visual extent is cardH + 2 * CARD_PAD_Y.
            height += estimateCardHeight(cfg, visible.get(i)) + CARD_PAD_Y * 2;
        }
        if (totalMatch - shown > 0) {
            height += 14;
        }
        return Math.max(HEAD_SIZE, height);
    }

    private static int estimateCardHeight(TeamViewConfig cfg, TeamViewManager.TeammateData data) {
        // HIDE HEALTH: if not valid health, remove BAR_H + 2
        boolean hasValidHealth = data.getLastServerHealthMs() > 0 || data.getLastLocalHealthMs() > 0;
        int cardH = 12 + (hasValidHealth ? BAR_H + 2 : 0);
        if (cfg.teamHudShowEffects && hasEffects(data)) {
            cardH += EFFECT_SIZE + 2;
        }
        return cardH;
    }

    private static int clampToScreen(int value, int size, int screenSize) {
        return clampInt(value, 0, Math.max(0, screenSize - size));
    }

    private static float clampFloat(float value, float min, float max) {
        if (!Float.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void drawMoreBadge(DrawContext ctx, MinecraftClient mc, int hiddenCount, int x, int y) {
        String msg = "\u00a77+" + hiddenCount + " more";
        int textW = mc.textRenderer.getWidth(msg);
        int padX = 6;
        int h = 13;
        fillRounded(ctx, x + 1, y + 1, x + textW + padX * 2 + 1, y + h + 1, 0x66000000, 3);
        fillRounded(ctx, x, y, x + textW + padX * 2, y + h, 0xAA05070B, 3);
        ctx.fill(x + 2, y + 1, x + textW + padX * 2 - 2, y + 2, 0x18FFFFFF);
        ctx.drawText(mc.textRenderer, msg, x + padX, y + 3, 0xFFB7C0CC, true);
    }

    private static void drawCardPanel(DrawContext ctx, int x0, int y0, int x1, int y1, int radius, RoleStyle style, boolean dead, boolean lowHp) {
        int border = lowHp ? 0xCCFF3333 : dead ? 0x88444444 : style.borderHighlight;
        int bgTop = dead ? 0xEE0D0D0D : style.bgTop;
        int bgBottom = dead ? 0xEE050505 : style.bgBottom;

        // Enhanced Multi-layer Glowing Drop Shadow
        int shadowColor1 = lowHp ? 0x66FF0000 : 0x66000000;
        int shadowColor2 = lowHp ? 0x44FF0000 : 0x44000000;
        int shadowColor3 = lowHp ? 0x22FF0000 : 0x22000000;
        fillRounded(ctx, x0 - 3, y0 - 3, x1 + 3, y1 + 3, shadowColor3, radius + 3);
        fillRounded(ctx, x0 - 2, y0 - 2, x1 + 2, y1 + 2, shadowColor2, radius + 2);
        fillRounded(ctx, x0 - 1, y0 - 1, x1 + 1, y1 + 1, shadowColor1, radius + 1);

        // Modern Acrylic/Glass Background with gradient
        fillRounded(ctx, x0, y0, x1, y1, 0xBB000000, radius);
        fillRoundedGradient(ctx, x0 + 1, y0 + 1, x1 - 1, y1 - 1, bgTop, bgBottom, radius - 1);

        // Enhanced Inner Gloss/Reflection (Top highlight)
        ctx.fillGradient(x0 + 2, y0 + 2, x1 - 2, y0 + 6, 0x33FFFFFF, 0x00FFFFFF);

        // Vibrant Outer Glow Border
        if (!dead && lowHp) {
            int pulseAlpha = (int)(128 + 127 * Math.sin(System.currentTimeMillis() / 150.0));
            int pulseBorder = (pulseAlpha << 24) | 0xFF3333;
            fillRounded(ctx, x0 - 1, y0 - 1, x1 + 1, y1 + 1, pulseBorder, radius + 1);
        }

        // Refined border edges
        ctx.fill(x0 + 1, y1 - 2, x1 - 1, y1 - 1, style.borderEdge);
        ctx.fill(x0, y0 + radius, x0 + 1, y1 - radius, border);
        ctx.fill(x1 - 1, y0 + radius, x1, y1 - radius, 0x44000000);
        ctx.fill(x0 + radius, y0, x1 - radius, y0 + 1, border);
        ctx.fill(x0 + radius, y1 - 1, x1 - radius, y1, 0x66000000);

        // Enhanced Vivid Left Accent Bar with glow and gradient
        int accentGlow = (style.accentTop & 0x00FFFFFF) | 0x66000000;
        fillRoundedLeft(ctx, x0 - 2, y0, x0, y1, accentGlow, radius);
        fillRoundedLeft(ctx, x0, y0, x0 + ACCENT_W, y1, dead ? 0xAA444444 : style.accentTop, radius);

        // Accent gradient for depth
        ctx.fillGradient(x0, y0 + 1, x0 + ACCENT_W, y1 - 1, style.accentTop, style.accentBottom);

        // Inner accent shadow
        ctx.fillGradient(x0 + ACCENT_W - 1, y0 + 1, x0 + ACCENT_W, y1 - 1, 0x44000000, 0x44000000);
        ctx.fill(x0 + ACCENT_W, y0 + 1, x0 + ACCENT_W + 1, y1 - 1, 0x22000000);
    }

    private static void drawHeadFrame(DrawContext ctx, int x, int y, int size, RoleStyle style, boolean dead, boolean lowHp) {
        int border = lowHp ? 0xFFFF3333 : dead ? 0xFF666666 : style.accentTop;

        // Multi-layer outer glow for depth
        int glow1 = (border & 0x00FFFFFF) | 0x44000000;
        int glow2 = (border & 0x00FFFFFF) | 0x22000000;
        ctx.fill(x - 3, y - 3, x + size + 3, y + size + 3, glow2);
        ctx.fill(x - 2, y - 2, x + size + 2, y + size + 2, glow1);

        // Dark inner frame base
        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xEE111111);

        // Vibrant border with gradient
        ctx.fillGradient(x - 1, y - 1, x + size + 1, y, border, border);
        ctx.fillGradient(x - 1, y + size, x + size + 1, y + size + 1, 0xAA000000, 0xAA000000);
        ctx.fill(x - 1, y, x, y + size, border);
        ctx.fill(x + size, y, x + size + 1, y + size, 0xAA000000);

        // Top highlight for glass effect
        ctx.fillGradient(x, y, x + size, y + 2, 0x33FFFFFF, 0x00FFFFFF);
    }

    private static int rolePriority(String role) {
        if ("king".equals(role)) return 0;
        if ("party".equals(role) || "apollo".equals(role)) return 1;
        return 2;
    }

    private static boolean hasEffects(TeamViewManager.TeammateData data) {
        AbstractClientPlayerEntity entity = playerCache.get(data.getName().toLowerCase(java.util.Locale.ROOT));
        if (entity != null) {
            return !entity.getStatusEffects().isEmpty();
        }
        return !data.getRemoteEffects().isEmpty();
    }

    public static int getCardWidth() {
        return CARD_W;
    }

    public static int drawCard(DrawContext ctx, MinecraftClient mc, TeamViewConfig cfg, TeamViewManager.TeammateData data, int yBase) {
        String role = data.getRole();
        RoleStyle style = getRoleStyle(data);

        int headX = ACCENT_W + 4;
        int contentX = headX + HEAD_SIZE + HEAD_GAP;
        int barRight = contentX + BAR_W;
        
        int panelX0 = 0;
        int panelX1 = barRight + 6;

        float realHp = data.getHealth();
        float absHp = data.getAbsorption();
        float totalHp = realHp + absHp;
        float maxHp = data.getMaxHealth();
        boolean dead = !data.isOnline() || realHp <= 0.0f;
        
        boolean isHoplite = "king".equals(role) || "party".equals(role) || "team".equals(role) || "apollo".equals(role);
        boolean hasValidHealth = !isHoplite && (data.getLastServerHealthMs() > 0 || data.getLastLocalHealthMs() > 0);
        
        if (!dead) {
            data.smoothHealthToward(realHp, absHp);
        } else {
            data.smoothHealthToward(0.0f, 0.0f);
        }
        float displayHp = data.getDisplayHealth();
        float trailHp = data.getTrailHealth();
        float displayAbs = data.getDisplayAbs();
        float trailAbs = data.getTrailAbs();
        
        float displayPct = maxHp > 0.0f ? Math.max(0.0f, displayHp / maxHp) : 0.0f;
        boolean lowHp = hasValidHealth && !dead && displayPct > 0.0f && displayPct < 0.25f;

        // Calculate card height
        int textRowY = yBase + 1;
        int barY = yBase + 12;
        int cardBottomBase = hasValidHealth ? barY + BAR_H + 2 : barY + 2;

        boolean showEffects = cfg.teamHudShowEffects && hasEffects(data);
        int cardH = cardBottomBase - yBase;
        if (showEffects) {
            cardH += EFFECT_SIZE + 2;
        }

        int panelY0 = yBase - 3;
        int panelY1 = yBase + cardH + 3;
        int R = 3; // corner radius

        long lastDmg = data.getLastDamageTimeMs();
        long now = System.currentTimeMillis();
        boolean takingDamage = hasValidHealth && (!dead && now - lastDmg < 400L);

        drawCardPanel(ctx, panelX0, panelY0, panelX1, panelY1, R, style, dead, lowHp);

        if (takingDamage) {
            float flashAlpha = (1.0f - ((now - lastDmg) / 400.0f)) * 0.35f;
            int flashOverlay = ((int)(flashAlpha * 255)) << 24 | 0xFF0000;
            fillRounded(ctx, panelX0, panelY0, panelX1, panelY1, flashOverlay, R);
        }

        // Head
        drawHeadFrame(ctx, headX, yBase, HEAD_SIZE, style, dead, lowHp);
        drawHead(ctx, mc, data, headX, yBase, HEAD_SIZE);
        if (dead) {
            ctx.fill(headX, yBase, headX + HEAD_SIZE, yBase + HEAD_SIZE, 0x88000000);
        } else if (takingDamage) {
            float flashAlpha = 1.0f - ((now - lastDmg) / 400.0f);
            int flashOverlay = ((int)(flashAlpha * 180)) << 24 | 0xFF3333;
            ctx.fill(headX, yBase, headX + HEAD_SIZE, yBase + HEAD_SIZE, flashOverlay);
        } else if (lowHp) {
            float pulse = (float)((Math.sin((double)System.currentTimeMillis() / 120.0) + 1.0) / 2.0);
            int redOverlay = (90 + (int)(pulse * 120.0f)) << 24 | 0xCC0000;
            ctx.fill(headX, yBase, headX + HEAD_SIZE, yBase + HEAD_SIZE, redOverlay);
        }

        // Name and info text
        int nameColor = dead ? 0xFF888888 : style.nameColor;
        String sideInfo = dead ? "" : buildSideInfo(mc, data, cfg);
        int sideInfoW = sideInfo.isEmpty() ? 0 : mc.textRenderer.getWidth(sideInfo);
        int sideInfoGap = sideInfo.isEmpty() ? 0 : 3;
        
        boolean showHp = hasValidHealth && cfg.teamHudShowHpText && !dead;
        String hpStr = showHp ? formatHp(totalHp, absHp) : "";
        int hpStrW = showHp ? mc.textRenderer.getWidth(hpStr) : 0;
        int hpBlockW = showHp ? hpStrW + 2 + HEART_SIZE : 0;
        
        int maxNameW = Math.max(8, BAR_W - (showHp ? hpBlockW + 3 : 0) - sideInfoGap - sideInfoW);
        String trimmedName = trimToWidth(mc, data.getName(), maxNameW);
        
        ctx.drawText(mc.textRenderer, trimmedName, contentX, textRowY, nameColor, true);
        
        if (!sideInfo.isEmpty()) {
            int nameW = mc.textRenderer.getWidth(trimmedName);
            ctx.drawText(mc.textRenderer, sideInfo, contentX + nameW + sideInfoGap, textRowY, 0xFFAAAAAA, true);
        }
        
        if (dead) {
            int nameW = mc.textRenderer.getWidth(trimmedName);
            int strikeY = textRowY + 4;
            ctx.fill(contentX - 1, strikeY, contentX + nameW + 1, strikeY + 1, 0xFFFF5555);
        }
        
        if (showHp) {
            int heartX = barRight - HEART_SIZE;
            int textX = heartX - 2 - hpStrW;
            ctx.drawText(mc.textRenderer, hpStr, textX, textRowY, lowHp ? 0xFFFF5555 : 0xFFDDDDDD, true);
            ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_FULL, heartX, textRowY - 1, HEART_SIZE, HEART_SIZE);
        }

        // Health bar
        if (hasValidHealth) {
            drawHpBar(ctx, contentX, barY, BAR_W, BAR_H, displayHp, trailHp, displayAbs, trailAbs, maxHp, !dead, style);
        }

        // Effects row
        if (showEffects) {
            int effY = cardBottomBase;
            if (!drawEffectsRow(ctx, mc, headX, effY, data)) {
                cardH -= EFFECT_SIZE + 2;
            }
        }

        return cardH;
    }

    // Role-based style definitions
    private static RoleStyle getRoleStyle(TeamViewManager.TeammateData data) {
        String role = data.getRole();
        if ("king".equals(role)) return STYLE_KING;
        if ("party".equals(role)) return STYLE_PARTY;
        if ("team".equals(role)) return STYLE_TEAM;

        if (data.getApolloColor() != null) {
            int rgb = data.getApolloColor().getRGB();
            int topBg = 0xCC000000 | (darken(rgb, 0.15f) & 0xFFFFFF);
            int bottomBg = 0xCC000000 | (darken(rgb, 0.05f) & 0xFFFFFF);
            int accentTop = 0xFF000000 | rgb;
            int accentBot = 0xFF000000 | darken(rgb, 0.7f);
            int highlight = 0x33000000 | brighten(rgb, 1.2f);
            int edge = 0x18000000 | rgb;
            int nameColor = 0xFF000000 | brighten(rgb, 1.4f);
            return new RoleStyle(topBg, bottomBg, accentTop, accentBot, highlight, edge, nameColor);
        }
        return STYLE_TEAM;
    }

    // King: Gold / amber royal theme - Enhanced
    private static final RoleStyle STYLE_KING = new RoleStyle(
        0xEE2A1C0A, 0xEE1A1104,           // bg top/bottom (premium deep amber dark)
        0xFFFFCC00, 0xFFDD9900,           // accent top/bottom (vibrant gold)
        0xCCFFAA00, 0x44885500,           // border high/edge
        0xFFFFDD44                        // name color (brighter)
    );

    // Party: Cyan / blue cool theme - Enhanced
    private static final RoleStyle STYLE_PARTY = new RoleStyle(
        0xEE0D1826, 0xEE070D17,           // bg top/bottom (premium navy dark)
        0xFF00EEFF, 0xFF0088DD,           // accent top/bottom (vibrant neon cyan)
        0xCC00DDFF, 0x44003366,           // border high/edge
        0xFF66FFFF                        // name color (brighter)
    );

    // Team: Vibrant green / emerald - Enhanced
    private static final RoleStyle STYLE_TEAM = new RoleStyle(
        0xEE141C16, 0xEE0A0F0B,           // bg top/bottom (premium dark emerald)
        0xFF44FF77, 0xFF22BB55,           // accent top/bottom (vibrant green)
        0xCC33EE66, 0x44004411,           // border high/edge
        0xFF88FF88                        // name color (brighter)
    );

    private record RoleStyle(
        int bgTop, int bgBottom,
        int accentTop, int accentBottom,
        int borderHighlight, int borderEdge,
        int nameColor
    ) {}

    private static void drawHpBar(DrawContext ctx, int x, int y, int w, int h, float displayHp, float trailHp, float displayAbs, float trailAbs, float maxHp, boolean alive, RoleStyle style) {
        // Enhanced Multi-layer Drop shadow for health bar
        fillRounded(ctx, x - 2, y - 2, x + w + 2, y + h + 2, 0x44000000, 3);
        fillRounded(ctx, x - 1, y - 1, x + w + 1, y + h + 1, 0x55000000, 2);

        // Rounded inner track with gradient
        fillRoundedGradient(ctx, x, y, x + w, y + h, 0xEE08080C, 0xEE050508, 2);

        if (!alive) {
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x66FF2222);
            return;
        }
        if (maxHp <= 0.0f) return;

        float hpPct = Math.max(0.0f, Math.min(1.0f, displayHp / maxHp));
        float trailPct = Math.max(0.0f, Math.min(1.0f, trailHp / maxHp));
        float absPct = Math.max(0.0f, Math.min(1.0f, displayAbs / maxHp));
        float absTrailPct = Math.max(0.0f, Math.min(1.0f, trailAbs / maxHp));

        // Enhanced Damage trail with glow
        if (trailPct > hpPct + 0.005f) {
            int tw = Math.round(trailPct * w);
            int hw = Math.round(hpPct * w);
            ctx.fillGradient(x + hw, y + 1, x + tw, y + h - 1, 0xCCFF9944, 0xCCCC7722);

            // Trail glow
            ctx.fillGradient(x + hw, y, x + tw, y + 1, 0x66FF9944, 0x66CC7722);
        }

        // Base health with enhanced gradient and glow
        int hpW = Math.round(hpPct * w);
        if (hpW > 0) {
            int hpC = hpColor(hpPct);
            int hpBright = brighten(hpC, 1.2f);
            int hpDark = darken(hpC, 0.6f);

            // Health glow
            int glowAlpha = (int)(80 * hpPct);
            int glowColor = (glowAlpha << 24) | (hpC & 0x00FFFFFF);
            fillRounded(ctx, x + 1, y - 1, x + hpW, y, glowColor, 1);

            // Health fill with gradient
            ctx.fillGradient(x + 1, y + 1, x + hpW, y + h - 1, hpBright, hpDark);
        }

        // Enhanced Absorption segment with glow
        int absStart = hpW;
        int absW = Math.min(Math.round(absPct * w), w - absStart);
        int absTrailW = Math.min(Math.round(absTrailPct * w), w - absStart);

        // Absorption trail
        if (absTrailW > absW && absTrailW > 0) {
            ctx.fillGradient(x + absStart + absW, y + 1, x + absStart + absTrailW, y + h - 1, 0xCCDDAA33, 0xCCBB8822);
        }

        // Absorption fill with glow
        if (absW > 0) {
            // Absorption glow
            fillRounded(ctx, x + absStart, y - 1, x + absStart + absW, y, 0x66FFDD44, 1);

            // Absorption gradient
            ctx.fillGradient(x + absStart, y + 1, x + absStart + absW, y + h - 1, 0xFFFFDD44, 0xFFDDAA11);
        }

        // Enhanced Glossy top highlight
        int totalFill = Math.min(w, absStart + Math.max(absW, absTrailW));
        totalFill = Math.max(totalFill, Math.round(Math.max(hpPct, trailPct) * w));
        if (totalFill > 2) {
            ctx.fillGradient(x + 1, y + 1, x + totalFill, y + 2, 0x44FFFFFF, 0x22FFFFFF);
        }

        // Animated shimmer effect on health bar
        if (hpW > 4) {
            float shimmer = (float)(Math.sin(System.currentTimeMillis() / 800.0) * 0.5 + 0.5);
            int shimmerX = x + 2 + (int)(hpW * shimmer * 0.3f);
            int shimmerW = Math.min(8, hpW / 4);
            ctx.fillGradient(shimmerX, y + 1, shimmerX + shimmerW, y + h - 1, 0x00FFFFFF, 0x44FFFFFF);
            ctx.fillGradient(shimmerX + shimmerW, y + 1, shimmerX + shimmerW * 2, y + h - 1, 0x44FFFFFF, 0x00FFFFFF);
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
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int hpColor(float pct) {
        if (pct >= 0.6f) return 0xFF55CC55;  // Green
        if (pct >= 0.35f) {
            float t = (pct - 0.35f) / 0.25f;
            return lerpColor(0xFFDDAA33, 0xFF55CC55, t);
        }
        if (pct >= 0.15f) {
            float t = (pct - 0.15f) / 0.2f;
            return lerpColor(0xFFDD4444, 0xFFDDAA33, t);
        }
        float t = Math.max(0.0f, pct / 0.15f);
        return lerpColor(0xFFCC2222, 0xFFDD4444, t);
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0.0f, Math.min(1.0f, t));
        int aa = a >> 24 & 0xFF, ar = a >> 16 & 0xFF, ag = a >> 8 & 0xFF, ab = a & 0xFF;
        int ba = b >> 24 & 0xFF, br = b >> 16 & 0xFF, bg = b >> 8 & 0xFF, bb = b & 0xFF;
        int al = (int)((float)aa + (float)(ba - aa) * t);
        int r = (int)((float)ar + (float)(br - ar) * t);
        int g = (int)((float)ag + (float)(bg - ag) * t);
        int bl = (int)((float)ab + (float)(bb - ab) * t);
        return al << 24 | r << 16 | g << 8 | bl;
    }

    private static String buildSideInfo(MinecraftClient mc, TeamViewManager.TeammateData data, TeamViewConfig cfg) {
        if (mc.world == null) return "";
        String localWorld = mc.world.getRegistryKey().getValue().getPath();
        String tWorld = data.getWorld();
        if (!WorldFormat.sameWorld(tWorld, localWorld)) {
            return "\u00a7c" + WorldFormat.formatWorld(tWorld);
        }
        StringBuilder sb = new StringBuilder();
        String biomeIcon = cfg.showBiome ? WorldFormat.getBiomeIcon(data.getBiome()) : "";
        if (!biomeIcon.isEmpty()) sb.append("\u00a7f").append(biomeIcon);
        
        if (cfg.showDistance) {
            double dist = computeDistanceTo(mc, data);
            if (dist >= 0.0) {
                if (sb.length() > 0) sb.append(' ');
                sb.append("\u00a77(").append((int) Math.round(dist)).append("m)");
            }
        }
        return sb.toString();
    }

    private static double computeDistanceTo(MinecraftClient mc, TeamViewManager.TeammateData data) {
        double tx, ty, tz;
        if (mc.player == null) return -1.0;
        AbstractClientPlayerEntity entity = playerCache.get(data.getName().toLowerCase(java.util.Locale.ROOT));
        if (entity != null) {
            tx = entity.getX(); ty = entity.getY(); tz = entity.getZ();
        } else if (data.hasRecentServerData()) {
            tx = data.getDisplayX(); ty = data.getDisplayY(); tz = data.getDisplayZ();
        } else {
            return -1.0;
        }
        double dx = tx - mc.player.getX(), dy = ty - mc.player.getY(), dz = tz - mc.player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static String formatHp(float totalHp, float absorption) {
        String formatted = formatNumber(totalHp);
        if (absorption > 0.0f) {
            return "\u00a7e" + formatted; // Gold text if they have absorption
        }
        return formatted;
    }

    private static String formatNumber(float value) {
        if (value <= 0.0f) return "0";
        long rounded = Math.round(value);
        return String.format(java.util.Locale.US, "%,d", rounded);
    }

    private static String trimToWidth(MinecraftClient mc, String s, int maxW) {
        if (mc.textRenderer.getWidth(s) <= maxW) return s;
        int ellipsisW = mc.textRenderer.getWidth("\u2026");
        return mc.textRenderer.trimToWidth(s, Math.max(0, maxW - ellipsisW)) + "\u2026";
    }

    private static void drawHead(DrawContext ctx, MinecraftClient mc, TeamViewManager.TeammateData data, int x, int y, int size) {
        String name = data.getName();
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh != null) {
            PlayerListEntry entry = nh.getPlayerListEntry(name);
            if (entry == null && data.getUuid() != null) {
                entry = nh.getPlayerListEntry(data.getUuid());
            }
            if (entry != null) {
                PlayerSkinDrawer.draw(ctx, entry.getSkinTextures(), x, y, size);
                return;
            }
        }
        UUID uuid = data.getUuid() != null ? data.getUuid() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        PlayerSkinDrawer.draw(ctx, DefaultSkinHelper.getSkinTextures(uuid), x, y, size);
    }

    private static boolean drawEffectsRow(DrawContext ctx, MinecraftClient mc, int x, int y, TeamViewManager.TeammateData data) {
        AbstractClientPlayerEntity entity = playerCache.get(data.getName().toLowerCase(java.util.Locale.ROOT));
        if (entity != null) {
            Collection<StatusEffectInstance> live = entity.getStatusEffects();
            if (live.isEmpty()) return false;
            int drawn = 0;
            for (StatusEffectInstance inst : live) {
                if (drawn >= MAX_EFFECTS) break;
                Identifier tex = InGameHud.getEffectTexture(inst.getEffectType());
                float alpha = expiringAlpha(inst.getDuration());
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tex, x + drawn * 10, y, EFFECT_SIZE, EFFECT_SIZE, alpha);
                ++drawn;
            }
            return drawn > 0;
        }
        List<TeamViewManager.RemoteEffect> remote = data.getRemoteEffects();
        if (remote.isEmpty()) return false;
        int drawn = 0;
        for (TeamViewManager.RemoteEffect eff : remote) {
            if (drawn >= MAX_EFFECTS) break;
            try {
                Identifier tex = Identifier.ofVanilla("mob_effect/" + eff.effectId);
                int remainingTicks = data.getEstimatedRemainingTicks(eff);
                if (remainingTicks <= 0) continue;
                float alpha = expiringAlpha(remainingTicks);
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, tex, x + drawn * 10, y, EFFECT_SIZE, EFFECT_SIZE, alpha);
                ++drawn;
            } catch (Throwable t) {
                // ignore
            }
        }
        return drawn > 0;
    }

    private static float expiringAlpha(int remainingTicks) {
        if (remainingTicks > 200) return 1.0f;
        if (remainingTicks <= 0) return 0.0f;
        double phase = (double)System.currentTimeMillis() / 100.0;
        return 0.3f + 0.7f * (float)(0.5 + 0.5 * Math.cos(phase));
    }

    private static void refreshPlayerCache(MinecraftClient mc) {
        long now = System.nanoTime() / 50000000L;
        if (now == lastCacheTick) return;
        lastCacheTick = now;
        playerCache.clear();
        if (mc.world == null) return;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            playerCache.put(p.getName().getString().toLowerCase(java.util.Locale.ROOT), p);
            TeamViewManager.TeammateData td = MahikariClient.MANAGER.findByName(p.getName().getString());
            if (td != null) {
                td.updateLocalHealth(p.getHealth(), p.getMaxHealth(), p.getAbsorptionAmount());
            }
        }
    }

    private enum Filter { ALL, PARTY_ONLY, TEAM_ONLY, DISABLED }

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
}
