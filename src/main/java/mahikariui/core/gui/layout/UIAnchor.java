/*
 * Decompiled with CFR 0.152.
 */
package mahikariui.core.gui.layout;

public enum UIAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;


    public int resolveX(int screenWidth, int elementWidth) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 1, 4, 7 -> (screenWidth - elementWidth) / 2;
            case 2, 5, 8 -> screenWidth - elementWidth - 8;
            case 0, 3, 6 -> 8;
        };
    }

    public int resolveY(int screenHeight, int elementHeight) {
        return switch (this.ordinal()) {
            default -> throw new MatchException(null, null);
            case 3, 4, 5 -> (screenHeight - elementHeight) / 2;
            case 6, 7, 8 -> screenHeight - elementHeight - 8;
            case 0, 1, 2 -> 8;
        };
    }
}

