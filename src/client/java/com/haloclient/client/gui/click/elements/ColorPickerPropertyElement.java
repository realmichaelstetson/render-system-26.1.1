package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.property.ColorPickerProperty;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

public class ColorPickerPropertyElement extends ClickGUIElement {
    private final ColorPickerProperty property;
    private int activeTarget = 0; // 0 = closed, 1 = editing color1, 2 = editing color2
    private int lastTarget = 1; // stores last active target for smooth collapse rendering
    private int dragMode = 0; // 0 = none, 1 = color map 2d, 2 = hue bar, 3 = alpha bar
    private final Animation expandAnim;

    // HSB & Alpha state for target 1 and target 2
    private float hue1 = 0.12f, sat1 = 0.95f, bri1 = 1.0f;
    private int alpha1 = 255;

    private float hue2 = 0.85f, sat2 = 0.85f, bri2 = 1.0f;
    private int alpha2 = 255;

    public ColorPickerPropertyElement(ColorPickerProperty property) {
        this.property = property;
        this.expandAnim = new Animation(Easing.EASE_OUT_QUART, 220L);
        syncHsbFromProperty();
    }

    private void syncHsbFromProperty() {
        int c1 = property.getColor1();
        alpha1 = ARGB.alpha(c1);
        float[] h1 = Color.RGBtoHSB(ARGB.red(c1), ARGB.green(c1), ARGB.blue(c1), null);
        hue1 = h1[0]; sat1 = h1[1]; bri1 = h1[2];

        int c2 = property.getColor2();
        alpha2 = ARGB.alpha(c2);
        float[] h2 = Color.RGBtoHSB(ARGB.red(c2), ARGB.green(c2), ARGB.blue(c2), null);
        hue2 = h2[0]; sat2 = h2[1]; bri2 = h2[2];
    }

    public ColorPickerProperty getProperty() {
        return property;
    }

    public boolean isExpanded() {
        return activeTarget > 0 || expandAnim.getValue() > 0.001f;
    }

