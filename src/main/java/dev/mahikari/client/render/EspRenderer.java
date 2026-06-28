package dev.mahikari.client.render;

import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;

public class EspRenderer {

    public static void register() {
        WorldRenderEvents.END_MAIN.register(EspRenderer::onWorldRender);
    }

    private static void onWorldRender(WorldRenderContext context) {
        TeamViewConfig cfg = TeamViewConfig.get();
        if (!cfg.enabled || !cfg.espEnabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        MatrixStack matrices = context.matrices();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        
        VertexConsumerProvider.Immediate vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

        for (AbstractClientPlayerEntity target : mc.world.getPlayers()) {
            if (target == mc.player) continue;

            String nameKey = TeamViewManager.getNameKey(target.getName().getString());
            TeamViewManager.TeammateData data = MahikariClient.MANAGER.findByName(nameKey);
            
            // Only ESP teammates if PARTY_ONLY is set
            if ("PARTY_ONLY".equals(cfg.viewMode) && (data == null || data.getRole().isEmpty())) {
                continue;
            }

            int colorArgb = 0xFFFFFFFF; // Default white
            if (data != null) {
                if ("king".equals(data.getRole())) colorArgb = 0xFFFFCC44;
                else if ("party".equals(data.getRole())) colorArgb = 0xFF44CCFF;
                else if ("vip".equals(data.getRole())) colorArgb = 0xFFFF5CDC;
                else if (data.getApolloColor() != null) colorArgb = 0xFF000000 | data.getApolloColor().getRGB();
                else colorArgb = cfg.getArrowColorARGB();
            }

            float r = ((colorArgb >> 16) & 0xFF) / 255f;
            float g = ((colorArgb >> 8) & 0xFF) / 255f;
            float b = (colorArgb & 0xFF) / 255f;
            float a = 1.0f;

            double x = net.minecraft.util.math.MathHelper.lerp(tickDelta, target.lastRenderX, target.getX()) - camPos.x;
            double y = net.minecraft.util.math.MathHelper.lerp(tickDelta, target.lastRenderY, target.getY()) - camPos.y;
            double z = net.minecraft.util.math.MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ()) - camPos.z;

            Box box = target.getBoundingBox().offset(-target.getX(), -target.getY(), -target.getZ()).offset(x, y, z);

            matrices.push();
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            VertexConsumer lines = vertexConsumers.getBuffer(RenderLayers.lines());

            if (cfg.espBoxes) {
                drawBox(matrix, lines, box, r, g, b, a);
            }

            if (cfg.espTracers) {
                Vec3d forward = new Vec3d(0, 0, 1).rotateX(-camera.getPitch() * 0.017453292F).rotateY(-camera.getYaw() * 0.017453292F);
                drawTracer(matrix, lines, forward.x, forward.y, forward.z, x, y + target.getHeight() / 2, z, r, g, b, a);
            }

            matrices.pop();
        }
        
        // Force draw
        vertexConsumers.draw();
    }

    private static void drawBox(Matrix4f matrix, VertexConsumer lines, Box box, float r, float g, float b, float a) {
        float minX = (float) box.minX; float minY = (float) box.minY; float minZ = (float) box.minZ;
        float maxX = (float) box.maxX; float maxY = (float) box.maxY; float maxZ = (float) box.maxZ;

        // Bottom
        lines.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        lines.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(1, 0, 0);
        lines.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 0, 1);
        lines.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        lines.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        lines.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        lines.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 0, -1);
        lines.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 0, -1);

        // Top
        lines.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);
        lines.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(1, 0, 0);
        lines.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 0, 1);
        lines.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 0, 1);
        lines.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        lines.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(-1, 0, 0);
        lines.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 0, -1);
        lines.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 0, -1);

        // Pillars
        lines.vertex(matrix, minX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, minX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, maxX, minY, minZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, maxX, maxY, minZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, maxX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, maxX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, minX, minY, maxZ).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, minX, maxY, maxZ).color(r, g, b, a).normal(0, 1, 0);
    }

    private static void drawTracer(Matrix4f matrix, VertexConsumer lines, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        lines.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(0, 1, 0);
    }
}
