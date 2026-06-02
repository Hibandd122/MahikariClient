package dev.mahikari.client.render;

import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.render.ProjectionCapture;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

public final class NametagRenderer {

    public static void register() {
        HudRenderCallback.EVENT.register(NametagRenderer::onHudRender);
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
        if (!cfg.nametagEnabled) {
            return;
        }

        float tickDelta = tick.getTickProgress(false);
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        double camX = ProjectionCapture.getCamX();
        double camY = ProjectionCapture.getCamY();
        double camZ = ProjectionCapture.getCamZ();
        double maxDistSq = (double) cfg.nametagMaxDistance * cfg.nametagMaxDistance;

        for (AbstractClientPlayerEntity entity : mc.world.getPlayers()) {
            if (entity == null) continue;
            // Own player: skip in first person, optional in third person
            if (entity == mc.player) {
                if (!cfg.nametagShowSelf || mc.options.getPerspective().isFirstPerson()) continue;
            }
            // Hide invisible / spectator players (vanilla behavior)
            if (entity.isInvisibleTo(mc.player)) continue;
            if (entity.isSpectator() && !mc.player.isSpectator()) continue;

            // Skip teammates if their custom TeamView hologram is active
            if (!cfg.nametagShowTeammates && cfg.enabled) {
                String lowerName = entity.getName().getString().toLowerCase(java.util.Locale.ROOT);
                if (dev.mahikari.client.MahikariClient.MANAGER.findByName(lowerName) != null) {
                    continue;
                }
            }

            Vec3d pos = entity.getLerpedPos(tickDelta);
            double ed = pos.x - camX;
            double ey = pos.y + (double) entity.getHeight() / 2.0 - camY;
            double ez = pos.z - camZ;
            double distSq = ed * ed + ey * ey + ez * ez;
            if (distSq > maxDistSq) continue;
            double playerDist = Math.sqrt(distSq);

            double tx = pos.x;
            double ty = pos.y + (double) entity.getHeight() + 0.6; // Slightly above head
            double tz = pos.z;

            float[] proj = ProjectionCapture.project(tx, ty, tz, sw, sh);
            boolean inFront = proj[5] > 0.0f;
            if (!inFront) continue;

            // Distance-based scale, clamped so it stays readable
            float distScale = (float) Math.max(0.3, Math.min(1.5, 8.0 / Math.max(1.0, playerDist)));
            float sc = distScale * cfg.nametagScale * ProjectionCapture.getZoomScale() * 1.5f;

            // Use displayName — vanilla already bakes in scoreboard team prefix/suffix and any
            // server-side icons / rank prefixes (e.g. "[VIP] ✦ name"). Fall back to plain name
            // only when displayName is empty (some servers send only a styled name component).
            Text displayName = entity.getDisplayName();
            if (displayName == null || displayName.getString().isEmpty()) {
                displayName = entity.getName();
                if (displayName == null) continue;
            }

            int nameWidth = mc.textRenderer.getWidth(displayName);
            if (nameWidth <= 0) continue;

            Matrix3x2fStack m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(proj[0], proj[1]);
            m.scale(sc, sc);

            // Background
            if (!cfg.nametagNoBackground) {
                int bgColor = cfg.getNametagBackgroundColorARGB();
                if ((bgColor >>> 24) > 0) {
                    ctx.fill(-nameWidth / 2 - 2, -10, nameWidth / 2 + 2, -1, bgColor);
                }
            }

            // Text — drop shadow so it's legible without a background
            ctx.drawText(mc.textRenderer, displayName, -nameWidth / 2, -9, 0xFFFFFFFF, true);

            m.popMatrix();
        }
    }
}
