package dev.mahikari.client.animation;

/**
 * Stateless easing & damping helpers.
 * All methods are allocation-free and FPS-independent where dt is taken.
 */
public final class Easings {
    private Easings() {}

    public static float clamp01(float t) {
        return t < 0.0f ? 0.0f : (t > 1.0f ? 1.0f : t);
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float linear(float t) {
        return t;
    }

    public static float smoothstep(float t) {
        t = clamp01(t);
        return t * t * (3.0f - 2.0f * t);
    }

    public static float easeOutCubic(float t) {
        float u = 1.0f - clamp01(t);
        return 1.0f - u * u * u;
    }

    public static float easeOutQuart(float t) {
        float u = 1.0f - clamp01(t);
        return 1.0f - u * u * u * u;
    }

    public static float easeOutExpo(float t) {
        t = clamp01(t);
        return t >= 1.0f ? 1.0f : 1.0f - (float) Math.pow(2.0, -10.0 * t);
    }

    public static float easeInOutCubic(float t) {
        t = clamp01(t);
        if (t < 0.5f) return 4.0f * t * t * t;
        float u = -2.0f * t + 2.0f;
        return 1.0f - u * u * u * 0.5f;
    }

    public static float easeOutBack(float t) {
        final float c1 = 1.70158f;
        final float c3 = c1 + 1.0f;
        float u = clamp01(t) - 1.0f;
        return 1.0f + c3 * u * u * u + c1 * u * u;
    }

    /**
     * Critically-damped step toward target. halfLife is the time in seconds
     * for half the remaining gap to close â€” independent of frame rate.
     */
    public static float damp(float current, float target, float halfLife, float dt) {
        if (halfLife <= 0.0f || dt <= 0.0f) return target;
        float decay = (float) Math.pow(0.5, dt / halfLife);
        return target + (current - target) * decay;
    }

    /**
     * Exponential smoothing with explicit time constant (seconds).
     * Equivalent to: current + (target - current) * (1 - e^(-dt / timeConstant)).
     */
    public static float expSmooth(float current, float target, float timeConstant, float dt) {
        if (timeConstant <= 0.0f || dt <= 0.0f) return target;
        float factor = 1.0f - (float) Math.exp(-dt / timeConstant);
        return current + (target - current) * factor;
    }
}
