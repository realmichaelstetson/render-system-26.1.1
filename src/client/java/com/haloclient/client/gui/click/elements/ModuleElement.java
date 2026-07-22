package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.Property;
import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.module.property.NumberProperty;
import com.haloclient.client.module.property.ModeProperty;
import com.haloclient.client.module.property.MultipleComboBoxProperty;
import com.haloclient.client.module.property.ColorPickerProperty;
import com.haloclient.client.module.property.ComboBoxProperty;
import com.haloclient.client.module.property.GroupProperty;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public class ModuleElement extends ClickGUIElement {

    private final Module module;
    private final Animation expandAnimation;
    private final Animation hoverAnimation;
    private final List<ClickGUIElement> properties;

    public ModuleElement(Module module) {
        this.module = module;
        this.expandAnimation = new Animation(Easing.EASE_OUT_QUART, 250L);
        this.expandAnimation.setValue(module.isExpanded() ? 1.0f : 0.0f);
        this.hoverAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
        this.properties = new ArrayList<>();
        for (Property<?> prop : module.getProperties()) {
            if (prop instanceof GroupProperty) {
                properties.add(new GroupPropertyElement((GroupProperty) prop));
            } else if (prop instanceof MultipleComboBoxProperty) {
                properties.add(new MultipleComboBoxPropertyElement((MultipleComboBoxProperty) prop));
            } else if (prop instanceof ComboBoxProperty) {
                properties.add(new ComboBoxPropertyElement((ComboBoxProperty) prop));
            } else if (prop instanceof BooleanProperty) {
                properties.add(new BooleanPropertyElement((BooleanProperty) prop));
            } else if (prop instanceof NumberProperty) {
                properties.add(new NumberPropertyElement((NumberProperty) prop));
            } else if (prop instanceof ModeProperty) {
                properties.add(new ModePropertyElement((ModeProperty) prop));
            } else if (prop instanceof ColorPickerProperty) {
                properties.add(new ColorPickerPropertyElement((ColorPickerProperty) prop));
            }
        }
        properties.add(new KeybindElement(module));
    }

    private boolean isPropVisible(ClickGUIElement el) {
        if (el instanceof GroupPropertyElement) {
            return ((GroupPropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof MultipleComboBoxPropertyElement) {
            return ((MultipleComboBoxPropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof ComboBoxPropertyElement) {
            return ((ComboBoxPropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof BooleanPropertyElement) {
            return ((BooleanPropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof NumberPropertyElement) {
            return ((NumberPropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof ModePropertyElement) {
            return ((ModePropertyElement) el).getProperty().isVisible();
        }
        if (el instanceof ColorPickerPropertyElement) {
            return ((ColorPickerPropertyElement) el).getProperty().isVisible();
        }
        return true; // Keybind is always visible
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
        expandAnimation.run(module.isExpanded() ? 1.0f : 0.0f);
        float animProgress = expandAnimation.getValue();

        boolean hovered = mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + 20.0f;

        hoverAnimation.run(hovered ? 1.0f : 0.0f);
        float hoverProgress = hoverAnimation.getValue();

        float moduleCornerMask = animProgress > 0.01f ? 4.0f : 0.0f;
        float activeWeight = module.isEnabled() ? 1.0f : animProgress;

        int hoverCol = ARGB.color((int) (80 * hoverProgress), 18, 19, 23);
        int activeCol = module.isEnabled() ? ARGB.color(100, 18, 19, 23) : ARGB.color(100, 0, 0, 0);

        int alpha = (int) (ARGB.alpha(hoverCol) + (ARGB.alpha(activeCol) - ARGB.alpha(hoverCol)) * activeWeight);
        int red = (int) (ARGB.red(hoverCol) + (ARGB.red(activeCol) - ARGB.red(hoverCol)) * activeWeight);
        int green = (int) (ARGB.green(hoverCol) + (ARGB.green(activeCol) - ARGB.green(hoverCol)) * activeWeight);
        int blue = (int) (ARGB.blue(hoverCol) + (ARGB.blue(activeCol) - ARGB.blue(hoverCol)) * activeWeight);
        int finalBgCol = ARGB.color(alpha, red, green, blue);

        if (ARGB.alpha(finalBgCol) > 1) {
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    x + 3, y + 1, width - 6, 20.0f - 2,
                    finalBgCol,
                    3.0f,
                    moduleCornerMask,
                    scissorArea
            ));
        }

        // Module name text
        if (font != null) {
            MsdfFont boldFont = com.haloclient.client.render.font.MsdfFontManager.getFont("inter-semibold", 9f);
            MsdfFont displayFont = boldFont != null ? boldFont : font;

            int textColor = module.isEnabled()
                    ? ARGB.color(255, 255, 255, 255)
                    : ARGB.color(200, 170, 175, 185);

            float textX = Math.round(x + 8.0f);
            float textY = Math.round(y + (20.0f - displayFont.getHeight(9f)) / 2.0f) - 0.5f;

            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    displayFont,
                    module.getName(),
                    pose,
                    textX, textY,
                    9f,
                    textColor,
                    scissorArea
            ));
        }

        // Render collapse arrow next to name if it has properties
        if (!module.getProperties().isEmpty()) {
            float arrowSize = 8.0f;
            float arrowX = x + width - 18.0f;
            float arrowY = y + (20.0f - arrowSize) / 2.0f;
            int arrowColor = ARGB.color(200, 220, 220, 220);
            float thickness = 0.5f;

            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.CHEVRON,
                    pose,
                    arrowX, arrowY, arrowSize, arrowSize,
                    arrowColor, arrowColor,
                    thickness,
                    animProgress,
                    0.0f,
                    scissorArea
            ));
        }

        // Close open comboboxes if module is not expanded or collapsing
        if (!module.isExpanded() || animProgress < 0.99f) {
            for (ClickGUIElement prop : properties) {
                if (prop instanceof MultipleComboBoxPropertyElement) {
                    ((MultipleComboBoxPropertyElement) prop).closeInstant();
                } else if (prop instanceof ComboBoxPropertyElement) {
                    ((ComboBoxPropertyElement) prop).closeInstant();
                }
            }
        }

        // Render settings if expanded or expanding
        float settingsHeight = 0.0f;
        for (ClickGUIElement prop : properties) {
            if (isPropVisible(prop)) {
                settingsHeight += prop.getHeight();
            }
        }

        float currentY = y + 19.0f;
        float animatedSettingsHeight = settingsHeight * animProgress;

        if (animProgress > 0.01f && animatedSettingsHeight >= 1.0f) {
            int scissorH = Math.max(1, Math.round(animatedSettingsHeight));

            // Group card background
            int cardColor = ARGB.color(100, 0, 0, 0);
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    x + 3, currentY, width - 6, animatedSettingsHeight,
                    cardColor, cardColor,
                    3.0f,
                    0.0f,
                    3.0f, // cornerMask = 3.0f (top corners flat)
                    scissorArea
            ));

            // Scissor bounds to clip settings elements during expansion
            ScreenRectangle settingsBounds = new ScreenRectangle(
                    (int) x + 3,
                    (int) currentY,
                    (int) width - 6,
                    scissorH
            ).transformMaxBounds(pose);

            ScreenRectangle elementScissor = scissorArea != null
                    ? scissorArea.intersection(settingsBounds)
                    : settingsBounds;

            if (elementScissor != null) {
                // Pass 1: Render base elements
                float propY = currentY + 2.0f;
                for (ClickGUIElement prop : properties) {
                    if (!isPropVisible(prop)) continue;
                    prop.render(graphics, pose, x, propY, width, mouseX, mouseY, delta, elementScissor, font);
                    propY += prop.getHeight();
                }

                // Pass 2: Render popup overlays on top of everything (using root scissorArea)
              //  CaptureManager.prepareBlurLayer(graphics);
                propY = currentY + 2.0f;
                for (ClickGUIElement prop : properties) {
                    if (!isPropVisible(prop)) continue;
                    if (prop instanceof MultipleComboBoxPropertyElement) {
                        ((MultipleComboBoxPropertyElement) prop).renderPopup(graphics, pose, x, propY, width, mouseX, mouseY, delta, scissorArea, font);
                    } else if (prop instanceof ComboBoxPropertyElement) {
                        ((ComboBoxPropertyElement) prop).renderPopup(graphics, pose, x, propY, width, mouseX, mouseY, delta, scissorArea, font);
                    } else if (prop instanceof GroupPropertyElement) {
                        ((GroupPropertyElement) prop).renderPopup(graphics, pose, x, propY, width, mouseX, mouseY, delta, scissorArea, font);
                    }
                    propY += prop.getHeight();
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        // Check module click
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0) {
                module.toggle();
                return true;
            } else if (button == 1) {
                if (!module.getProperties().isEmpty()) {
                    module.setExpanded(!module.isExpanded());
                }
                return true;
            }
        }

        // Check child properties click if expanded/expanding
        float animProgress = expandAnimation.getValue();
        if (animProgress > 0.8f) { // Only clickable when mostly expanded
            float currentY = y + 20.0f;

            // Pass 1: Check open popup clicks first
            for (ClickGUIElement prop : properties) {
                if (!isPropVisible(prop)) continue;
                if (prop instanceof MultipleComboBoxPropertyElement) {
                    if (((MultipleComboBoxPropertyElement) prop).mouseClickedPopup(mouseX, mouseY, button, x, currentY, width)) {
                        return true;
                    }
                } else if (prop instanceof ComboBoxPropertyElement) {
                    if (((ComboBoxPropertyElement) prop).mouseClickedPopup(mouseX, mouseY, button, x, currentY, width)) {
                        return true;
                    }
                }
                currentY += prop.getHeight();
            }

            // Pass 2: Check standard element header clicks
            currentY = y + 20.0f;
            for (ClickGUIElement prop : properties) {
                if (!isPropVisible(prop)) continue;
                if (prop.mouseClicked(mouseX, mouseY, button, x, currentY, width)) {
                    return true;
                }
                currentY += prop.getHeight();
            }
        }

        return false;
    }

    @Override
    public float getHeight() {
        expandAnimation.run(module.isExpanded() ? 1.0f : 0.0f);
        float animProgress = expandAnimation.getValue();

        float settingsHeight = 0.0f;
        for (ClickGUIElement prop : properties) {
            if (isPropVisible(prop)) {
                settingsHeight += prop.getHeight();
            }
        }

        float totalHeight = 20.0f;
        if (animProgress > 0.001f) {
            totalHeight += (settingsHeight + 4.0f) * animProgress;
        }
        return totalHeight;
    }

    public Module getModule() {
        return module;
    }

    public void closePopupsExcept(ClickGUIElement except) {
        for (ClickGUIElement prop : properties) {
            if (prop == except) continue;
            if (prop instanceof ComboBoxPropertyElement) {
                ((ComboBoxPropertyElement) prop).closeInstant();
            } else if (prop instanceof MultipleComboBoxPropertyElement) {
                ((MultipleComboBoxPropertyElement) prop).closeInstant();
            } else if (prop instanceof GroupPropertyElement) {
                ((GroupPropertyElement) prop).closePopupsExcept(except);
            }
        }
    }
}
