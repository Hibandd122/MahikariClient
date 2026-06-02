package dev.mahikari.client.animation;

/**
 * Mutable float that smoothly chases a target value with critically-damped
 * spring smoothing. Intended for things like tab indicators, slide offsets,
 * HUD positions that the user should never see snap.
 *
 * No per-frame allocations.
 */
public final class AnimatedFloat {
    private float current;
    private float target;
    private float halfLife;

    public AnimatedFloat(float initial, float halfLife) {
        this.current = initial;
        this.target = initial;
        this.halfLife = halfLife;
    }

    public void setTarget(float v) {
        this.target = v;
    }

    public void snap(float v) {
        this.current = v;
        this.target = v;
    }

    public void setHalfLife(float halfLife) {
        this.halfLife = halfLife;
    }

    public float get() {
        return current;
    }

    public float target() {
        return target;
    }

    /** Advance toward target by dt seconds, returning the new current value. */
    public float update(float dt) {
        current = Easings.damp(current, target, halfLife, dt);
        return current;
    }

    /** Convenience: read the global delta clock and step. */
    public float tick() {
        return update(AnimTime.delta());
    }
}
