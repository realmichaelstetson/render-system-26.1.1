package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.property.ModeProperty;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

public class ModePropertyElement extends ClickGUIElement {
    private final ModeProperty property;

    public ModePropertyElement(ModeProperty property) {
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
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    property.getName(),
                    pose,
                    x + 10.0f, y + (20.0f - font.getHeight(8f)) / 2.0f,
                    8f,
                    ARGB.color(200, 220, 220, 220),
                    scissorArea
            ));

            String valStr = property.getValue();
            float valW = font.getWidth(valStr, 7.5f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    valStr,
                    pose,
                    x + width - 10.0f - valW, y + (20.0f - font.getHeight(7.5f)) / 2.0f,
                    7.5f,
                    ARGB.color(255, 50, 120, 220),
                    scissorArea
            ));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0) {
                property.increment();
                return true;
            } else if (button == 1) {
                property.decrement();
                return true;
            }
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 20.0f;
    }

    public ModeProperty getProperty() {
        return property;
    }
}