    public void closeInstant() {
        this.activeTarget = 0;
        this.dragMode = 0;
        this.expandAnim.setValue(0.0f);
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
        if (activeTarget > 0) {
            lastTarget = activeTarget;
        }

        expandAnim.run(activeTarget > 0 ? 1.0f : 0.0f);
        float progress = expandAnim.getValue();

        float popupY = y + 22.0f;
        float popupW = width - 12.0f;
        float popupH = 78.0f * progress;
        float popupX = x + 6.0f;

        float mapX = popupX + 6.0f;
        float mapY = popupY + 6.0f;
        float mapW = popupW - 24.0f;
        float mapH = 54.0f;

        float hueX = mapX + mapW + 4.0f;
        float hueY = mapY;
        float hueW = 8.0f;
        float hueH = mapH;

        float alphaX = mapX;
        float alphaY = mapY + mapH + 6.0f;
        float alphaW = popupW - 12.0f;
        float alphaH = 6.0f;

        // Handle Active Mouse Dragging
        if (activeTarget > 0 && dragMode > 0) {
            boolean mouseDown = GLFW.glfwGetMouseButton(
                    Minecraft.getInstance().getWindow().handle(),
                    GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == GLFW.GLFW_PRESS;

            if (mouseDown) {
                if (dragMode == 1) {
                    float valSat = (float) Math.max(0.0f, Math.min(1.0f, (mouseX - mapX) / mapW));
                    float valBri = (float) Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - mapY) / mapH));
                    if (activeTarget == 2) { sat2 = valSat; bri2 = valBri; } else { sat1 = valSat; bri1 = valBri; }
                    applyHsbToProperty();
                } else if (dragMode == 2) {
                    float valHue = (float) Math.max(0.0f, Math.min(1.0f, (mouseY - hueY) / hueH));
                    if (activeTarget == 2) { hue2 = valHue; } else { hue1 = valHue; }
                    applyHsbToProperty();
                } else if (dragMode == 3) {
                    int valAlpha = (int) (Math.max(0.0f, Math.min(1.0f, (mouseX - alphaX) / alphaW)) * 255.0f);
                    if (activeTarget == 2) { alpha2 = valAlpha; } else { alpha1 = valAlpha; }
                    applyHsbToProperty();
                }
            } else {
                dragMode = 0;
            }
        }

        // 1. Label text (Property Name)
        if (font != null) {
            float labelY = Math.round(y + (20.0f - font.getHeight(7.5f)) / 2.0f - 1.0f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    property.getName(),
                    pose,
                    x + 8.0f, labelY,
                    7.5f,
                    ARGB.color(200, 220, 220, 220),
                    scissorArea
            ));
        }

        // 2. Color Swatches on Right
        float boxW = 14.0f;
        float boxH = 12.0f;
        float boxY = Math.round(y + (20.0f - boxH) / 2.0f);

        if (property.isDual()) {
            float rect2X = x + width - 8.0f - boxW;
            float rect1X = rect2X - boxW - 4.0f;

            // Rect 1
            renderSwatch(graphics, pose, rect1X, boxY, boxW, boxH, property.getColor1(), activeTarget == 1, scissorArea);
            // Rect 2
            renderSwatch(graphics, pose, rect2X, boxY, boxW, boxH, property.getColor2(), activeTarget == 2, scissorArea);
        } else {
            float rect1X = x + width - 8.0f - boxW;
            renderSwatch(graphics, pose, rect1X, boxY, boxW, boxH, property.getColor1(), activeTarget == 1, scissorArea);
        }

        // 3. Render Popup if expanding
        if (progress > 0.01f) {
            float contentAlpha = Math.max(0.0f, Math.min(1.0f, (progress - 0.05f) / 0.95f));

           // CaptureManager.prepareBlurLayer(graphics);

            // Popup Background
            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_BLUR,
                    CaptureManager.getCaptureTextureSetup(),
                    pose,
                    popupX, popupY, popupW, popupH,
                    ARGB.color((int) (235 * progress), 20, 22, 28),
                    5.0f, 10.0f, 0.0f, scissorArea
            ));

            if (contentAlpha > 0.01f) {
                int targetToRender = (activeTarget > 0) ? activeTarget : lastTarget;
                float curHue = (targetToRender == 2) ? hue2 : hue1;
                float curSat = (targetToRender == 2) ? sat2 : sat1;
                float curBri = (targetToRender == 2) ? bri2 : bri1;
                int curAlpha = (targetToRender == 2) ? alpha2 : alpha1;
                int curColor = (targetToRender == 2) ? property.getColor2() : property.getColor1();

                int pureHueRGB = Color.HSBtoRGB(curHue, 1.0f, 1.0f);

                // A) Base Pure Hue Box
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        mapX, mapY, mapW, mapH,
                        multiplyAlpha(pureHueRGB, contentAlpha), 3.0f, scissorArea
                ));

                // Saturation Overlay (White Left -> Transparent Right, Horizontal Gradient)
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        mapX, mapY, mapW, mapH,
                        ARGB.color((int) (255 * contentAlpha), 255, 255, 255), ARGB.color(0, 255, 255, 255),
                        3.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        scissorArea
                ));

                // Brightness Overlay (Transparent Top -> Black Bottom, Vertical Gradient)
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        mapX, mapY, mapW, mapH,
                        ARGB.color(0, 0, 0, 0), ARGB.color((int) (255 * contentAlpha), 0, 0, 0),
                        3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                        scissorArea
                ));

                // Draggable Picker Ring Handle
                float dotX = mapX + curSat * mapW;
                float dotY = mapY + (1.0f - curBri) * mapH;

                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        dotX - 4.5f, dotY - 4.5f, 9.0f, 9.0f,
                        ARGB.color((int) (255 * contentAlpha), 255, 255, 255), 4.5f, scissorArea
                ));
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        dotX - 3.0f, dotY - 3.0f, 6.0f, 6.0f,
                        multiplyAlpha(curColor, contentAlpha), 3.0f, scissorArea
                ));

                // B) Vertical Rainbow HUE Bar Slider (6 Gradient Segments)
                int[] rainbowColors = new int[]{
                        ARGB.color(255, 255, 0, 0),     // Red
                        ARGB.color(255, 255, 255, 0),   // Yellow
                        ARGB.color(255, 0, 255, 0),     // Green
                        ARGB.color(255, 0, 255, 255),   // Cyan
                        ARGB.color(255, 0, 0, 255),     // Blue
                        ARGB.color(255, 255, 0, 255),   // Magenta
                        ARGB.color(255, 255, 0, 0)      // Red
                };

                float segH = hueH / 6.0f;
                for (int i = 0; i < 6; i++) {
                    float segY = hueY + i * segH;
                    float cornerMask = 0.0f;
                    float rad = 0.0f;
                    if (i == 0) {
                        cornerMask = 4.0f;
                        rad = 3.0f;
                    } else if (i == 5) {
                        cornerMask = 3.0f;
                        rad = 3.0f;
                    }

                    graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                            HaloRenderPipelines.ROUNDED_RECT, pose,
                            hueX, segY, hueW, segH + (i < 5 ? 0.5f : 0.0f),
                            multiplyAlpha(rainbowColors[i], contentAlpha), multiplyAlpha(rainbowColors[i + 1], contentAlpha),
                            rad, 0.0f, cornerMask, scissorArea
                    ));
                }

                // Hue Slider White Line Thumb
                float thumbY = hueY + curHue * hueH;
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        hueX - 1.0f, thumbY - 1.0f, hueW + 2.0f, 2.0f,
                        ARGB.color((int) (255 * contentAlpha), 255, 255, 255), 1.0f, scissorArea
                ));

                // C) Horizontal OPACITY Bar Slider
                int solidRgb = ARGB.color((int) (255 * contentAlpha), ARGB.red(curColor), ARGB.green(curColor), ARGB.blue(curColor));
                int transRgb = ARGB.color(0, ARGB.red(curColor), ARGB.green(curColor), ARGB.blue(curColor));

                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        alphaX, alphaY, alphaW, alphaH,
                        transRgb, solidRgb,
                        3.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                        scissorArea
                ));
                // Vertical Line Thumb
                float alphaThumbX = alphaX + (curAlpha / 255.0f) * alphaW;
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT, pose,
                        alphaThumbX - 1.0f, alphaY - 1.0f, 2.0f, alphaH + 2.0f,
                        ARGB.color((int) (255 * contentAlpha), 255, 255, 255), 1.0f, scissorArea
                ));
            }
        }
    }

    private int multiplyAlpha(int color, float alphaFactor) {
        int a = (int) (ARGB.alpha(color) * alphaFactor);
        return ARGB.color(a, ARGB.red(color), ARGB.green(color), ARGB.blue(color));
    }

    private void renderSwatch(
            GuiGraphicsExtractor graphics,
            Matrix3x2f pose,
            float sx, float sy, float sw, float sh,
            int color, boolean active,
            ScreenRectangle scissorArea
    ) {
        int borderColor = active ? ARGB.color(255, 255, 255, 255) : ARGB.color(150, 70, 75, 85);
        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                sx - 1.0f, sy - 1.0f, sw + 2.0f, sh + 2.0f,
                borderColor, 3.0f, scissorArea
        ));

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                sx, sy, sw, sh,
                color, 2.0f, scissorArea
        ));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        float boxW = 14.0f;
        float boxH = 12.0f;
        float boxY = Math.round(y + (20.0f - boxH) / 2.0f);

        if (property.isDual()) {
            float rect2X = x + width - 8.0f - boxW;
            float rect1X = rect2X - boxW - 4.0f;

            // Click Rect 1
            if (mouseX >= rect1X && mouseX <= rect1X + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                activeTarget = (activeTarget == 1) ? 0 : 1;
                dragMode = 0;
                return true;
            }
            // Click Rect 2
            if (mouseX >= rect2X && mouseX <= rect2X + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                activeTarget = (activeTarget == 2) ? 0 : 2;
                dragMode = 0;
                return true;
            }
        } else {
            float rect1X = x + width - 8.0f - boxW;
            if (mouseX >= rect1X && mouseX <= rect1X + boxW && mouseY >= boxY && mouseY <= boxY + boxH) {
                activeTarget = (activeTarget == 1) ? 0 : 1;
                dragMode = 0;
                return true;
            }
        }

        // Click inside active popup
        if (activeTarget > 0) {
            float popupY = y + 22.0f;
            float popupW = width - 12.0f;
            float popupX = x + 6.0f;

            float mapX = popupX + 6.0f;
            float mapY = popupY + 6.0f;
            float mapW = popupW - 24.0f;
            float mapH = 54.0f;

            float hueX = mapX + mapW + 4.0f;
            float hueY = mapY;
            float hueW = 8.0f;
            float hueH = mapH;

            float alphaX = mapX;
            float alphaY = mapY + mapH + 6.0f;
            float alphaW = popupW - 12.0f;
            float alphaH = 6.0f;

            // 1. Color Map / 2D Sat-Val Canvas Box
            if (mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
                dragMode = 1;
                float valSat = (float) Math.max(0.0f, Math.min(1.0f, (mouseX - mapX) / mapW));
                float valBri = (float) Math.max(0.0f, Math.min(1.0f, 1.0f - (mouseY - mapY) / mapH));

                if (activeTarget == 2) {
                    sat2 = valSat; bri2 = valBri;
                } else {
                    sat1 = valSat; bri1 = valBri;
                }
                applyHsbToProperty();
                return true;
            }

            // 2. Vertical HUE Bar Slider
            if (mouseX >= hueX - 2.0f && mouseX <= hueX + hueW + 2.0f && mouseY >= hueY - 2.0f && mouseY <= hueY + hueH + 2.0f) {
                dragMode = 2;
                float valHue = (float) Math.max(0.0f, Math.min(1.0f, (mouseY - hueY) / hueH));
                if (activeTarget == 2) {
                    hue2 = valHue;
                } else {
                    hue1 = valHue;
                }
                applyHsbToProperty();
                return true;
            }

            // 3. Horizontal OPACITY Bar Slider
            if (mouseX >= alphaX - 2.0f && mouseX <= alphaX + alphaW + 2.0f && mouseY >= alphaY - 3.0f && mouseY <= alphaY + alphaH + 3.0f) {
                dragMode = 3;
                int valAlpha = (int) (Math.max(0.0f, Math.min(1.0f, (mouseX - alphaX) / alphaW)) * 255.0f);
                if (activeTarget == 2) {
                    alpha2 = valAlpha;
                } else {
                    alpha1 = valAlpha;
                }
                applyHsbToProperty();
                return true;
            }
        }

        return false;
    }

    private void applyHsbToProperty() {
        if (activeTarget == 2) {
            int rgb = Color.HSBtoRGB(hue2, sat2, bri2);
            property.setColor2(ARGB.color(alpha2, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb)));
        } else {
            int rgb = Color.HSBtoRGB(hue1, sat1, bri1);
            property.setColor1(ARGB.color(alpha1, ARGB.red(rgb), ARGB.green(rgb), ARGB.blue(rgb)));
        }
    }

    @Override
    public float getHeight() {
        return 20.0f + (activeTarget > 0 ? 86.0f * expandAnim.getValue() : 0.0f);
    }
}
