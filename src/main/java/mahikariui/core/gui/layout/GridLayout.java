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

public class GridLayout {
    private final List<GridItem> items = new ArrayList<GridItem>();
    private final int columns;
    private final int rows;
    private final int horizontalSpacing;
    private final int verticalSpacing;
    private final UIAnchor anchor;
    private Spacing padding = Spacing.none();
    private CellSizing cellSizing = CellSizing.UNIFORM;

    public GridLayout(UIAnchor anchor, int columns, int rows, int spacing) {
        this(anchor, columns, rows, spacing, spacing);
    }

    public GridLayout(UIAnchor anchor, int columns, int rows, int horizontalSpacing, int verticalSpacing) {
        this.anchor = anchor;
        this.columns = columns;
        this.rows = rows;
        this.horizontalSpacing = horizontalSpacing;
        this.verticalSpacing = verticalSpacing;
    }

    public GridLayout add(ClickableWidget widget) {
        return this.add(widget, this.items.size() / this.columns, this.items.size() % this.columns);
    }

    public GridLayout add(ClickableWidget widget, int row, int col) {
        return this.add(widget, row, col, 1, 1, Spacing.none());
    }

    public GridLayout add(ClickableWidget widget, int row, int col, int rowSpan, int colSpan, Spacing margin) {
        GridItem item = new GridItem();
        item.widget = widget;
        item.row = row;
        item.col = col;
        item.rowSpan = rowSpan;
        item.colSpan = colSpan;
        item.margin = margin;
        this.items.add(item);
        return this;
    }

    public GridLayout padding(Spacing padding) {
        this.padding = padding;
        return this;
    }

    public GridLayout cellSizing(CellSizing sizing) {
        this.cellSizing = sizing;
        return this;
    }

    public List<ClickableWidget> build(int screenWidth, int screenHeight) {
        if (this.items.isEmpty()) {
            return List.of();
        }
        int availableWidth = screenWidth - this.padding.getHorizontal();
        int availableHeight = screenHeight - this.padding.getVertical();
        int[] columnWidths = this.calculateColumnWidths(availableWidth);
        int[] rowHeights = this.calculateRowHeights(availableHeight);
        int totalWidth = this.sum(columnWidths) + this.horizontalSpacing * (this.columns - 1);
        int totalHeight = this.sum(rowHeights) + this.verticalSpacing * (this.rows - 1);
        int startX = this.anchor.resolveX(screenWidth, totalWidth) + this.padding.getLeft();
        int startY = this.anchor.resolveY(screenHeight, totalHeight) + this.padding.getTop();
        ArrayList<ClickableWidget> widgets = new ArrayList<ClickableWidget>();
        for (GridItem item : this.items) {
            int cellX = startX;
            for (int i = 0; i < item.col; ++i) {
                cellX += columnWidths[i] + this.horizontalSpacing;
            }
            int cellY = startY;
            for (int i = 0; i < item.row; ++i) {
                cellY += rowHeights[i] + this.verticalSpacing;
            }
            int cellWidth = 0;
            for (int i = 0; i < item.colSpan && item.col + i < this.columns; ++i) {
                cellWidth += columnWidths[item.col + i];
                if (i >= item.colSpan - 1) continue;
                cellWidth += this.horizontalSpacing;
            }
            int cellHeight = 0;
            for (int i = 0; i < item.rowSpan && item.row + i < this.rows; ++i) {
                cellHeight += rowHeights[item.row + i];
                if (i >= item.rowSpan - 1) continue;
                cellHeight += this.verticalSpacing;
            }
            int widgetX = cellX + item.margin.getLeft();
            int widgetY = cellY + item.margin.getTop();
            int widgetWidth = cellWidth - item.margin.getHorizontal();
            int widgetHeight = cellHeight - item.margin.getVertical();
            item.widget.setX(widgetX);
            item.widget.setY(widgetY);
            if (this.cellSizing == CellSizing.STRETCH) {
                item.widget.setWidth(widgetWidth);
                item.widget.setHeight(widgetHeight);
            }
            widgets.add(item.widget);
        }
        return widgets;
    }

    private int[] calculateColumnWidths(int availableWidth) {
        int[] widths = new int[this.columns];
        switch (this.cellSizing.ordinal()) {
            case 0: {
                int totalSpacing = this.horizontalSpacing * (this.columns - 1);
                int cellWidth = (availableWidth - totalSpacing) / this.columns;
                for (int i = 0; i < this.columns; ++i) {
                    widths[i] = cellWidth;
                }
                break;
            }
            case 1: {
                for (GridItem item : this.items) {
                    if (item.colSpan != 1) continue;
                    int widgetWidth = item.widget.getWidth() + item.margin.getHorizontal();
                    widths[item.col] = Math.max(widths[item.col], widgetWidth);
                }
                break;
            }
            case 2: {
                int totalSpacing = this.horizontalSpacing * (this.columns - 1);
                int cellWidth = (availableWidth - totalSpacing) / this.columns;
                for (int i = 0; i < this.columns; ++i) {
                    widths[i] = cellWidth;
                }
                break;
            }
        }
        return widths;
    }

    private int[] calculateRowHeights(int availableHeight) {
        int[] heights = new int[this.rows];
        switch (this.cellSizing.ordinal()) {
            case 0: {
                int totalSpacing = this.verticalSpacing * (this.rows - 1);
                int cellHeight = (availableHeight - totalSpacing) / this.rows;
                for (int i = 0; i < this.rows; ++i) {
                    heights[i] = cellHeight;
                }
                break;
            }
            case 1: {
                for (GridItem item : this.items) {
                    if (item.rowSpan != 1) continue;
                    int widgetHeight = item.widget.getHeight() + item.margin.getVertical();
                    heights[item.row] = Math.max(heights[item.row], widgetHeight);
                }
                break;
            }
            case 2: {
                int totalSpacing = this.verticalSpacing * (this.rows - 1);
                int cellHeight = (availableHeight - totalSpacing) / this.rows;
                for (int i = 0; i < this.rows; ++i) {
                    heights[i] = cellHeight;
                }
                break;
            }
        }
        return heights;
    }

    private int sum(int[] array) {
        int total = 0;
        for (int value : array) {
            total += value;
        }
        return total;
    }

    public static enum CellSizing {
        UNIFORM,
        AUTO,
        STRETCH;

    }

    private static class GridItem {
        ClickableWidget widget;
        int row;
        int col;
        int rowSpan = 1;
        int colSpan = 1;
        Spacing margin = Spacing.none();

        private GridItem() {
        }
    }
}

