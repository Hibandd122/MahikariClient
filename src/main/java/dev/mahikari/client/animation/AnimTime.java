package dev.mahikari.client.animation;

/**
 * Global delta-time clock for all HUD animation. Call {@link #tick()} once at
 * the very start of each render frame; everything else can read {@link #delta()}
 * to get the seconds elapsed since the previous frame (clamped to avoid spikes).
 */
public final class AnimTime {
    private static final float MAX_DELTA = 0.1f;
    private static final float DEFAULT_DELTA = 1.0f / 60.0f;

    private static long lastNanos = 0L;
    private static float deltaSec = DEFAULT_DELTA;
    private static int frameId = 0;

    private AnimTime() {}

    public static void tick() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
            deltaSec = DEFAULT_DELTA;
        } else {
            long dn = now - lastNanos;
            lastNanos = now;
            float d = dn / 1_000_000_000.0f;
            if (d < 0.0f) d = 0.0f;
            if (d > MAX_DELTA) d = MAX_DELTA;
            deltaSec = d;
        }
        frameId++;
    }

    public static float delta() {
        return deltaSec;
    }

    public static int frameId() {
        return frameId;
    }

    public static long nowMs() {
        return System.currentTimeMillis();
    }
}
