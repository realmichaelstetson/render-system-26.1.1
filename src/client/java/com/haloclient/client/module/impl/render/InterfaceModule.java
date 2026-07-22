package com.haloclient.client.module.impl.render;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.module.property.ColorPickerProperty;
import com.haloclient.client.module.property.GroupProperty;
import com.haloclient.client.module.property.NumberProperty;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.font.MsdfFontManager;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import com.haloclient.client.render.ImageManager;
import com.haloclient.client.render.renderstates.ImageRenderState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class InterfaceModule extends Module {

    private final BooleanProperty watermark = new BooleanProperty("Enabled", "Displays client watermark in top left corner", true);
    private final BooleanProperty showFps = new BooleanProperty("Show FPS", "Displays current FPS in watermark", true);
    private final BooleanProperty showPing = new BooleanProperty("Show Ping", "Displays current latency in watermark", true);
    private final BooleanProperty showServer = new BooleanProperty("Show Server IP", "Displays current server IP in watermark", true);

    private final GroupProperty watermarkGroup = new GroupProperty("Watermark").addProperties(watermark, showFps, showPing, showServer);

    private final BooleanProperty moduleList = new BooleanProperty("Enabled", "Displays active modules list in top right corner", true);
    private final NumberProperty listScale = new NumberProperty("List Scale", "Scale factor for ModuleList HUD element", 0.5, 2.0, 0.8, 0.05);
    private final BooleanProperty lowercase = new BooleanProperty("Lowercase", "Renders module list text in lowercase", false);
    private final ColorPickerProperty textColors = new ColorPickerProperty("Text Color", "Primary and Secondary colors for module text gradient", ARGB.color(255, 255, 255, 255), ARGB.color(255, 255, 0, 0));
    private final NumberProperty animSpeed = new NumberProperty("Anim Speed", "Speed of text color gradient loop animation", 0.1, 5.0, 1.0, 0.1);

    private final GroupProperty moduleListGroup = new GroupProperty("Module List").addProperties(moduleList, listScale, lowercase, textColors, animSpeed);

    private final Map<Module, Animation> moduleAnimations = new HashMap<>();
    private final Animation watermarkWidthAnimation = new Animation(Easing.EASE_OUT_CUBIC, 250L);

    public InterfaceModule() {
        super("Interface", "HUD interface overlay with Watermark and ModuleList", Category.VISUAL);
        addProperties(watermarkGroup, moduleListGroup);
        setEnabled(true);
    }

    public BooleanProperty getWatermark() {
        return watermark;
    }

    public BooleanProperty getShowFps() {
        return showFps;
    }

    public BooleanProperty getShowPing() {
        return showPing;
    }

    public BooleanProperty getShowServer() {
        return showServer;
    }

    public GroupProperty getWatermarkGroup() {
        return watermarkGroup;
    }

    public BooleanProperty getModuleList() {
        return moduleList;
    }

    public NumberProperty getListScale() {
        return listScale;
    }

    public BooleanProperty getLowercase() {
        return lowercase;
    }

    public ColorPickerProperty getTextColors() {
        return textColors;
    }

    public NumberProperty getAnimSpeed() {
        return animSpeed;
    }

    public GroupProperty getModuleListGroup() {
        return moduleListGroup;
    }

    @Override
    public void onRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (mc.options.hideGui) {
            return;
        }

        // CaptureManager.prepareBlurLayer(graphics);

        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissorArea = graphics.scissorStack.peek();

        if (watermark.getValue()) {
            renderWatermark(graphics, pose, scissorArea);
        }

        if (moduleList.getValue()) {
            renderModuleList(graphics, pose, scissorArea);
        }
    }

    private record TextSegment(String text, int color, boolean isBold) {}

    private void renderWatermark(GuiGraphicsExtractor graphics, Matrix3x2f pose, ScreenRectangle scissorArea) {
        MsdfFont boldFont = MsdfFontManager.getFont("inter-semibold", 9f);
        MsdfFont regularFont = MsdfFontManager.getFont("inter-regular", 11f);

        Identifier logoId = Identifier.fromNamespaceAndPath("halo", "cwe23424.png");
        ImageManager.CachedImage logoImage = ImageManager.fromIdentifier(logoId);

        List<TextSegment> segments = new ArrayList<>();
        segments.add(new TextSegment("v0.1", ARGB.color(255, 200, 205, 215), true));

        if (showFps.getValue()) {
            int fps = mc.getFps();
            segments.add(new TextSegment(" | ", ARGB.color(255, 100, 105, 115), false));
            segments.add(new TextSegment(fps + " fps", ARGB.color(255, 200, 205, 215), true));
        }

        if (showPing.getValue()) {
            int ping = 0;
            if (mc.getConnection() != null && mc.player != null) {
                var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (entry != null) {
                    ping = entry.getLatency();
                }
            }
            segments.add(new TextSegment(" | ", ARGB.color(255, 100, 105, 115), false));
            segments.add(new TextSegment(ping + " ms", ARGB.color(255, 230, 235, 245), true));
        }

        if (showServer.getValue()) {
            String serverIp = "singleplayer";
            if (mc.getCurrentServer() != null && mc.getCurrentServer().ip != null && !mc.getCurrentServer().ip.isEmpty()) {
                serverIp = mc.getCurrentServer().ip.toLowerCase();
            }
            segments.add(new TextSegment(" | ", ARGB.color(255, 100, 105, 115), false));
            segments.add(new TextSegment(serverIp, ARGB.color(255, 230, 235, 245), true));
        }

        float fontSize = 9f;
        float logoH = 16f;
        float logoW = (logoImage != null) ? logoH * logoImage.aspectRatio() : 16f;

        float textTotalW = 0f;
        for (TextSegment seg : segments) {
            MsdfFont font = seg.isBold() ? (boldFont != null ? boldFont : regularFont) : regularFont;
            if (font != null) {
                textTotalW += font.getWidth(seg.text(), fontSize);
            }
        }

        float targetW = logoW + 6f + textTotalW + 12f;
        if (watermarkWidthAnimation.getValue() == 0.0f) {
            watermarkWidthAnimation.setValue(targetW);
        }
        watermarkWidthAnimation.run(targetW);
        float totalW = watermarkWidthAnimation.getValue();
        float cardH = 17f;

        float x = 8f;
        float y = 8f;

        ScreenRectangle cardScissor = new ScreenRectangle((int) x, (int) y, Math.max(1, (int) totalW), (int) cardH);
        ScreenRectangle effectiveScissor = scissorArea != null ? scissorArea.intersection(cardScissor) : cardScissor;

        // Blurred Background card
        graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_BLUR,
                CaptureManager.getCaptureTextureSetup(),
                pose,
                x, y, totalW, cardH,
                ARGB.color(120, 15, 17, 23),
                5f,
                10.0f,
                0.0f,
                scissorArea
        ));

        float currentX = x + 6f;

        // Logo image
        if (logoImage != null) {
            float logoY = y + (cardH - logoH) / 2f;
            graphics.guiRenderState.addGuiElement(new ImageRenderState(
                    HaloRenderPipelines.IMAGE,
                    logoImage.textureSetup(),
                    pose,
                    currentX, logoY, logoW, logoH,
                    0xFFFFFFFF,
                    0.0f,
                    effectiveScissor
            ));
            currentX += logoW + 6f;
        }

        // Render text segments
        for (TextSegment seg : segments) {
            MsdfFont font = seg.isBold() ? (boldFont != null ? boldFont : regularFont) : regularFont;
            if (font != null) {
                float segW = font.getWidth(seg.text(), fontSize);
                float segY = y + (cardH - font.getHeight(fontSize)) / 2f;
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        font,
                        seg.text(),
                        pose,
                        currentX, segY, fontSize,
                        seg.color(),
                        effectiveScissor
                ));
                currentX += segW;
            }
        }
    }

    private void renderModuleList(GuiGraphicsExtractor graphics, Matrix3x2f pose, ScreenRectangle scissorArea) {
        if (HaloClient.INSTANCE == null || HaloClient.INSTANCE.getModuleManager() == null) {
            return;
        }

        MsdfFont nameFont = MsdfFontManager.getFont("productsans-bold", 10f);
        MsdfFont suffixFont = MsdfFontManager.getFont("inter-regular", 10f);

        if (nameFont == null) {
            return;
        }

        float nameFontSize = 11f;
        float suffixFontSize = 10f;

        List<Module> allModules = HaloClient.INSTANCE.getModuleManager().getModules();

        // 1. Update disappearing modules
        Iterator<Map.Entry<Module, Animation>> iterator = moduleAnimations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Module, Animation> entry = iterator.next();
            Module module = entry.getKey();
            Animation anim = entry.getValue();

            if (!module.isEnabled() || module == this || module.getName().equalsIgnoreCase("ClickGUI")) {
                anim.run(0.0f);
                if (anim.getValue() <= 0.001f && anim.isFinished()) {
                    iterator.remove();
                }
            }
        }

        // 2. Update appearing modules
        for (Module module : allModules) {
            if (!module.isEnabled() || module == this || module.getName().equalsIgnoreCase("ClickGUI")) {
                continue;
            }

            Animation anim = moduleAnimations.computeIfAbsent(module, k -> {
                Animation a = new Animation(Easing.EASE_OUT_QUART, 250L);
                a.setValue(0.0f);
                return a;
            });
            anim.run(1.0f);
        }

        // 3. Collect active and animating module data
        List<ModuleData> activeModules = new ArrayList<>();
        for (Map.Entry<Module, Animation> entry : moduleAnimations.entrySet()) {
            Module module = entry.getKey();
            Animation anim = entry.getValue();
            float progress = anim.getValue();
            if (progress <= 0.001f) {
                continue;
            }

            boolean isLowercase = lowercase.getValue();
            String name = isLowercase ? module.getName().toLowerCase() : module.getName();
            String suffix = module.getSuffix();
            if (suffix != null && isLowercase) {
                suffix = suffix.toLowerCase();
            }

            float nameW = nameFont.getWidth(name, nameFontSize);
            float suffixW = (suffix != null && !suffix.isEmpty() && suffixFont != null)
                    ? suffixFont.getWidth(" - " + suffix, suffixFontSize)
                    : 0f;

            float totalWidth = nameW + suffixW;
            activeModules.add(new ModuleData(module, name, suffix, nameW, totalWidth, progress));
        }

        // Sort descending by total width
        activeModules.sort(Comparator.comparingDouble(ModuleData::totalWidth).reversed());

        float scale = listScale.getValue().floatValue();
        Matrix3x2f listPose = new Matrix3x2f(pose);
        if (Math.abs(scale - 1.0f) > 0.001f) {
            float pivotX = graphics.guiWidth() - 8.0f;
            float pivotY = 8.0f;
            listPose.translate(pivotX, pivotY);
            listPose.scale(scale);
            listPose.translate(-pivotX, -pivotY);
        }

        float screenW = graphics.guiWidth();
        float currentY = 8f;
        float rowHeight = 18f;
        float paddingX = 6f;
        float rightMargin = 8f;

        int totalCount = activeModules.size();
        float barX = screenW - rightMargin - 2f;
        float startY = 8f;

        int c1 = textColors.getColor1();
        int c2 = textColors.isDual() ? textColors.getColor2() : c1;

        double speed = animSpeed.getValue();
        long time = System.currentTimeMillis();
        double timeSec = (time % 1000000L) / 1000.0;

        for (int i = 0; i < totalCount; i++) {
            ModuleData data = activeModules.get(i);
            float progress = data.progress();

            float slideOffset = (1.0f - progress) * 25.0f;
            float effectiveRowH = (rowHeight - 1f) * progress;

            float itemW = data.totalWidth() + paddingX * 2;
            float x = screenW - rightMargin - itemW + slideOffset;
            float y = currentY;

            float itemCornerMask = (i == 0) ? 1.0f : 5.0f;

            int bgAlpha = (int) (120 * progress);
            int textAlpha1 = (int) (ARGB.alpha(c1) * progress);
            int textAlpha2 = (int) (ARGB.alpha(c2) * progress);
            int animC1 = ARGB.color(textAlpha1, ARGB.red(c1), ARGB.green(c1), ARGB.blue(c1));
            int animC2 = ARGB.color(textAlpha2, ARGB.red(c2), ARGB.green(c2), ARGB.blue(c2));

            // Blurred Background rect
            if (bgAlpha > 1 && progress * rowHeight >= 1.0f) {
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        CaptureManager.getCaptureTextureSetup(),
                        listPose,
                        x, y, itemW, rowHeight * progress,
                        ARGB.color(bgAlpha, 15, 17, 23),
                        5f,
                        10.0f,
                        0.0f,
                        itemCornerMask,
                        scissorArea
                ));
            }

            // Module name text (Per-character horizontal animated color gradient wave)
            float textX = x + paddingX;
            float textY = y + (rowHeight - nameFont.getHeight(nameFontSize)) / 2f - 0.5f;

            if (progress > 0.05f) {
                graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                        nameFont,
                        data.name(),
                        listPose,
                        textX, textY, nameFontSize,
                        animC1, animC2, speed,
                        scissorArea
                ));

                // Suffix text
                if (data.suffix() != null && !data.suffix().isEmpty() && suffixFont != null) {
                    float suffixX = textX + data.nameWidth();
                    float suffixY = y + (rowHeight - suffixFont.getHeight(suffixFontSize)) / 2f;
                    int suffixAlpha = (int) (255 * progress);

                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            suffixFont,
                            " - " + data.suffix(),
                            listPose,
                            suffixX, suffixY, suffixFontSize,
                            ARGB.color(suffixAlpha, 170, 175, 185),
                            scissorArea
                    ));
                }
            }

            currentY += effectiveRowH;
        }

        // Render ONE single continuous vertical accent bar on the right OVERLAYING all modules
        if (currentY > startY + 0.1f) {
            float listH = (currentY - startY) + 1.0f;
            int barColorTop;
            int barColorBottom;
            if (c1 != c2) {
                double phaseTop = (-timeSec * 3.0 * speed);
                double phaseBottom = (-timeSec * 3.0 * speed) + (totalCount * 0.35);
                float factorTop = (float) (Math.sin(phaseTop) * 0.5 + 0.5);
                float factorBottom = (float) (Math.sin(phaseBottom) * 0.5 + 0.5);
                barColorTop = interpolateColor(c1, c2, factorTop);
                barColorBottom = interpolateColor(c1, c2, factorBottom);
            } else {
                float[] hsb = java.awt.Color.RGBtoHSB(ARGB.red(c1), ARGB.green(c1), ARGB.blue(c1), null);
                double hueShiftTop = (-timeSec * 0.6 * speed);
                double hueShiftBottom = (-timeSec * 0.6 * speed + totalCount * 0.12);
                float finalHueTop = (float) (((hsb[0] + hueShiftTop) % 1.0 + 1.0) % 1.0);
                float finalHueBottom = (float) (((hsb[0] + hueShiftBottom) % 1.0 + 1.0) % 1.0);
                barColorTop = java.awt.Color.HSBtoRGB(finalHueTop, Math.max(0.6f, hsb[1]), hsb[2]);
                barColorBottom = java.awt.Color.HSBtoRGB(finalHueBottom, Math.max(0.6f, hsb[1]), hsb[2]);
            }

            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    listPose,
                    barX, startY, 2.5f, listH,
                    barColorTop, barColorBottom,
                    0f, 0.0f, 0.0f,
                    scissorArea
            ));
        }
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int a1 = ARGB.alpha(color1), r1 = ARGB.red(color1), g1 = ARGB.green(color1), b1 = ARGB.blue(color1);
        int a2 = ARGB.alpha(color2), r2 = ARGB.red(color2), g2 = ARGB.green(color2), b2 = ARGB.blue(color2);

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return ARGB.color(a, r, g, b);
    }

    private record ModuleData(Module module, String name, String suffix, float nameWidth, float totalWidth, float progress) {}
}

