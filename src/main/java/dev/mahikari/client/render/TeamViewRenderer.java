package dev.mahikari.client.render;

import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import dev.mahikari.client.render.ProjectionCapture;
import dev.mahikari.client.util.WorldFormat;
import java.util.HashMap;
import java.util.HashSet;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class TeamViewRenderer {
    private static final int C_GRAY = -5592406;
    private static final float PHYS_PAD_TOP = 22.0f;
    private static final float PHYS_PAD_SIDE = 55.0f;
    private static final float PHYS_PAD_BOTTOM = 130.0f;
    private static final String ICON_TEAM = "▼";
    private static final String ICON_KING = "♛";
    private static final String ICON_PARTY = "▼";
    private static final int C_KING_ARROW = -570438656;
    private static final int C_KING_SHADOW = -2141895680;
    private static final int C_KING_TEXT = -8892;
    private static final int C_PARTY_ARROW = -583833857;
    private static final int C_PARTY_SHADOW = -2146360747;
    private static final int C_PARTY_TEXT = -11171585;
    private static final String ICON_VIP = "VIP";
    private static final int TRANSLUCENT_ALPHA = 176;
    private static final int VERY_TRANSLUCENT_ALPHA = 85;
    private static final HashMap<String, AbstractClientPlayerEntity> playerCache = new HashMap<>();
    private static long lastCacheTick;

    public static void register() {
        HudRenderCallback.EVENT.register(TeamViewRenderer::onHudRender);
    }

    private static void onHudRender(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }
        if (!ProjectionCapture.isReady()) {
            return;
        }
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.enabled) {
            return;
        }
        float tickDelta = tick.getTickProgress(false);
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double guiScale = mc.getWindow().getScaleFactor();
        String playerWorld = mc.world.getRegistryKey().getValue().getPath();
        int cA = cfg.getArrowColorARGB();
        int cS = cfg.getShadowARGB();
        int cT = cfg.getTextARGB();
        float oss = cfg.offScreenScale;
        float padT = (float)(22.0 / guiScale);
        float padS = (float)(55.0 / guiScale);
        float padB = (float)(130.0 / guiScale);
        double camX = ProjectionCapture.getCamX();
        double camY = ProjectionCapture.getCamY();
        double camZ = ProjectionCapture.getCamZ();
        TeamViewRenderer.refreshPlayerCache(mc);
        String mode = cfg.viewMode;
        HashSet<String> rendered = new HashSet<>();

        for (TeamViewManager.TeammateData data : MahikariClient.MANAGER.getAll()) {
            String rIcon;
            int rS;
            int rA;
            boolean entityLoaded;
            double playerDist;
            String biome;
            String tWorld;
            double tz;
            double ty;
            double tx;
            boolean isPartyOrKing;
            if (!data.isOnline()) continue;
            String role = data.getRole();
            isPartyOrKing = "party".equals(role) || "king".equals(role) || "vip".equals(role) || "apollo".equals(role);
            if ("PARTY_ONLY".equals(mode) && !isPartyOrKing && !role.isEmpty()) continue;
            AbstractClientPlayerEntity entity = playerCache.get(TeamViewManager.getNameKey(data.getName()));
            String identityKey = identityKey(data, entity);
            if (identityKey.isEmpty() || !rendered.add(identityKey)) continue;
            if (entity != null) {
                data.updateUuid(entity.getUuid());
                Vec3d pos = entity.getLerpedPos(tickDelta);
                tx = pos.x;
                ty = pos.y + (double)entity.getHeight() + (double)0.7f;
                tz = pos.z;
                tWorld = playerWorld;
                biome = data.getBiome();
                double ed = pos.x - camX;
                double ey = pos.y + (double)entity.getHeight() / 2.0 - camY;
                double ez = pos.z - camZ;
                playerDist = Math.sqrt(ed * ed + ey * ey + ez * ez);
                entityLoaded = true;
                data.snapDisplay(tx, ty, tz);
            } else {
                if (!data.hasRecentServerData()) continue;
                // Use server position directly. Exponential smoothing toward a stale server
                // pos makes the name visibly lag behind the moving player and, on fast camera
                // turns, leaves a ghost name briefly visible at the old screen edge.
                tx = data.getServerX();
                ty = data.getServerY() + 1.6;
                tz = data.getServerZ();
                data.snapDisplay(tx, ty, tz);
                tWorld = data.getWorld();
                biome = data.getBiome();
                double d0 = tx - camX;
                double d1 = ty - camY;
                double d2 = tz - camZ;
                playerDist = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                entityLoaded = false;
            }
            boolean sameWorld = WorldFormat.sameWorld(tWorld, playerWorld);
            String icon = cfg.showBiome ? WorldFormat.getBiomeIcon(biome) : "";
            int rT;
            if ("king".equals(role)) {
                rA = -570438656;
                rS = -2141895680;
                rT = -8892;
            } else if ("party".equals(role)) {
                rA = -583833857;
                rS = -2146360747;
                rT = -11171585;
            } else if ("vip".equals(role)) {
                rA = 0xDDFF4FD8;
                rS = 0x802B061E;
                rT = 0xFFFF9BEE;
            } else if ("team".equals(role)) {
                rA = cA;
                rS = cS;
                rT = cT;
            } else if (data.getApolloColor() != null) {
                int rgb = data.getApolloColor().getRGB();
                rA = 0xDD000000 | (rgb & 0xFFFFFF);
                rS = 0x80000000 | (rgb & 0xFFFFFF);
                rT = 0xFF000000 | brighten(rgb);
            } else {
                rA = cA;
                rS = cS;
                rT = cT;
            }
            float[] proj = ProjectionCapture.project(tx, ty, tz, sw, sh);
            boolean inFront = proj[5] > 0.0f;
            boolean onScreen = inFront && proj[0] > padS && proj[0] < (float)sw - padS && proj[1] > padT && proj[1] < (float)sh - padB;
            switch (role) {
                case "king": {
                    rIcon = ICON_KING;
                    break;
                }
                case "party": {
                    rIcon = ICON_PARTY;
                    break;
                }
                case "vip": {
                    rIcon = ICON_VIP;
                    break;
                }
                default: {
                    rIcon = ICON_TEAM;
                }
            }
            if (onScreen && cfg.onScreenEnabled) {
                int displayGray;
                boolean occluded = TeamViewRenderer.isOccludedByForegroundPlayer(mc, proj[0], proj[1], playerDist, tickDelta, sw, sh, padT, padS, padB);
                float distScale = (float)Math.max(0.25, Math.min(1.2, 6.0 / playerDist));
                float sc = distScale * cfg.scaleMultiplier * ProjectionCapture.getZoomScale() * 1.5f;
                int displayRT = occluded ? TeamViewRenderer.veryTranslucent(rT) : TeamViewRenderer.translucent(rT);
                int n = displayGray = occluded ? TeamViewRenderer.veryTranslucent(-5592406) : TeamViewRenderer.translucent(-5592406);
                if (entityLoaded && sameWorld) {
                    TeamViewRenderer.drawNearIcon(ctx, mc, proj[0], proj[1], sc, displayRT, rIcon, role);
                    continue;
                }
                String nameDisplay = data.getName();
                String tagLine = TeamViewRenderer.buildTagLine(sameWorld, tWorld, icon, cfg.showDistance, playerDist);
                TeamViewRenderer.drawFarHologram(ctx, mc, proj[0], proj[1], sc, nameDisplay, tagLine, displayRT, displayGray, rIcon, role);
                continue;
            }
            if (onScreen || !cfg.offScreenEnabled || !cfg.offScreenNear && entityLoaded && playerDist <= (double)cfg.nearRange && sameWorld || "ALL_PARTY_OFFSCREEN".equals(mode) && !isPartyOrKing) continue;
            // For players behind the camera (vz < 0), negate both vy and vx to correct screen-edge angle
            float eoVx = proj[2];
            float eoVy = proj[3];
            if (proj[4] < 0) { eoVx = -eoVx; eoVy = -eoVy; }
            double angle = Math.atan2(-eoVy, eoVx);
            String biomeTag = sameWorld ? (icon.isEmpty() ? "" : "§f" + icon) : "§c" + WorldFormat.formatWorld(tWorld);
            String distStr = String.valueOf((int) Math.round(playerDist));
            TeamViewRenderer.drawEdgeArrow(ctx, mc, angle, sw, sh, padT, padS, padB, oss, data.getName(), distStr, biomeTag, TeamViewRenderer.translucent(rA), TeamViewRenderer.translucent(rS), TeamViewRenderer.translucent(rT));
        }
    }

    private static boolean isOccludedByForegroundPlayer(MinecraftClient mc, float teammateScreenX, float teammateScreenY, double teammateDist, float tickDelta, int sw, int sh, float padT, float padS, float padB) {
        if (mc.world == null) {
            return false;
        }
        float OCCLUSION_RADIUS = 24.0f;
        double MIN_DEPTH_GAP = 1.5;
        double camX = ProjectionCapture.getCamX();
        double camY = ProjectionCapture.getCamY();
        double camZ = ProjectionCapture.getCamZ();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            float[] proj;
            double dz;
            if (p == mc.player) continue;
            Vec3d pos = p.getLerpedPos(tickDelta);
            double dx = pos.x - camX;
            double headY = pos.y + (double)p.getHeight() * 0.6;
            double dy = headY - camY;
            double dist = Math.sqrt(dx * dx + dy * dy + (dz = pos.z - camZ) * dz);
            if (dist + 1.5 >= teammateDist || (proj = ProjectionCapture.project(pos.x, headY, pos.z, sw, sh))[5] <= 0.0f || Math.abs(proj[0] - teammateScreenX) > 24.0f || Math.abs(proj[1] - teammateScreenY) > 24.0f) continue;
            return true;
        }
        return false;
    }

    private static int translucent(int argb) {
        return argb & 0xFFFFFF | 0xB0000000;
    }

    private static int veryTranslucent(int argb) {
        return argb & 0xFFFFFF | 0x55000000;
    }

    private static void drawNearIcon(DrawContext ctx, MinecraftClient mc, float x, float y, float sc, int cT, String icon, String role) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(sc, sc);
        int iw = mc.textRenderer.getWidth(icon);
        int px = 3, py = 2;
        int pillL = -iw / 2 - px, pillR = iw / 2 + px + 1;
        int pillT = -10 - py, pillB = -10 + 9 + py;
        // Background and accent removed as per request
        ctx.drawText(mc.textRenderer, icon, -iw / 2, -10, cT, true);
        m.popMatrix();
    }

    private static void drawFarHologram(DrawContext ctx, MinecraftClient mc, float x, float y, float sc, String name, String tagLine, int cT, int cGray, String icon, String role) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(sc, sc);

        // Compute total content width for background pill
        int iw = mc.textRenderer.getWidth(icon);
        int nw = mc.textRenderer.getWidth(name);
        int tw = tagLine.isEmpty() ? 0 : mc.textRenderer.getWidth(tagLine);
        int maxW = Math.max(iw, Math.max(nw, tw));

        int pillPadX = 5;
        int pillPadY = 2;
        int pillTop = -36 - pillPadY;
        int pillBottom = (tagLine.isEmpty() ? -12 : -2) + pillPadY;
        int pillLeft = -maxW / 2 - pillPadX;
        int pillRight = maxW / 2 + pillPadX + 1;

        // Background and accent removed as per request

        ctx.drawText(mc.textRenderer, icon, -iw / 2, -34, cT, true);
        ctx.drawText(mc.textRenderer, name, -nw / 2, -22, cT, true);
        if (!tagLine.isEmpty()) {
            ctx.drawText(mc.textRenderer, tagLine, -tw / 2, -12, cGray, true);
        }
        m.popMatrix();
    }

    // Role accent color helpers
    private static int roleAccentTop(String role) {
        return switch (role == null ? "" : role) {
            case "king" -> 0xCCE8A825;
            case "vip" -> 0xCCFF4FD8;
            case "party" -> 0xCC22AAEE;
            default -> 0x8844AA55;
        };
    }

    private static int roleAccentBottom(String role) {
        return switch (role == null ? "" : role) {
            case "king" -> 0xCCCC8800;
            case "vip" -> 0xCCD81B8C;
            case "party" -> 0xCC1177CC;
            default -> 0x88227733;
        };
    }

    private static int roleAccentBg(String role) {
        return switch (role == null ? "" : role) {
            case "king" -> 0x880F0A04;
            case "vip" -> 0x88140712;
            case "party" -> 0x88040A0F;
            default -> 0x88080808;
        };
    }

    private static int roleBorderHighlight(String role) {
        return switch (role == null ? "" : role) {
            case "king" -> 0x33FFCC44;
            case "vip" -> 0x33FF5CDC;
            case "party" -> 0x3344CCFF;
            default -> 0x18FFFFFF;
        };
    }

    private static String buildTagLine(boolean sameWorld, String tWorld, String icon, boolean showDist, double dist) {
        StringBuilder sb = new StringBuilder();
        if (!sameWorld) {
            sb.append("\u00a7c").append(WorldFormat.formatWorld(tWorld));
        } else if (!icon.isEmpty()) {
            sb.append("\u00a7f").append(icon);
        }
        if (showDist) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("\u00a77(").append((int) Math.round(dist)).append("m)");
        }
        return sb.toString();
    }

    private static void refreshPlayerCache(MinecraftClient mc) {
        long now = System.nanoTime() / 50000000L;
        if (now == lastCacheTick) {
            return;
        }
        lastCacheTick = now;
        playerCache.clear();
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            playerCache.put(TeamViewManager.getNameKey(p.getName().getString()), p);
        }
    }

    private static String identityKey(TeamViewManager.TeammateData data, AbstractClientPlayerEntity entity) {
        if (entity != null && entity.getUuid() != null) {
            return "uuid:" + entity.getUuid();
        }
        if (data.getUuid() != null) {
            return "uuid:" + data.getUuid();
        }
        String nameKey = TeamViewManager.getNameKey(data.getName());
        return nameKey.isEmpty() ? "" : "name:" + nameKey;
    }

    private static void drawEdgeArrow(DrawContext ctx, MinecraftClient mc, double angle, int sw, int sh, float padT, float padS, float padB, float oss, String name, String distStr, String biomeTag, int cA, int cS, int cT) {
        double t2;
        double t1;
        int cx = sw / 2;
        int cy = sh / 2;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double tMin = 1.0E9;
        if (Math.abs(cos) > 0.001) {
            t1 = (double)((float)sw - padS - (float)cx) / cos;
            t2 = (double)(padS - (float)cx) / cos;
            if (t1 > 0.0) {
                tMin = Math.min(tMin, t1);
            }
            if (t2 > 0.0) {
                tMin = Math.min(tMin, t2);
            }
        }
        if (Math.abs(sin) > 0.001) {
            t1 = (double)((float)sh - padB - (float)cy) / sin;
            t2 = (double)(padT - (float)cy) / sin;
            if (t1 > 0.0) {
                tMin = Math.min(tMin, t1);
            }
            if (t2 > 0.0) {
                tMin = Math.min(tMin, t2);
            }
        }
        if (tMin >= 1.0E9) {
            tMin = 100.0;
        }
        float ax = (float)((double)cx + cos * tMin);
        float ay = (float)((double)cy + sin * tMin);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(ax, ay);
        m.scale(oss, oss);
        m.pushMatrix();
        m.rotate((float)(angle + 1.5707963267948966));
        TeamViewRenderer.drawStyledArrow(ctx, cA, cS);
        m.popMatrix();

        // Build label text
        String line1 = biomeTag.isEmpty() ? "\u00a77(" + distStr + "m)" : biomeTag + " \u00a77(" + distStr + "m)";
        int l1w = mc.textRenderer.getWidth(line1);
        int nw = mc.textRenderer.getWidth(name);
        int labelMaxW = Math.max(l1w, nw);

        // Dark pill background behind label text removed as per request

        ctx.drawText(mc.textRenderer, line1, -l1w / 2, 12, TeamViewRenderer.translucent(-5592406), true);
        ctx.drawText(mc.textRenderer, name, -nw / 2, 22, cT, true);
        m.popMatrix();
    }

    private static void drawStyledArrow(DrawContext ctx, int cA, int cS) {
        int aBody = cA & 0xFFFFFF | 0xDD000000;
        int aGlow = cA & 0xFFFFFF | 0x30000000;
        int aBright = TeamViewRenderer.brighten(cA);
        TeamViewRenderer.drawTri(ctx, 2, 3, 12, 9, 0x40000000);
        TeamViewRenderer.drawTri(ctx, 1, 1, 11, 8, aGlow);
        TeamViewRenderer.drawTri(ctx, 0, 0, 10, 7, cS);
        TeamViewRenderer.drawTri(ctx, 0, 0, 9, 6, aBody);
        TeamViewRenderer.drawTri(ctx, 0, -1, 6, 3, aBright);
    }

    private static void drawTri(DrawContext ctx, int ox, int oy, int h, int bh, int c) {
        for (int r = 0; r < h; ++r) {
            int hw = bh * r / Math.max(h - 1, 1);
            ctx.fill(ox - hw, oy - h + r, ox + hw + 1, oy - h + r + 1, c);
        }
    }

    private static int brighten(int argb) {
        int a = argb >> 24 & 0xFF;
        int r = Math.min(255, (argb >> 16 & 0xFF) + 70);
        int g = Math.min(255, (argb >> 8 & 0xFF) + 70);
        int b = Math.min(255, (argb & 0xFF) + 70);
        return a << 24 | r << 16 | g << 8 | b;
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
