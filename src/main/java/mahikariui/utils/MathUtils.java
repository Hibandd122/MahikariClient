/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.utils;

public class MathUtils {
    public static boolean isWithinValue(double value, double min, double max, boolean contains_min, boolean contains_max, boolean check_sanity) {
        if (check_sanity && min > max) {
            double temp = min;
            min = max;
            max = temp;
        }
        if (contains_min && contains_max) {
            return value >= min && value <= max;
        }
        if (contains_min) {
            return value >= min && value < max;
        }
        if (contains_max) {
            return value > min && value <= max;
        }
        return value > min && value < max;
    }

    public static boolean isWithinValue(double value, double min, double max, boolean contains_min, boolean contains_max) {
        return MathUtils.isWithinValue(value, min, max, contains_min, contains_max, true);
    }

    public static boolean isWithinValue(double value, double min, double max) {
        return MathUtils.isWithinValue(value, min, max, false, false);
    }

    public static double roundDouble(double value, int places) {
        if (places < 0) {
            return value;
        }
        double x = Math.pow(10.0, places);
        return (double)Math.round(value * x) / x;
    }

    public static int clamp(int num, int min, int max) {
        return Math.min(Math.max(num, min), max);
    }

    public static long clamp(long num, long min, long max) {
        return Math.min(Math.max(num, min), max);
    }

    public static float clamp(float num, float min, float max) {
        return Math.min(Math.max(num, min), max);
    }

    public static double clamp(double num, double min, double max) {
        return Math.min(Math.max(num, min), max);
    }

    public static float lerp(float percentage, float min, float max) {
        return min + percentage * (max - min);
    }

    public static double lerp(double percentage, double min, double max) {
        return min + percentage * (max - min);
    }

    public static float normalizeValue(float num, float valueStep, float min, float max) {
        return MathUtils.clamp((MathUtils.snapToStepClamp(num, valueStep, min, max) - min) / (max - min), 0.0f, 1.0f);
    }

    public static float denormalizeValue(float num, float valueStep, float min, float max) {
        return MathUtils.snapToStepClamp(min + (max - min) * MathUtils.clamp(num, 0.0f, 1.0f), valueStep, min, max);
    }

    public static float snapToStepClamp(float num, float valueStep, float min, float max) {
        float value = MathUtils.snapToStep(num, valueStep);
        return MathUtils.clamp(value, min, max);
    }

    public static float snapToStep(float num, float valueStep) {
        float value = num;
        if (valueStep > 0.0f) {
            value = valueStep * (float)Math.round(value / valueStep);
        }
        return value;
    }
}

