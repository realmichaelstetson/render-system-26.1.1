package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;

public class BooleanPropertyElement extends ClickGUIElement {
    private final BooleanProperty property;
    private final Animation toggleAnimation;

    public BooleanPropertyElement(BooleanProperty property) {
        this.property = property;
        this.toggleAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
        this.toggleAnimation.setValue(property.getValue() ? 1.0f : 0.0f);
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
        toggleAnimation.run(property.getValue() ? 1.0f : 0.0f);
        float progress = toggleAnimation.getValue();

        if (font != null) {
            float textX = Math.round(x + 8.0f);
            float textY = Math.round(y + (20.0f - font.getHeight(8f)) / 2.0f - 1.0f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    property.getName(),
                    pose,
                    textX, textY,
                    8f,
                    ARGB.color(200, 220, 220, 220),
                    scissorArea
            ));
        }

        // Toggle Switch Track (Pill Shape)
        float trackW = 14.0f;
        float trackH = 7.0f;
        float trackX = x + width - 8.0f - trackW;
        float trackY = Math.round(y + (20.0f - trackH) / 2.0f - 1.0f);

        int offCol = ARGB.color(100, 60, 60, 60);
        int onCol = ARGB.color(255, 50, 160, 220);
        int alpha = (int) (ARGB.alpha(offCol) + (ARGB.alpha(onCol) - ARGB.alpha(offCol)) * progress);
        int red = (int) (ARGB.red(offCol) + (ARGB.red(onCol) - ARGB.red(offCol)) * progress);
        int green = (int) (ARGB.green(offCol) + (ARGB.green(onCol) - ARGB.green(offCol)) * progress);
        int blue = (int) (ARGB.blue(offCol) + (ARGB.blue(onCol) - ARGB.blue(offCol)) * progress);
        int trackCol = ARGB.color(alpha, red, green, blue);

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                trackX, trackY, trackW, trackH,
                trackCol,
                trackH / 2.0f,
                scissorArea
        ));

        // Toggle Switch Thumb (Circle)
        float thumbSize = 5.0f;
        float leftX = trackX + 1.0f;
        float rightX = trackX + trackW - thumbSize - 1.0f;
        float thumbX = leftX + (rightX - leftX) * progress;
        float thumbY = trackY + (trackH - thumbSize) / 2.0f;
        int thumbCol = ARGB.color(255, 255, 255, 255);

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                thumbX, thumbY, thumbSize, thumbSize,
                thumbCol,
                thumbSize / 2.0f,
                scissorArea
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0) {
                property.toggle();
                return true;
            }
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 20.0f;
    }

    public BooleanProperty getProperty() {
        return property;
    }
}
