package com.haloclient.client.gui.click.elements;

import com.haloclient.client.gui.click.ClickGUI;
import com.haloclient.client.module.property.ComboBoxProperty;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

import java.util.List;

public class ComboBoxPropertyElement extends ClickGUIElement {
    private final ComboBoxProperty property;
    private boolean expanded = false;
    private final Animation expandAnim;
    private final Animation boxWidthAnim;
    private final Animation popupWidthAnim;

    public ComboBoxPropertyElement(ComboBoxProperty property) {
        this.property = property;
        this.expandAnim = new Animation(Easing.EASE_OUT_QUART, 200L);
        this.boxWidthAnim = new Animation(Easing.EASE_OUT_QUART, 200L);
        this.popupWidthAnim = new Animation(Easing.EASE_OUT_QUART, 200L);
    }

    public boolean isExpanded() {
        return expanded || expandAnim.getValue() > 0.001f;
    }

    public void closeInstant() {
        this.expanded = false;
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
        expandAnim.run(expanded ? 1.0f : 0.0f);
        float progress = expandAnim.getValue();

        // 1. Render left label (property name)
        float labelW = 0.0f;
        if (font != null) {
            float labelY = Math.round(y + (20.0f - font.getHeight(7.5f)) / 2.0f - 1.0f);
            labelW = font.getWidth(property.getName(), 7.5f);
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

        // 2. Right box calculation
        String displayStr = property.getValue();
        float labelEndX = x + 8.0f + labelW;
        float maxAvailableBoxW = Math.max(25.0f, (x + width - 6.0f) - (labelEndX + 6.0f));

        float fontScale = 7.0f;
        if (font != null) {
            while (displayStr.length() > 3 && font.getWidth(displayStr, fontScale) + 21.0f > maxAvailableBoxW) {
                displayStr = displayStr.substring(0, displayStr.length() - 4) + "..";
            }
        }

        float contentW = font != null ? font.getWidth(displayStr, fontScale) : 20.0f;
        float targetBoxW = Math.min(maxAvailableBoxW, Math.max(35.0f, contentW + 21.0f));
        if (boxWidthAnim.getValue() <= 0.001f) {
            boxWidthAnim.setValue(targetBoxW);
        }
        boxWidthAnim.run(targetBoxW);
        float boxW = boxWidthAnim.getValue();
        float boxH = 13.0f;
        float boxX = x + width - 6.0f - boxW;
        float boxY = Math.round(y + (20.0f - boxH) / 2.0f - 0.5f);

        // Header rect background
        boolean hovered = mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f;
        int bgCol = hovered ? ARGB.color(170, 28, 30, 38) : ARGB.color(130, 22, 24, 30);
        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                boxX, boxY, boxW, boxH,
                bgCol, bgCol,
                3.0f, 0.0f,
                0.0f,
                scissorArea
        ));

