package dev.mahikari.client.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.mahikari.client.MahikariClient;
import dev.mahikari.client.TeamViewConfig;
import dev.mahikari.client.TeamViewManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

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
            
            if (cfg.espThroughWalls) {
                GlStateManager._disableDepthTest();
            }
            
            VertexConsumer lines = vertexConsumers.getBuffer(RenderLayers.lines());

            if (cfg.espBoxes) {
                if (cfg.espCornerBoxes) {
                    drawCornerBox(matrix, lines, box, r, g, b, a);
                } else {
                    drawBox(matrix, lines, box, r, g, b, a);
                }
            }

            if (cfg.espTracers) {
                // Draw tracers starting slightly in front of the camera (0, -0.1, -0.2) to target's center
                drawTracer(matrix, lines, 0, -0.1, -0.2, x, y + target.getHeight() / 2, z, r, g, b, a);
            }

            if (cfg.espHealthBar) {
                drawHealthBar(matrix, lines, box, target, r, g, b, a);
            }

            // Force draw lines buffer now to flush it before restoring depth test
            vertexConsumers.draw(RenderLayers.lines());

            if (cfg.espThroughWalls) {
                GlStateManager._enableDepthTest();
            }

            matrices.pop();

            // Render Name and Distance labels
            if (cfg.espNames || cfg.espDistance) {
                renderLabels(matrices, vertexConsumers, target, x, y, z, colorArgb, camera, mc.textRenderer, cfg);
            }
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

    private static void drawCornerBox(Matrix4f matrix, VertexConsumer lines, Box box, float r, float g, float b, float a) {
        float minX = (float) box.minX; float minY = (float) box.minY; float minZ = (float) box.minZ;
        float maxX = (float) box.maxX; float maxY = (float) box.maxY; float maxZ = (float) box.maxZ;
        
        float w = (maxX - minX) * 0.25f;
        float h = (maxY - minY) * 0.25f;
        float d = (maxZ - minZ) * 0.25f;

        drawCorner(matrix, lines, minX, minY, minZ, w, h, d, r, g, b, a);
        drawCorner(matrix, lines, maxX, minY, minZ, -w, h, d, r, g, b, a);
        drawCorner(matrix, lines, minX, maxY, minZ, w, -h, d, r, g, b, a);
        drawCorner(matrix, lines, maxX, maxY, minZ, -w, -h, d, r, g, b, a);
        drawCorner(matrix, lines, minX, minY, maxZ, w, h, -d, r, g, b, a);
        drawCorner(matrix, lines, maxX, minY, maxZ, -w, h, -d, r, g, b, a);
        drawCorner(matrix, lines, minX, maxY, maxZ, w, -h, -d, r, g, b, a);
        drawCorner(matrix, lines, maxX, maxY, maxZ, -w, -h, -d, r, g, b, a);
    }

    private static void drawCorner(Matrix4f matrix, VertexConsumer lines, float x, float y, float z, float dx, float dy, float dz, float r, float g, float b, float a) {
        lines.vertex(matrix, x, y, z).color(r, g, b, a).normal(0, 0, 0);
        lines.vertex(matrix, x + dx, y, z).color(r, g, b, a).normal(0, 0, 0);

        lines.vertex(matrix, x, y, z).color(r, g, b, a).normal(0, 0, 0);
        lines.vertex(matrix, x, y + dy, z).color(r, g, b, a).normal(0, 0, 0);

        lines.vertex(matrix, x, y, z).color(r, g, b, a).normal(0, 0, 0);
        lines.vertex(matrix, x, y, z + dz).color(r, g, b, a).normal(0, 0, 0);
    }

    private static void drawTracer(Matrix4f matrix, VertexConsumer lines, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        lines.vertex(matrix, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(0, 1, 0);
        lines.vertex(matrix, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(0, 1, 0);
    }

    private static void drawHealthBar(Matrix4f matrix, VertexConsumer lines, Box box, AbstractClientPlayerEntity target, float r, float g, float b, float a) {
        float minX = (float) box.minX; float minY = (float) box.minY; float minZ = (float) box.minZ;
        float maxX = (float) box.maxX; float maxY = (float) box.maxY; float maxZ = (float) box.maxZ;

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float hpPercent = Math.max(0.0f, Math.min(1.0f, hp / maxHp));

        float barR = 0f; float barG = 1f; float barB = 0f;
        if (hpPercent <= 0.2f) {
            barR = 1f; barG = 0f; barB = 0f;
        } else if (hpPercent <= 0.5f) {
            barR = 1f; barG = 1f; barB = 0f;
        }

        float barX = minX - 0.05f;
        float barZ = minZ - 0.05f;

        // Gray background
        lines.vertex(matrix, barX, minY, barZ).color(0.2f, 0.2f, 0.2f, 0.8f).normal(0, 1, 0);
        lines.vertex(matrix, barX, maxY, barZ).color(0.2f, 0.2f, 0.2f, 0.8f).normal(0, 1, 0);

        // Active green/red bar
        float activeY = minY + (maxY - minY) * hpPercent;
        lines.vertex(matrix, barX, minY, barZ).color(barR, barG, barB, 1f).normal(0, 1, 0);
        lines.vertex(matrix, barX, activeY, barZ).color(barR, barG, barB, 1f).normal(0, 1, 0);
    }

    private static void renderLabels(MatrixStack matrices, VertexConsumerProvider vertexConsumers, AbstractClientPlayerEntity target, double x, double y, double z, int colorArgb, Camera camera, TextRenderer textRenderer, TeamViewConfig cfg) {
        StringBuilder labelBuilder = new StringBuilder();
        if (cfg.espNames) {
            labelBuilder.append(target.getName().getString());
        }
        if (cfg.espDistance) {
            double dist = Math.sqrt(x * x + y * y + z * z);
            if (labelBuilder.length() > 0) labelBuilder.append(" ");
            labelBuilder.append(String.format("[%.1fm]", dist));
        }

        Text text = Text.literal(labelBuilder.toString());
        
        matrices.push();
        matrices.translate(x, y + target.getHeight() + 0.3f, z);
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float bgOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25f);
        int bgColor = (int)(bgOpacity * 255.0f) << 24;
        
        float textX = (float)(-textRenderer.getWidth(text) / 2);
        
        TextRenderer.TextLayerType layerType = cfg.espThroughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;

        textRenderer.draw(
            text,
            textX,
            0f,
            colorArgb,
            true,
            matrix4f,
            vertexConsumers,
            layerType,
            bgColor,
            0xF000F0
        );

        matrices.pop();
    }
}
