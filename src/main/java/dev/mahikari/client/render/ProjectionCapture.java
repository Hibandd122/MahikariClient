package dev.mahikari.client.render;

import dev.mahikari.client.mixin.GameRendererAccessor;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ProjectionCapture {
    private static final Matrix4f projMatrix = new Matrix4f();
    private static final Quaternionf invRotation = new Quaternionf();
    private static double camX;
    private static double camY;
    private static double camZ;
    private static float cachedZoomScale;
    private static boolean ready;
    private static final Vector3f reusableVec3;
    private static final Vector4f reusableVec4;

    public static void register() {
        WorldRenderEvents.END_EXTRACTION.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            GameRenderer gr = mc.gameRenderer;
            Camera camera = ctx.camera();
            float tickDelta = ctx.tickCounter().getTickProgress(true);
            float fov = ((GameRendererAccessor)gr).newgenMahikari$getFov(camera, tickDelta, true);
            projMatrix.set(gr.getBasicProjectionMatrix(fov));
            invRotation.set(camera.getRotation()).conjugate();
            Vec3d pos = camera.getCameraPos();
            camX = pos.x;
            camY = pos.y;
            camZ = pos.z;
            float baseFov = ((Integer)mc.options.getFov().getValue()).intValue();
            float baseProjY = 1.0f / (float)Math.tan(Math.toRadians(baseFov) / 2.0);
            float currentProjY = projMatrix.m11();
            cachedZoomScale = baseProjY > 0.0f ? currentProjY / baseProjY : 1.0f;
            ready = true;
        });
    }

    public static boolean isReady() {
        return ready;
    }

    public static double getCamX() {
        return camX;
    }

    public static double getCamY() {
        return camY;
    }

    public static double getCamZ() {
        return camZ;
    }

    public static float getZoomScale() {
        return cachedZoomScale;
    }

    public static float[] project(double wx, double wy, double wz, int sw, int sh) {
        float screenY;
        float screenX;
        boolean inFront;
        float dx = (float)(wx - camX);
        float dy = (float)(wy - camY);
        float dz = (float)(wz - camZ);
        reusableVec3.set(dx, dy, dz);
        invRotation.transform(reusableVec3);
        float vx = ProjectionCapture.reusableVec3.x;
        float vy = ProjectionCapture.reusableVec3.y;
        float vz = ProjectionCapture.reusableVec3.z;
        reusableVec4.set(vx, vy, vz, 1.0f);
        projMatrix.transform(reusableVec4);
        boolean bl = inFront = ProjectionCapture.reusableVec4.w > 0.0f;
        if (inFront) {
            float ndcX = ProjectionCapture.reusableVec4.x / ProjectionCapture.reusableVec4.w;
            float ndcY = ProjectionCapture.reusableVec4.y / ProjectionCapture.reusableVec4.w;
            screenX = (ndcX + 1.0f) / 2.0f * (float)sw;
            screenY = (1.0f - ndcY) / 2.0f * (float)sh;
        } else {
            screenX = Float.NaN;
            screenY = Float.NaN;
        }
        return new float[]{screenX, screenY, vx, vy, vz, inFront ? 1.0f : 0.0f};
    }

    static {
        cachedZoomScale = 1.0f;
        reusableVec3 = new Vector3f();
        reusableVec4 = new Vector4f();
    }
}
