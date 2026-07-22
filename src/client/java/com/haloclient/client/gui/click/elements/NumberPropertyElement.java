package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.property.NumberProperty;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

public class NumberPropertyElement extends ClickGUIElement {
    private final NumberProperty property;
    private boolean dragging = false;

    public NumberPropertyElement(NumberProperty property) {
        this.property = property;
    }

    @Override
    public void render(
            GuiGraphicsExtractor graphics,
            Matrix3x2f pose,
            float x,
            float y,
            float width,
            int mouseX,
            int mouseY,
            float delta,
            ScreenRectangle scissorArea,
            MsdfFont font
    ) {
        if (font != null) {
            float textX = Math.round(x + 8.0f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    property.getName(),
                    pose,
                    textX, y + 1.0f,
                    7.5f,
                    ARGB.color(200, 220, 220, 220),
                    scissorArea
            ));

            String valStr = String.format("%.1f", property.getValue());
            float valW = font.getWidth(valStr, 7.5f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    valStr,
                    pose,
                    x + width - 8.0f - valW, y + 1.0f,
                    7.5f,
                    ARGB.color(200, 180, 180, 180),
                    scissorArea
            ));
        }

        // Slider bar
        float sliderX = x + 8.0f;
        float sliderY = y + 12.0f;
        float sliderW = width - 16.0f;
        float sliderH = 2.0f;

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                sliderX, sliderY, sliderW, sliderH,
                ARGB.color(100, 60, 60, 60),
                1.0f,
                scissorArea
        ));

        double percent = (property.getValue() - property.getMin()) / (property.getMax() - property.getMin());
        float fillW = (float) (percent * sliderW);
        if (fillW > 0) {
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    sliderX, sliderY, fillW, sliderH,
                    ARGB.color(255, 50, 120, 220),
                    1.0f,
                    scissorArea
            ));
        }

        // Drag handle update
        if (dragging) {
            boolean mouseDown = GLFW.glfwGetMouseButton(
                    Minecraft.getInstance().getWindow().handle(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS;

            if (mouseDown) {
                float pct = (mouseX - sliderX) / sliderW;
                pct = Math.max(0.0f, Math.min(1.0f, pct));
                double val = property.getMin() + pct * (property.getMax() - property.getMin());
                property.setValue(val);
            } else {
                dragging = false;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0) {
                dragging = true;
                float sliderX = x + 8.0f;
                float sliderW = width - 16.0f;
                float pct = (float) ((mouseX - sliderX) / sliderW);
                pct = Math.max(0.0f, Math.min(1.0f, pct));
                double val = property.getMin() + pct * (property.getMax() - property.getMin());
                property.setValue(val);
                return true;
            }
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 20.0f;
    }

    public NumberProperty getProperty() {
        return property;
    }
}
