package com.haloclient.client.gui.click.elements;

import com.haloclient.client.module.property.GroupProperty;
import com.haloclient.client.module.property.Property;
import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.module.property.NumberProperty;
import com.haloclient.client.module.property.ModeProperty;
import com.haloclient.client.module.property.MultipleComboBoxProperty;
import com.haloclient.client.module.property.ComboBoxProperty;
import com.haloclient.client.module.property.ColorPickerProperty;
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

public class GroupPropertyElement extends ClickGUIElement {
    private final GroupProperty property;
    private boolean expanded = false;
    private final Animation expandAnimation;
    private final Animation hoverAnimation;
    private final List<ClickGUIElement> childElements;

    public GroupPropertyElement(GroupProperty property) {
        this.property = property;
        this.expandAnimation = new Animation(Easing.EASE_OUT_QUART, 200L);
        this.hoverAnimation = new Animation(Easing.EASE_OUT_QUART, 150L);
        this.childElements = new ArrayList<>();

        for (Property<?> child : property.getProperties()) {
            if (child instanceof GroupProperty) {
                childElements.add(new GroupPropertyElement((GroupProperty) child));
            } else if (child instanceof MultipleComboBoxProperty) {
                childElements.add(new MultipleComboBoxPropertyElement((MultipleComboBoxProperty) child));
            } else if (child instanceof ComboBoxProperty) {
                childElements.add(new ComboBoxPropertyElement((ComboBoxProperty) child));
            } else if (child instanceof BooleanProperty) {
                childElements.add(new BooleanPropertyElement((BooleanProperty) child));
            } else if (child instanceof NumberProperty) {
                childElements.add(new NumberPropertyElement((NumberProperty) child));
            } else if (child instanceof ModeProperty) {
                childElements.add(new ModePropertyElement((ModeProperty) child));
            } else if (child instanceof ColorPickerProperty) {
                childElements.add(new ColorPickerPropertyElement((ColorPickerProperty) child));
            }
        }
    }

    public GroupProperty getProperty() {
        return property;
    }

    public boolean isExpanded() {
        return expanded || expandAnimation.getValue() > 0.001f;
    }

    public void closeInstant() {
        this.expanded = false;
        this.expandAnimation.setValue(0.0f);
        for (ClickGUIElement child : childElements) {
            if (child instanceof ComboBoxPropertyElement) {
                ((ComboBoxPropertyElement) child).closeInstant();
            } else if (child instanceof MultipleComboBoxPropertyElement) {
                ((MultipleComboBoxPropertyElement) child).closeInstant();
            } else if (child instanceof GroupPropertyElement) {
                ((GroupPropertyElement) child).closeInstant();
            } else if (child instanceof ColorPickerPropertyElement) {
                ((ColorPickerPropertyElement) child).closeInstant();
            }
        }
    }

    private boolean isChildVisible(ClickGUIElement el) {
        if (el instanceof GroupPropertyElement) return ((GroupPropertyElement) el).getProperty().isVisible();
        if (el instanceof MultipleComboBoxPropertyElement) return ((MultipleComboBoxPropertyElement) el).getProperty().isVisible();
        if (el instanceof ComboBoxPropertyElement) return ((ComboBoxPropertyElement) el).getProperty().isVisible();
        if (el instanceof BooleanPropertyElement) return ((BooleanPropertyElement) el).getProperty().isVisible();
        if (el instanceof NumberPropertyElement) return ((NumberPropertyElement) el).getProperty().isVisible();
        if (el instanceof ModePropertyElement) return ((ModePropertyElement) el).getProperty().isVisible();
        if (el instanceof ColorPickerPropertyElement) return ((ColorPickerPropertyElement) el).getProperty().isVisible();
        return true;
    }

    public float getChildrenHeight() {
        float h = 0.0f;
        for (ClickGUIElement child : childElements) {
            if (isChildVisible(child)) {
                h += child.getHeight();
            }
        }
        return h;
    }

