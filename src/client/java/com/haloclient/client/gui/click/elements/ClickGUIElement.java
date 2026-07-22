package com.haloclient.client.gui.click.elements;

import com.haloclient.client.render.font.MsdfFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2f;

public abstract class ClickGUIElement {
    public abstract void render(
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
    );

    public abstract boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button,
            float x,
            float y,
            float width
    );

    public abstract float getHeight();
}
