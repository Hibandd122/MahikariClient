/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.widget.ClickableWidget
 */
package mahikariui.core.gui.layout;

import net.minecraft.client.gui.widget.ClickableWidget;

public class LayoutConstraint {
    private final ConstraintType type;
    private final float value;
    private final ClickableWidget relativeWidget;

    private LayoutConstraint(ConstraintType type, float value, ClickableWidget relativeWidget) {
        this.type = type;
        this.value = value;
        this.relativeWidget = relativeWidget;
    }

    public static LayoutConstraint fixed(int pixels) {
        return new LayoutConstraint(ConstraintType.FIXED, pixels, null);
    }

    public static LayoutConstraint percentage(float percent) {
        return new LayoutConstraint(ConstraintType.PERCENTAGE, percent, null);
    }

    public static LayoutConstraint relativeStart(ClickableWidget widget, int offset) {
        return new LayoutConstraint(ConstraintType.RELATIVE_START, offset, widget);
    }

    public static LayoutConstraint relativeEnd(ClickableWidget widget, int offset) {
        return new LayoutConstraint(ConstraintType.RELATIVE_END, offset, widget);
    }

    public static LayoutConstraint center() {
        return new LayoutConstraint(ConstraintType.CENTER, 0.0f, null);
    }

    public static LayoutConstraint fill() {
        return new LayoutConstraint(ConstraintType.FILL, 0.0f, null);
    }

    public static LayoutConstraint wrapContent() {
        return new LayoutConstraint(ConstraintType.WRAP_CONTENT, 0.0f, null);
    }

    public int resolve(int parentSize, int contentSize, boolean isHorizontal) {
        return switch (this.type.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> (int)this.value;
            case 1 -> (int)((float)parentSize * this.value);
            case 4 -> (parentSize - contentSize) / 2;
            case 5 -> parentSize;
            case 6 -> contentSize;
            case 2 -> {
                if (this.relativeWidget == null) {
                    yield 0;
                }
                yield (isHorizontal ? this.relativeWidget.getX() : this.relativeWidget.getY()) + (int)this.value;
            }
            case 3 -> {
                if (this.relativeWidget == null) {
                    yield 0;
                }
                int widgetEnd = isHorizontal ? this.relativeWidget.getX() + this.relativeWidget.getWidth() : this.relativeWidget.getY() + this.relativeWidget.getHeight();
                yield widgetEnd + (int)this.value;
            }
        };
    }

    public ConstraintType getType() {
        return this.type;
    }

    public float getValue() {
        return this.value;
    }

    public ClickableWidget getRelativeWidget() {
        return this.relativeWidget;
    }

    public static enum ConstraintType {
        FIXED,
        PERCENTAGE,
        RELATIVE_START,
        RELATIVE_END,
        CENTER,
        FILL,
        WRAP_CONTENT;

    }
}

