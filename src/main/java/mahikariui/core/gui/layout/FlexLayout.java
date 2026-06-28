/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.widget.ClickableWidget
 */
package mahikariui.core.gui.layout;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.widget.ClickableWidget;
import mahikariui.core.gui.layout.Spacing;
import mahikariui.core.gui.layout.UIAnchor;

public class FlexLayout {
    private final List<FlexItem> items = new ArrayList<FlexItem>();
    private final Direction direction;
    private final Alignment mainAxisAlignment;
    private final Alignment crossAxisAlignment;
    private final int spacing;
    private final UIAnchor anchor;
    private Spacing padding = Spacing.none();

    public FlexLayout(Direction direction, UIAnchor anchor, int spacing) {
        this.direction = direction;
        this.anchor = anchor;
        this.spacing = spacing;
        this.mainAxisAlignment = Alignment.START;
        this.crossAxisAlignment = Alignment.START;
    }

    public FlexLayout(Direction direction, UIAnchor anchor, int spacing, Alignment mainAxis, Alignment crossAxis) {
        this.direction = direction;
        this.anchor = anchor;
        this.spacing = spacing;
        this.mainAxisAlignment = mainAxis;
        this.crossAxisAlignment = crossAxis;
    }

    public FlexLayout add(ClickableWidget widget) {
        return this.add(widget, 0, Spacing.none());
    }

    public FlexLayout add(ClickableWidget widget, int flexGrow) {
        return this.add(widget, flexGrow, Spacing.none());
    }

    public FlexLayout add(ClickableWidget widget, int flexGrow, Spacing margin) {
        FlexItem item = new FlexItem();
        item.widget = widget;
        item.flexGrow = flexGrow;
        item.margin = margin;
        this.items.add(item);
        return this;
    }

    public FlexLayout padding(Spacing padding) {
        this.padding = padding;
        return this;
    }

    public FlexLayout wrap(boolean wrap) {
        return this;
    }

    public List<ClickableWidget> build(int screenWidth, int screenHeight) {
        int startY;
        int startX;
        if (this.items.isEmpty()) {
            return List.of();
        }
        boolean isHorizontal = this.direction == Direction.ROW || this.direction == Direction.ROW_REVERSE;
        int availableWidth = screenWidth - this.padding.getHorizontal();
        int availableHeight = screenHeight - this.padding.getVertical();
        int totalFixedSize = 0;
        int totalFlexGrow = 0;
        int maxCrossSize = 0;
        for (FlexItem item : this.items) {
            if (isHorizontal) {
                totalFixedSize += item.widget.getWidth() + item.margin.getHorizontal();
                maxCrossSize = Math.max(maxCrossSize, item.widget.getHeight() + item.margin.getVertical());
            } else {
                totalFixedSize += item.widget.getHeight() + item.margin.getVertical();
                maxCrossSize = Math.max(maxCrossSize, item.widget.getWidth() + item.margin.getHorizontal());
            }
            totalFlexGrow += item.flexGrow;
        }
        int totalSpacing = this.spacing * (this.items.size() - 1);
        int availableMainSize = isHorizontal ? availableWidth : availableHeight;
        int flexSpace = Math.max(0, availableMainSize - (totalFixedSize += totalSpacing));
        int flexUnit = totalFlexGrow > 0 ? flexSpace / totalFlexGrow : 0;
        int totalSize = totalFixedSize + flexUnit * totalFlexGrow;
        if (isHorizontal) {
            startX = this.anchor.resolveX(screenWidth, totalSize) + this.padding.getLeft();
            startY = this.anchor.resolveY(screenHeight, maxCrossSize) + this.padding.getTop();
        } else {
            startX = this.anchor.resolveX(screenWidth, maxCrossSize) + this.padding.getLeft();
            startY = this.anchor.resolveY(screenHeight, totalSize) + this.padding.getTop();
        }
        startX = this.applyMainAxisAlignment(startX, availableWidth, totalSize, isHorizontal);
        startY = this.applyMainAxisAlignment(startY, availableHeight, totalSize, !isHorizontal);
        int currentX = startX;
        int currentY = startY;
        ArrayList<ClickableWidget> widgets = new ArrayList<ClickableWidget>();
        for (FlexItem item : this.items) {
            int extraSize = item.flexGrow * flexUnit;
            if (isHorizontal) {
                int widgetWidth = item.widget.getWidth() + extraSize;
                item.widget.setWidth(widgetWidth);
                item.widget.setX(currentX + item.margin.getLeft());
                int widgetY = this.applyCrossAxisAlignment(currentY, availableHeight, item.widget.getHeight(), item.margin, false);
                item.widget.setY(widgetY);
                currentX += widgetWidth + item.margin.getHorizontal() + this.spacing;
            } else {
                int widgetHeight = item.widget.getHeight() + extraSize;
                item.widget.setHeight(widgetHeight);
                item.widget.setY(currentY + item.margin.getTop());
                int widgetX = this.applyCrossAxisAlignment(currentX, availableWidth, item.widget.getWidth(), item.margin, true);
                item.widget.setX(widgetX);
                currentY += widgetHeight + item.margin.getVertical() + this.spacing;
            }
            widgets.add(item.widget);
        }
        if (this.direction == Direction.ROW_REVERSE || this.direction == Direction.COLUMN_REVERSE) {
            this.reversePositions(widgets, isHorizontal, startX, startY, totalSize);
        }
        return widgets;
    }

    private int applyMainAxisAlignment(int start, int available, int total, boolean apply) {
        if (!apply) {
            return start;
        }
        return switch (this.mainAxisAlignment.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> start;
            case 1 -> start + (available - total) / 2;
            case 2 -> start + (available - total);
            case 3, 4, 5 -> start;
        };
    }

    private int applyCrossAxisAlignment(int start, int available, int size, Spacing margin, boolean isHorizontal) {
        int marginSize = isHorizontal ? margin.getHorizontal() : margin.getVertical();
        int marginStart = isHorizontal ? margin.getLeft() : margin.getTop();
        return switch (this.crossAxisAlignment.ordinal()) {
            case 1 -> start + (available - size - marginSize) / 2 + marginStart;
            case 2 -> start + available - size - (isHorizontal ? margin.getRight() : margin.getBottom());
            default -> start + marginStart;
        };
    }

    private void reversePositions(List<ClickableWidget> widgets, boolean isHorizontal, int start, int startY, int totalSize) {
        for (ClickableWidget widget : widgets) {
            if (isHorizontal) {
                int currentX = widget.getX();
                int newX = start + (totalSize - (currentX - start) - widget.getWidth());
                widget.setX(newX);
                continue;
            }
            int currentY = widget.getY();
            int newY = startY + (totalSize - (currentY - startY) - widget.getHeight());
            widget.setY(newY);
        }
    }

    public static enum Direction {
        ROW,
        COLUMN,
        ROW_REVERSE,
        COLUMN_REVERSE;

    }

    public static enum Alignment {
        START,
        CENTER,
        END,
        SPACE_BETWEEN,
        SPACE_AROUND,
        SPACE_EVENLY;

    }

    private static class FlexItem {
        ClickableWidget widget;
        int flexGrow = 0;
        int flexShrink = 1;
        Integer flexBasis = null;
        Spacing margin = Spacing.none();

        private FlexItem() {
        }
    }
}

