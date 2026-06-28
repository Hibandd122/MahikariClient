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
import mahikariui.core.gui.layout.LayoutConstraint;
import mahikariui.core.gui.layout.Spacing;

public class LayoutBuilder {
    private final List<WidgetConfig> widgetConfigs = new ArrayList<WidgetConfig>();
    private Spacing containerPadding = Spacing.none();
    private int screenWidth;
    private int screenHeight;

    public WidgetBuilder addWidget(ClickableWidget widget) {
        return new WidgetBuilder(this, widget);
    }

    public LayoutBuilder padding(Spacing padding) {
        this.containerPadding = padding;
        return this;
    }

    public List<ClickableWidget> build(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        ArrayList<ClickableWidget> widgets = new ArrayList<ClickableWidget>();
        for (WidgetConfig config : this.widgetConfigs) {
            this.positionWidget(config);
            widgets.add(config.widget);
        }
        return widgets;
    }

    private void positionWidget(WidgetConfig config) {
        int height;
        int availableWidth = this.screenWidth - this.containerPadding.getHorizontal();
        int availableHeight = this.screenHeight - this.containerPadding.getVertical();
        int width = config.width != null ? config.width.resolve(availableWidth - config.margin.getHorizontal(), config.widget.getWidth(), true) : config.widget.getWidth();
        int n = height = config.height != null ? config.height.resolve(availableHeight - config.margin.getVertical(), config.widget.getHeight(), false) : config.widget.getHeight();
        if (config.width != null || config.height != null) {
            config.widget.setWidth(width);
            config.widget.setHeight(height);
        }
        int x = this.containerPadding.getLeft() + config.margin.getLeft();
        if (config.x != null) {
            int resolvedX = config.x.resolve(availableWidth, width, true);
            x = resolvedX + config.margin.getLeft();
        }
        x = this.applyHorizontalAlignment(x, width, availableWidth, config.horizontalAlign, config.margin);
        int y = this.containerPadding.getTop() + config.margin.getTop();
        if (config.y != null) {
            int resolvedY = config.y.resolve(availableHeight, height, false);
            y = resolvedY + config.margin.getTop();
        }
        y = this.applyVerticalAlignment(y, height, availableHeight, config.verticalAlign, config.margin);
        config.widget.setX(x);
        config.widget.setY(y);
    }

    private int applyHorizontalAlignment(int x, int width, int availableWidth, WidgetConfig.Alignment align, Spacing margin) {
        return switch (align.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> x;
            case 1 -> this.containerPadding.getLeft() + (availableWidth - width) / 2;
            case 2 -> this.screenWidth - this.containerPadding.getRight() - margin.getRight() - width;
        };
    }

    private int applyVerticalAlignment(int y, int height, int availableHeight, WidgetConfig.Alignment align, Spacing margin) {
        return switch (align.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> y;
            case 1 -> this.containerPadding.getTop() + (availableHeight - height) / 2;
            case 2 -> this.screenHeight - this.containerPadding.getBottom() - margin.getBottom() - height;
        };
    }

    public static class WidgetBuilder {
        private final LayoutBuilder parent;
        private final WidgetConfig config;

        WidgetBuilder(LayoutBuilder parent, ClickableWidget widget) {
            this.parent = parent;
            this.config = new WidgetConfig();
            this.config.widget = widget;
        }

        public WidgetBuilder x(LayoutConstraint constraint) {
            this.config.x = constraint;
            return this;
        }

        public WidgetBuilder y(LayoutConstraint constraint) {
            this.config.y = constraint;
            return this;
        }

        public WidgetBuilder width(LayoutConstraint constraint) {
            this.config.width = constraint;
            return this;
        }

        public WidgetBuilder height(LayoutConstraint constraint) {
            this.config.height = constraint;
            return this;
        }

        public WidgetBuilder margin(Spacing margin) {
            this.config.margin = margin;
            return this;
        }

        public WidgetBuilder alignHorizontal(WidgetConfig.Alignment alignment) {
            this.config.horizontalAlign = alignment;
            return this;
        }

        public WidgetBuilder alignVertical(WidgetConfig.Alignment alignment) {
            this.config.verticalAlign = alignment;
            return this;
        }

        public WidgetBuilder center() {
            this.config.horizontalAlign = WidgetConfig.Alignment.CENTER;
            this.config.verticalAlign = WidgetConfig.Alignment.CENTER;
            return this;
        }

        public LayoutBuilder add() {
            this.parent.widgetConfigs.add(this.config);
            return this.parent;
        }
    }

    public static class WidgetConfig {
        ClickableWidget widget;
        LayoutConstraint x;
        LayoutConstraint y;
        LayoutConstraint width;
        LayoutConstraint height;
        Spacing margin = Spacing.none();
        Alignment horizontalAlign = Alignment.START;
        Alignment verticalAlign = Alignment.START;

        public static enum Alignment {
            START,
            CENTER,
            END;

        }
    }
}

