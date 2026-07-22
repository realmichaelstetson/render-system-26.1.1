package com.haloclient.client.gui.click;

import com.haloclient.client.HaloClient;
import com.haloclient.client.gui.click.elements.ModuleElement;
import com.haloclient.client.gui.click.elements.ClickGUIElement;
import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.font.MsdfFontManager;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {

    private static final float PANEL_WIDTH = 120.0f;
    private static final float HEADER_HEIGHT = 22.0f;

    public static class CategoryPanel {
        final Category category;
        String icon;
        float iconSize;
        float x;
        float y;
        boolean collapsed = false;
        
        // Dragging state
        boolean dragging = false;
        float dragX;
        float dragY;

        final List<ModuleElement> modules = new ArrayList<>();

        public CategoryPanel(Category category, String icon, float iconSize, float x, float y) {
            this.category = category;
            this.icon = icon;
            this.iconSize = iconSize;
            this.x = x;
            this.y = y;
        }

        public CategoryPanel(Category category, float x, float y) {
            this(category, "K", 22.0f, x, y);
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        public float getIconSize() {
            return iconSize;
        }

        public void setIconSize(float iconSize) {
            this.iconSize = iconSize;
        }

        void initModules() {
            if (modules.isEmpty() && HaloClient.INSTANCE != null && HaloClient.INSTANCE.getModuleManager() != null) {
                List<Module> list = HaloClient.INSTANCE.getModuleManager().getModulesByCategory(category);
                for (Module m : list) {
                    modules.add(new ModuleElement(m));
                }
            }
        }
    }

    private static final List<CategoryPanel> PANELS = new ArrayList<>();
    static {
        PANELS.add(new CategoryPanel(Category.COMBAT, "L", 22.0f, 30.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.MOVEMENT, "N", 24.0f, 170.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.PLAYER, "K", 22.0f, 310.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.RENDER, "M", 22.0f, 450.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.VISUAL, "J", 22.0f, 590.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.EXPLOIT, "A", 22.0f, 730.0f, 50.0f));
        PANELS.add(new CategoryPanel(Category.WORLD, "B", 22.0f, 870.0f, 50.0f));
    }

    private final Animation scaleAnimation = new Animation(Easing.EASE_OUT_QUART, 300L);
    private boolean closing = false;
    private static Module bindingModule = null;

    public ClickGUI() {
        super(Component.literal("ClickGUI"));
        scaleAnimation.setValue(0.0f);
    }

    public static Module getBindingModule() {
        return bindingModule;
    }

    public static void setBindingModule(Module module) {
        bindingModule = module;
    }

    private float getPanelContentHeight(CategoryPanel panel) {
        panel.initModules();
        if (panel.modules.isEmpty()) return 0.0f;

        float height = 0.0f;
        for (ModuleElement me : panel.modules) {
            height += me.getHeight();
        }
        return height;
    }

    public static void closeOtherPopups(ClickGUIElement except) {
        for (CategoryPanel panel : PANELS) {
            for (ModuleElement me : panel.modules) {
                me.closePopupsExcept(except);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        scaleAnimation.run(closing ? 0.0f : 1.0f);

        if (closing && scaleAnimation.getValue() <= 0.001f) {
            this.minecraft.setScreen(null);
            return;
        }

        // Handle dragging update
        boolean mouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                this.minecraft.getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        float scale = scaleAnimation.getValue();
        if (scale < 0.999f) {
            float centerX = this.width / 2.0f;
            float centerY = this.height / 2.0f;
            pose.translate(centerX, centerY);
            pose.scale(scale);
            pose.translate(-centerX, -centerY);
        }

        ScreenRectangle rootScissor = graphics.scissorStack.peek();

        MsdfFont headerFont = MsdfFontManager.getFont("productsans-semibold", 16f);
        MsdfFont moduleFont = MsdfFontManager.getFont("productsans-regular", 15f);
        MsdfFont iconFont = MsdfFontManager.getFont("fluid-regular", 16f);

        for (CategoryPanel panel : PANELS) {
            panel.initModules();
            if (panel.modules.isEmpty()) {
                continue;
            }

            // Drag logic
            if (panel.dragging) {
                if (mouseDown) {
                    panel.x = mouseX - panel.dragX;
                    panel.y = mouseY - panel.dragY;
                } else {
                    panel.dragging = false;
                }
            }

            // Draw Header Background with Blurred Liquid Glass
            int headerColor = ARGB.color(200, 25, 27, 33);
            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_BLUR,
                    CaptureManager.getCaptureTextureSetup(),
                    pose,
                    panel.x, panel.y, PANEL_WIDTH, HEADER_HEIGHT,
                    headerColor, headerColor,
                    5.0f,  // radius
                    10.0f, // blurStrength
                    0.0f,  // bloom
                    0.0f,  // gradientAngle
                    4.0f,  // cornerMask (bottom corners flat)
                    rootScissor
            ));

            // Draw Header Text
            if (headerFont != null) {
                String title = panel.category.getName();
                float textX = panel.x + 7;
                float textY = panel.y + (HEADER_HEIGHT - headerFont.getHeight(10f)) / 2.0f;
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        headerFont,
                        title,
                        pose,
                        textX, textY,
                        10f,
                        ARGB.color(255, 255, 255, 255),
                        rootScissor
                ));
            }

            // Draw Category Icon
            if (iconFont != null && panel.icon != null && !panel.icon.isEmpty()) {
                float size = panel.iconSize > 0 ? panel.iconSize : 22.0f;
                float iconW = iconFont.getWidth(panel.icon, size);
                float iconX = panel.x + PANEL_WIDTH - iconW - 8.0f;
                float iconY = panel.y + (HEADER_HEIGHT - iconFont.getHeight(size)) / 2.0f;
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        iconFont,
                        panel.icon,
                        pose,
                        iconX, iconY,
                        size,
                        ARGB.color(255, 255, 255, 255),
                        rootScissor
                ));
            }

            // Dropdown contents
            if (!panel.collapsed) {
                panel.initModules();
                if (!panel.modules.isEmpty()) {
                    float totalContentHeight = getPanelContentHeight(panel);

                    // Dropdown Background with Blurred Liquid Glass
                    int bgColor = ARGB.color(160, 18, 19, 23);
                    graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                            HaloRenderPipelines.ROUNDED_BLUR,
                            CaptureManager.getCaptureTextureSetup(),
                            pose,
                            panel.x, panel.y + HEADER_HEIGHT - 1, PANEL_WIDTH, totalContentHeight + 2,
                            bgColor, bgColor,
                            5.0f,  // radius
                            10.0f, // blurStrength
                            0.0f,  // bloom
                            0.0f,  // gradientAngle
                            3.0f,  // cornerMask (top corners flat)
                            rootScissor
                    ));

                    // Modules
                    float currentY = panel.y + HEADER_HEIGHT;
                    for (ModuleElement me : panel.modules) {
                        me.render(graphics, pose, panel.x, currentY, PANEL_WIDTH, mouseX, mouseY, delta, rootScissor, moduleFont);
                        currentY += me.getHeight();
                    }
                }
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (handled) return true;

        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Iterate backward so top-most elements are clicked first
        for (int i = PANELS.size() - 1; i >= 0; i--) {
            CategoryPanel panel = PANELS.get(i);
            panel.initModules();
            if (panel.modules.isEmpty()) {
                continue;
            }

            // Check header click
            if (mouseX >= panel.x && mouseX <= panel.x + PANEL_WIDTH
                    && mouseY >= panel.y && mouseY <= panel.y + HEADER_HEIGHT) {
                if (button == 0) { // Left click to drag
                    panel.dragging = true;
                    panel.dragX = (float) (mouseX - panel.x);
                    panel.dragY = (float) (mouseY - panel.y);
                    // Bring panel to front
                    PANELS.remove(i);
                    PANELS.add(panel);
                    return true;
                } else if (button == 1) { // Right click to collapse
                    panel.collapsed = !panel.collapsed;
                    return true;
                }
            }

            // Check module click (only if not collapsed and GUI is open)
            if (!panel.collapsed && scaleAnimation.getValue() > 0.8f) {
                panel.initModules();
                float currentY = panel.y + HEADER_HEIGHT;
                for (ModuleElement me : panel.modules) {
                    if (me.mouseClicked(mouseX, mouseY, button, panel.x, currentY, PANEL_WIDTH)) {
                        return true;
                    }
                    currentY += me.getHeight();
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (CategoryPanel panel : PANELS) {
            panel.dragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (bindingModule != null) {
            int key = event.key();
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setKey(0);
            } else {
                bindingModule.setKey(key);
            }
            bindingModule = null;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void onClose() {
        this.closing = true;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

