/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.Click
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.widget.ClickableWidget
 *  net.minecraft.client.gui.screen.narration.NarrationMessageBuilder
 *  org.jetbrains.annotations.NotNull
 */
package mahikariui.core.gui.widget;

import java.lang.runtime.SwitchBootstraps;
import java.util.Objects;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import org.jetbrains.annotations.NotNull;
import mahikariui.core.gui.button.ImageButton;
import mahikariui.core.gui.button.TextButton;
import mahikariui.core.gui.button.TextIconButton;
import mahikariui.core.gui.widget.VersionInfoWidget;

public class AnimatedWidget
extends ClickableWidget {
    private final ClickableWidget inner;
    public float opacity = 0.0f;
    private float offsetX = 0.0f;
    private float offsetY = 0.0f;
    private AnimationState state;
    private final Direction direction;
    private boolean cleanModeIgnore = false;
    private static final float OPACITY_THRESHOLD = 0.01f;
    private static final float OFFSET_THRESHOLD = 0.5f;
    private static final float SLIDE_DECAY = 0.85f;
    private static final float SLIDE_OUT_SPEED = 1.5f;
    private static final float INITIAL_OFFSET = 30.0f;
    private float lastOpacity = 0.0f;

    public AnimatedWidget(ClickableWidget inner, Direction direction) {
        super(inner.getX(), inner.getY(), inner.getWidth(), inner.getHeight(), inner.getMessage());
        this.inner = inner;
        this.direction = direction;
        this.offsetX = direction.offsetX;
        this.offsetY = direction.offsetY;
        this.state = AnimationState.ANIMATING_IN;
        this.inner.active = true;
    }

    public AnimatedWidget cleanModeIgnore(boolean ignore) {
        this.cleanModeIgnore = ignore;
        return this;
    }

    public AnimatedWidget cleanModeIgnore() {
        return this.cleanModeIgnore(true);
    }

    public boolean isCleanModeIgnored() {
        return this.cleanModeIgnore;
    }

    protected void renderWidget(@NotNull DrawContext graphics, int mouseX, int mouseY, float delta) {
        boolean hasOffset;
        if (this.opacity <= 0.01f && this.state == AnimationState.COMPLETED_OUT) {
            return;
        }
        this.updateAnimation(delta);
        if (Math.abs(this.opacity - this.lastOpacity) > 0.01f) {
            this.updateInnerOpacity();
            this.lastOpacity = this.opacity;
        }
        if (this.opacity <= 0.01f) {
            return;
        }
        boolean bl = hasOffset = Math.abs(this.offsetX) > 0.5f || Math.abs(this.offsetY) > 0.5f;
        if (hasOffset) {
            graphics.getMatrices().pushMatrix();
            graphics.getMatrices().translate(this.offsetX, this.offsetY);
            this.inner.render(graphics, mouseX, mouseY, delta);
            graphics.getMatrices().popMatrix();
        } else {
            this.inner.render(graphics, mouseX, mouseY, delta);
        }
    }

    private void updateAnimation(float delta) {
        float speed = delta * 0.1f;
        switch (this.state.ordinal()) {
            case 1: {
                this.opacity = Math.min(this.opacity + speed, 1.0f);
                this.offsetX *= 0.85f;
                this.offsetY *= 0.85f;
                if (Math.abs(this.offsetX) < 0.5f) {
                    this.offsetX = 0.0f;
                }
                if (Math.abs(this.offsetY) < 0.5f) {
                    this.offsetY = 0.0f;
                }
                if (!(this.opacity >= 1.0f) || this.offsetX != 0.0f || this.offsetY != 0.0f) break;
                this.state = AnimationState.COMPLETED_IN;
                break;
            }
            case 2: {
                this.opacity = Math.max(this.opacity - speed, 0.0f);
                switch (this.direction.ordinal()) {
                    case 0: {
                        this.offsetX -= 1.5f;
                        break;
                    }
                    case 1: {
                        this.offsetX += 1.5f;
                        break;
                    }
                    case 2: {
                        this.offsetY -= 1.5f;
                        break;
                    }
                    case 3: {
                        this.offsetY += 1.5f;
                    }
                }
                if (!(this.opacity <= 0.01f)) break;
                this.state = AnimationState.COMPLETED_OUT;
                this.opacity = 0.0f;
                break;
            }
        }
    }

    private void updateInnerOpacity() {
        if (this.inner instanceof TextButton) {
            ((TextButton)this.inner).setExternalOpacity(this.opacity);
        } else if (this.inner instanceof ImageButton) {
            ((ImageButton)this.inner).setExternalOpacity(this.opacity);
        } else if (this.inner instanceof TextIconButton) {
            ((TextIconButton)this.inner).setExternalOpacity(this.opacity);
        } else if (this.inner instanceof VersionInfoWidget) {
            ((VersionInfoWidget)this.inner).setExternalOpacity(this.opacity);
        }
    }

    public void slideOut() {
        if (this.cleanModeIgnore) {
            return;
        }
        if (this.state != AnimationState.ANIMATING_OUT && this.state != AnimationState.COMPLETED_OUT) {
            this.state = AnimationState.ANIMATING_OUT;
            this.inner.active = false;
        }
    }

    public void slideOut(boolean bypassIgnore) {
        if (!bypassIgnore && this.cleanModeIgnore) {
            return;
        }
        if (this.state != AnimationState.ANIMATING_OUT && this.state != AnimationState.COMPLETED_OUT) {
            this.state = AnimationState.ANIMATING_OUT;
            this.inner.active = false;
        }
    }

    public void slideIn() {
        if (this.state != AnimationState.ANIMATING_IN) {
            this.state = AnimationState.ANIMATING_IN;
            this.offsetX = this.direction.offsetX;
            this.offsetY = this.direction.offsetY;
            this.opacity = 0.0f;
            this.inner.active = true;
        }
    }

    public boolean isAnimating() {
        return this.state == AnimationState.ANIMATING_IN || this.state == AnimationState.ANIMATING_OUT;
    }

    public boolean isFullyVisible() {
        return this.state == AnimationState.COMPLETED_IN;
    }

    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        if (!this.inner.active || this.opacity <= 0.01f) {
            return false;
        }
        double adjustedX = click.x() - (double)this.offsetX;
        double adjustedY = click.y() - (double)this.offsetY;
        net.minecraft.client.gui.Click adjustedClick = new net.minecraft.client.gui.Click(adjustedX, adjustedY, click.buttonInfo());
        return this.inner.mouseClicked(adjustedClick, bl);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (this.opacity <= 0.01f) {
            return false;
        }
        double adjustedX = mouseX - (double)this.offsetX;
        double adjustedY = mouseY - (double)this.offsetY;
        return this.inner.isMouseOver(adjustedX, adjustedY);
    }

    protected void appendClickableNarrations(@NotNull NarrationMessageBuilder narrationElementOutput) {
        if (this.opacity > 0.01f && this.inner.active) {
            this.inner.appendNarrations(narrationElementOutput);
        }
    }

    public void reset() {
        this.state = AnimationState.IDLE;
        this.opacity = 0.0f;
        this.offsetX = this.direction.offsetX;
        this.offsetY = this.direction.offsetY;
        this.inner.active = false;
    }

    public float getOpacity() {
        return this.opacity;
    }

    public ClickableWidget getInnerWidget() {
        return this.inner;
    }

    public static enum Direction {
        LEFT(-30.0f, 0.0f),
        RIGHT(30.0f, 0.0f),
        TOP(0.0f, -30.0f),
        BOTTOM(0.0f, 30.0f);

        public final float offsetX;
        public final float offsetY;

        private Direction(float offsetX, float offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static enum AnimationState {
        IDLE,
        ANIMATING_IN,
        ANIMATING_OUT,
        COMPLETED_IN,
        COMPLETED_OUT;

    }
}

