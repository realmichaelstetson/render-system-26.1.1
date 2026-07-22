package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.Module;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import com.haloclient.client.gui.click.ClickGUI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

public class KeybindElement extends ClickGUIElement {
    private final Module module;

    public KeybindElement(Module module) {
        this.module = module;
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
            // 1. Left label
            float textX = Math.round(x + 8.0f);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    "Keybind",
                    pose,
                    textX, y + (20.0f - font.getHeight(8f)) / 2.0f,
                    8f,
                    ARGB.color(200, 220, 220, 220),
                    scissorArea
            ));

            // 2. Resolve key name
            boolean isBinding = ClickGUI.getBindingModule() == module;
            String keyName = isBinding ? "..." : (module.getKey() == 0 ? "NONE" : GLFW.glfwGetKeyName(module.getKey(), 0));
            if (keyName == null && !isBinding) {
                if (module.getKey() > 0) {
                    keyName = "KEY " + module.getKey();
                } else {
                    keyName = "NONE";
                }
            }

            // 3. Right box calculation
            float fontScale = 7.0f;
            float contentW = font.getWidth(keyName, fontScale);
            float boxW = Math.max(30.0f, contentW + 16.0f);
            float boxH = 13.0f;
            float boxX = x + width - 6.0f - boxW;
            float boxY = Math.round(y + (20.0f - boxH) / 2.0f - 0.5f);

            // Header rect background
            boolean hovered = mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f;
            int bgCol = isBinding
                    ? ARGB.color(180, 40, 100, 160)
                    : (hovered ? ARGB.color(170, 28, 30, 38) : ARGB.color(130, 22, 24, 30));

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
            float textY = Math.round(boxY + (boxH - font.getHeight(fontScale)) / 2.0f);
            float keyTextX = boxX + (boxW - contentW) / 2.0f;

            ScreenRectangle textClip = new ScreenRectangle((int) boxX + 2, (int) boxY, (int) boxW - 4, (int) boxH).transformMaxBounds(pose);
            ScreenRectangle finalFontScissor = scissorArea != null ? scissorArea.intersection(textClip) : textClip;

            if (finalFontScissor != null) {
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        font,
                        keyName,
                        pose,
                        keyTextX, textY,
                        fontScale,
                        ARGB.color(255, 180, 190, 205),
                        finalFontScissor
                ));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        if (mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0) {
                ClickGUI.setBindingModule(module);
                return true;
            }
        }
        return false;
    }

    @Override
    public float getHeight() {
        return 20.0f;
    }
}
