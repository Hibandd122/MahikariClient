/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.text.Text
 *  net.minecraft.client.gui.widget.SliderWidget
 */
package mahikariui.core.gui.slider;

import java.util.function.Consumer;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.SliderWidget;

public class ConfigSlider
extends SliderWidget {
    private final int min;
    private final int max;
    private final Consumer<Integer> onChange;

    public ConfigSlider(int x, int y, int width, int height, int min, int max, int currentValue, Consumer<Integer> onChange) {
        super(x, y, width, height, net.minecraft.text.Text.literal((String)("Frame: " + currentValue)), (double)((float)(currentValue - min) / (float)(max - min)));
        this.min = min;
        this.max = max;
        this.onChange = onChange;
    }

    protected void updateMessage() {
        this.setMessage(net.minecraft.text.Text.literal((String)("Frame: " + this.getIntValue())));
    }

    protected void applyValue() {
        int value = this.getIntValue();
        this.onChange.accept(value);
    }

    private int getIntValue() {
        return this.min + (int)((double)(this.max - this.min) * this.value);
    }
}