        // Text inside header rect
        if (font != null) {
            float textY = Math.round(boxY + (boxH - font.getHeight(fontScale)) / 2.0f);
            ScreenRectangle textClip = new ScreenRectangle((int) boxX + 3, (int) boxY, (int) boxW - 14, (int) boxH).transformMaxBounds(pose);
            ScreenRectangle finalFontScissor = scissorArea != null ? scissorArea.intersection(textClip) : textClip;

            if (finalFontScissor != null) {
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        font,
                        displayStr,
                        pose,
                        boxX + 4.0f, textY,
                        fontScale,
                        ARGB.color(255, 180, 190, 205),
                        finalFontScissor
                ));
            }
        }

        // Chevron Arrow on right side of header box (progress: 0 = down, 1 = up)
        float arrowSize = 6.0f;
        float arrowX = boxX + boxW - 10.0f;
        float arrowY = boxY + (boxH - arrowSize) / 2.0f;
        int arrowColor = ARGB.color(220, 200, 200, 200);

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.CHEVRON,
                pose,
                arrowX, arrowY, arrowSize, arrowSize,
                arrowColor, arrowColor,
                0.5f,
                progress,
                0.0f,
                scissorArea
        ));
    }

    public void renderPopup(
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
        expandAnim.run(expanded ? 1.0f : 0.0f);
        float progress = expandAnim.getValue();
        if (progress <= 0.001f) return;

        List<String> options = property.getOptions();
        if (options.isEmpty()) return;

        float labelW = font != null ? font.getWidth(property.getName(), 7.5f) : 30.0f;
        String displayStr = property.getValue();
        float labelEndX = x + 8.0f + labelW;
        float maxAvailableBoxW = Math.max(25.0f, (x + width - 6.0f) - (labelEndX + 6.0f));

        float fontScale = 7.0f;
        float contentW = font != null ? font.getWidth(displayStr, fontScale) : 20.0f;
        float targetBoxW = Math.min(maxAvailableBoxW, Math.max(35.0f, contentW + 21.0f));
        boxWidthAnim.run(targetBoxW);
        float boxW = boxWidthAnim.getValue();
        float boxH = 13.0f;
        float boxX = x + width - 6.0f - boxW;
        float boxY = Math.round(y + (20.0f - boxH) / 2.0f - 0.5f);

        float maxOptW = 0.0f;
        if (font != null) {
            for (String opt : options) {
                float w = font.getWidth(opt, 7.0f);
                if (w > maxOptW) maxOptW = w;
            }
        } else {
            maxOptW = 40.0f;
        }

        float dotSize = 4.0f;
        float requiredPopupW = maxOptW + 10.0f + dotSize + 5.0f;
        float targetPopupW = Math.max(boxW, requiredPopupW);
        if (popupWidthAnim.getValue() <= 0.001f) {
            popupWidthAnim.setValue(targetPopupW);
        }
        popupWidthAnim.run(targetPopupW);
        float currentTargetW = popupWidthAnim.getValue();

        // Animate width smoothly from boxW to target popup width as progress expands
        float animatedPopupW = boxW + (currentTargetW - boxW) * progress;
        float popupX = Math.min(x + width - 6.0f - animatedPopupW, boxX);
        float itemH = 15.0f;
        float popupH = options.size() * itemH + 2.0f;
        float animatedPopupH = popupH * progress;
        float popupY = boxY + boxH + 2.0f;

        // Blurred background popup card
        int popupBg = ARGB.color(210, 18, 20, 26);
       // CaptureManager.prepareBlurLayer(graphics);
        graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_BLUR,
                CaptureManager.getCaptureTextureSetup(),
                pose,
                popupX, popupY, animatedPopupW, animatedPopupH,
                popupBg, popupBg,
                4.0f,
                5.0f,
                0.0f,
                0.0f,
                scissorArea
        ));

        ScreenRectangle popupBounds = new ScreenRectangle(
                (int) popupX, (int) popupY, (int) animatedPopupW, (int) animatedPopupH
        ).transformMaxBounds(pose);
        ScreenRectangle popupScissor = scissorArea != null ? scissorArea.intersection(popupBounds) : popupBounds;

        float optY = popupY + 1.0f;
        for (String opt : options) {
            boolean isSelected = opt.equals(property.getValue());
            boolean optHovered = mouseX >= popupX && mouseX <= popupX + animatedPopupW && mouseY >= optY && mouseY < optY + itemH;

            if (optHovered) {
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        pose,
                        popupX + 1, optY, animatedPopupW - 2, itemH,
                        ARGB.color(230, 20, 20, 20),
                        4.0f,
                        popupScissor
                ));
            }

            // Option text on left
            if (font != null) {
                int optTextColor = isSelected ? ARGB.color(255, 255, 255, 255) : ARGB.color(180, 170, 175, 185);
                float textY = Math.round(optY + (itemH - font.getHeight(7.0f)) / 2.0f);
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        font,
                        opt,
                        pose,
                        popupX + 6.0f, textY,
                        7.0f,
                        optTextColor,
                        popupScissor
                ));
            }

            // White dot indicator on the right for active item
            if (isSelected) {
                float dotX = popupX + animatedPopupW - 5.0f - dotSize;
                float dotY = Math.round(optY + (itemH - dotSize) / 2.0f);

                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        pose,
                        dotX, dotY, dotSize, dotSize,
                        ARGB.color(255, 255, 255, 255),
                        2.0f,
                        popupScissor
                ));
            }

            optY += itemH;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0 || button == 1) {
                boolean targetState = !expanded;
                if (targetState) {
                    ClickGUI.closeOtherPopups(this);
                }
                expanded = targetState;
                return true;
            }
        }
        return false;
    }

    public boolean mouseClickedPopup(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (expanded && expandAnim.getValue() > 0.5f) {
            float labelW = 30.0f;
            String displayStr = property.getValue();
            float labelEndX = x + 8.0f + labelW;
            float maxAvailableBoxW = Math.max(25.0f, (x + width - 6.0f) - (labelEndX + 6.0f));

            float contentW = 20.0f;
            float boxW = Math.min(maxAvailableBoxW, Math.max(30.0f, contentW + 16.0f));
            float boxH = 13.0f;
            float boxX = x + width - 6.0f - boxW;
            float boxY = Math.round(y + (20.0f - boxH) / 2.0f - 0.5f);

            float popupW = Math.max(boxW, 70.0f);
            float popupX = Math.min(x + width - 6.0f - popupW, boxX);
            float popupY = boxY + boxH + 2.0f;
            float itemH = 15.0f;
            List<String> options = property.getOptions();
            float optY = popupY + 1.0f;

            for (String opt : options) {
                if (mouseX >= popupX && mouseX <= popupX + popupW && mouseY >= optY && mouseY < optY + itemH) {
                    if (button == 0) {
                        property.select(opt);
                        expanded = false;
                        return true;
                    }
                }
                optY += itemH;
            }
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 20.0f;
    }

    public ComboBoxProperty getProperty() {
        return property;
    }
}