    @Override
    public float getHeight() {
        expandAnimation.run(expanded ? 1.0f : 0.0f);
        float progress = expandAnimation.getValue();
        return 20.0f + getChildrenHeight() * progress;
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
        expandAnimation.run(expanded ? 1.0f : 0.0f);
        float animProgress = expandAnimation.getValue();

        boolean hovered = mouseX >= x + 3 && mouseX <= x + width - 3 && mouseY >= y && mouseY <= y + 20.0f;
        hoverAnimation.run(hovered ? 1.0f : 0.0f);
        float hoverProgress = hoverAnimation.getValue();

        // 1. Group title text on left
        if (font != null) {
            float textX = x + 8.0f;
            float textY = Math.round(y + (20.0f - font.getHeight(7.5f)) / 2.0f - 1.0f);
            int textAlpha = expanded ? 250 : 200;
            int textColor = ARGB.color(textAlpha, 220, 220, 220);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    property.getName(),
                    pose,
                    textX, textY,
                    7.5f,
                    textColor,
                    scissorArea
            ));
        }

        // 2. Chevron arrow on right side (rotates 0 = down, 1 = up)
        float arrowSize = 6.0f;
        float arrowX = x + width - 15.0f;
        float arrowY = y + (20.0f - arrowSize) / 2.0f;
        int arrowColor = ARGB.color(200, 220, 220, 220);

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.CHEVRON,
                pose,
                arrowX, arrowY, arrowSize, arrowSize,
                arrowColor, arrowColor,
                0.5f,
                animProgress,
                0.0f,
                scissorArea
        ));

        // Close popups if group is collapsing or collapsed
        if (!expanded || animProgress < 0.99f) {
            for (ClickGUIElement child : childElements) {
                if (child instanceof ComboBoxPropertyElement) {
                    ((ComboBoxPropertyElement) child).closeInstant();
                } else if (child instanceof MultipleComboBoxPropertyElement) {
                    ((MultipleComboBoxPropertyElement) child).closeInstant();
                } else if (child instanceof ColorPickerPropertyElement) {
                    ((ColorPickerPropertyElement) child).closeInstant();
                }
            }
        }

        // 3. Render children if expanding or expanded
        float totalChildrenH = getChildrenHeight();
        float animatedChildrenH = totalChildrenH * animProgress;
        if (animProgress > 0.01f && animatedChildrenH >= 1.0f) {
            float childrenY = y + 20.0f;
            int scissorH = Math.max(1, Math.round(animatedChildrenH));

            ScreenRectangle groupBounds = new ScreenRectangle(
                    (int) x + 3,
                    (int) childrenY,
                    (int) width - 6,
                    scissorH
            ).transformMaxBounds(pose);

            ScreenRectangle childrenScissor = scissorArea != null
                    ? scissorArea.intersection(groupBounds)
                    : groupBounds;

            if (childrenScissor != null) {
                float childY = childrenY;
                for (ClickGUIElement child : childElements) {
                    if (!isChildVisible(child)) continue;
                    child.render(graphics, pose, x, childY, width, mouseX, mouseY, delta, childrenScissor, font);
                    childY += child.getHeight();
                }
            }
        }
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
        if (!isExpanded()) return;
        float animProgress = expandAnimation.getValue();
        if (animProgress <= 0.001f) return;

        float childY = y + 20.0f;
        for (ClickGUIElement child : childElements) {
            if (!isChildVisible(child)) continue;
            if (child instanceof ComboBoxPropertyElement) {
                ((ComboBoxPropertyElement) child).renderPopup(graphics, pose, x, childY, width, mouseX, mouseY, delta, scissorArea, font);
            } else if (child instanceof MultipleComboBoxPropertyElement) {
                ((MultipleComboBoxPropertyElement) child).renderPopup(graphics, pose, x, childY, width, mouseX, mouseY, delta, scissorArea, font);
            } else if (child instanceof GroupPropertyElement) {
                ((GroupPropertyElement) child).renderPopup(graphics, pose, x, childY, width, mouseX, mouseY, delta, scissorArea, font);
            }
            childY += child.getHeight();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
        // Left (0) or Right (1) click on group header toggles expand/collapse
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20.0f) {
            if (button == 0 || button == 1) {
                expanded = !expanded;
                return true;
            }
        }

        // If expanded, forward clicks to child elements
        if (isExpanded()) {
            float childY = y + 20.0f;
            for (ClickGUIElement child : childElements) {
                if (!isChildVisible(child)) continue;
                float childH = child.getHeight();
                if (mouseY >= childY && mouseY < childY + childH) {
                    if (child.mouseClicked(mouseX, mouseY, button, x, childY, width)) {
                        return true;
                    }
                }
                childY += childH;
            }
        }

        return false;
    }

    public void closePopupsExcept(ClickGUIElement except) {
        for (ClickGUIElement child : childElements) {
            if (child == except) continue;
            if (child instanceof ComboBoxPropertyElement) {
                ((ComboBoxPropertyElement) child).closeInstant();
            } else if (child instanceof MultipleComboBoxPropertyElement) {
                ((MultipleComboBoxPropertyElement) child).closeInstant();
            } else if (child instanceof GroupPropertyElement) {
                ((GroupPropertyElement) child).closePopupsExcept(except);
            } else if (child instanceof ColorPickerPropertyElement) {
                ((ColorPickerPropertyElement) child).closeInstant();
            }
        }
    }
}
