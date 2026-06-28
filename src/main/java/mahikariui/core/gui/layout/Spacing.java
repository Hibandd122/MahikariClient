/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.core.gui.layout;

public class Spacing {
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    private Spacing(int top, int right, int bottom, int left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public static Spacing all(int value) {
        return new Spacing(value, value, value, value);
    }

    public static Spacing symmetric(int vertical, int horizontal) {
        return new Spacing(vertical, horizontal, vertical, horizontal);
    }

    public static Spacing only(int top, int right, int bottom, int left) {
        return new Spacing(top, right, bottom, left);
    }

    public static Spacing horizontal(int value) {
        return new Spacing(0, value, 0, value);
    }

    public static Spacing vertical(int value) {
        return new Spacing(value, 0, value, 0);
    }

    public static Spacing none() {
        return new Spacing(0, 0, 0, 0);
    }

    public int getTop() {
        return this.top;
    }

    public int getRight() {
        return this.right;
    }

    public int getBottom() {
        return this.bottom;
    }

    public int getLeft() {
        return this.left;
    }

    public int getHorizontal() {
        return this.left + this.right;
    }

    public int getVertical() {
        return this.top + this.bottom;
    }
}

