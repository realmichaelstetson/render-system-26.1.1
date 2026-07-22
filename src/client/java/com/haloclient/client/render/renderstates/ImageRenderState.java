package com.haloclient.client.render.renderstates;

import com.haloclient.client.render.HaloVertexConsumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * RenderState do renderowania obrazów (Identifier, URL, NativeImage)
 * z obsługą:
 * - Zaokrąglonych rogów (SDF rounded corners)
 * - Trzech trybów skalowania (FILL, FIT, STRETCH)
 * - Tintowania kolorem
 * - Poprawnego aspect ratio
 * - Filtrowaniem LINEAR dla najlepszej jakości
 *
 * Współpracuje z ImageManager do ładowania tekstur
 * i shaderem halo:core/image do renderowania.
 */
@Environment(EnvType.CLIENT)
public record ImageRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x, float y, float width, float height,
    int tintColor,
    float radius,
    float uMin, float vMin, float uMax, float vMax,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    /**
     * Tryb skalowania obrazu:
     * FILL    - Wypełnia cały prostokąt, przycina nadmiar (cover)
     * FIT     - Mieści cały obraz w prostokącie, dodaje marginesy (contain)
     * STRETCH - Rozciąga obraz do wymiarów prostokąta
     */
    public enum ScaleMode {
        FILL,
        FIT,
        STRETCH
    }

    // ============================================
    // === KONSTRUKTOR Z AUTOMATYCZNYM SKALOWANIEM
    // ============================================

    /**
     * Główny konstruktor - automatycznie oblicza UV na podstawie trybu skalowania.
     *
     * @param pipeline    Pipeline renderowania (HaloRenderPipelines.IMAGE)
     * @param textureSetup TextureSetup z ImageManager
     * @param pose        Macierz transformacji 2D
     * @param x           Pozycja X lewego górnego rogu
     * @param y           Pozycja Y lewego górnego rogu
     * @param width       Szerokość prostokąta docelowego
     * @param height      Wysokość prostokąta docelowego
     * @param tintColor   Kolor tintowania (0xFFFFFFFF = brak tintowania)
     * @param radius      Promień zaokrąglenia rogów (0 = ostre rogi)
     * @param scaleMode   Tryb skalowania (FILL, FIT, STRETCH)
     * @param imageWidth  Oryginalna szerokość obrazu w pikselach
     * @param imageHeight Oryginalna wysokość obrazu w pikselach
     * @param scissorArea Opcjonalny obszar przycinania
     */
    public ImageRenderState(
        final RenderPipeline pipeline,
        final TextureSetup textureSetup,
        final Matrix3x2fc pose,
        float x, float y, float width, float height,
        final int tintColor,
        final float radius,
        final ScaleMode scaleMode,
        final int imageWidth, final int imageHeight,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose,
            // Pozycja i wymiary (mogą być zmodyfikowane przez FIT)
            computeX(x, width, height, scaleMode, imageWidth, imageHeight),
            computeY(y, width, height, scaleMode, imageWidth, imageHeight),
            computeWidth(width, height, scaleMode, imageWidth, imageHeight),
            computeHeight(width, height, scaleMode, imageWidth, imageHeight),
            tintColor, radius,
            // UV coordinates (modyfikowane przez FILL)
            computeUMin(width, height, scaleMode, imageWidth, imageHeight),
            computeVMin(width, height, scaleMode, imageWidth, imageHeight),
            computeUMax(width, height, scaleMode, imageWidth, imageHeight),
            computeVMax(width, height, scaleMode, imageWidth, imageHeight),
            scissorArea,
            getBounds(
                computeX(x, width, height, scaleMode, imageWidth, imageHeight),
                computeY(y, width, height, scaleMode, imageWidth, imageHeight),
                computeWidth(width, height, scaleMode, imageWidth, imageHeight),
                computeHeight(width, height, scaleMode, imageWidth, imageHeight),
                pose, scissorArea
            )
        );
    }

    // ============================================
    // === UPROSZCZONY KONSTRUKTOR (FILL, BEZ TINTOWANIA)
    // ============================================

    /**
     * Uproszczony konstruktor - FILL mode, bez tintowania, bez zaokrągleń.
     */
    public ImageRenderState(
        final RenderPipeline pipeline,
        final TextureSetup textureSetup,
        final Matrix3x2fc pose,
        final float x, final float y, final float width, final float height,
        final int imageWidth, final int imageHeight,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, width, height,
            0xFFFFFFFF, 0.0f, ScaleMode.FILL, imageWidth, imageHeight, scissorArea);
    }

    // ============================================
    // === STRETCH KONSTRUKTOR (IGNORUJE ASPECT RATIO)
    // ============================================

    /**
     * Konstruktor STRETCH - rozciąga obraz do pełnych wymiarów, ignoruje aspect ratio.
     */
    public ImageRenderState(
        final RenderPipeline pipeline,
        final TextureSetup textureSetup,
        final Matrix3x2fc pose,
        final float x, final float y, final float width, final float height,
        final int tintColor,
        final float radius,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, width, height,
            tintColor, radius, 0.0f, 0.0f, 1.0f, 1.0f,
            scissorArea, getBounds(x, y, width, height, pose, scissorArea));
    }

    // ============================================
    // === OBLICZENIA SKALOWANIA (FILL MODE)
    // ============================================
    // FILL: Obraz wypełnia cały prostokąt, nadmiar jest przycinany.
    // Obliczamy UV tak, aby widoczna była centralna część obrazu.

    private static float computeUMin(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FILL) return 0.0f;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect > dstAspect) {
            // Obraz jest szerszy - przycinamy boki
            float scale = dstAspect / srcAspect;
            return (1.0f - scale) * 0.5f;
        }
        return 0.0f;
    }

    private static float computeVMin(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FILL) return 0.0f;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect < dstAspect) {
            // Obraz jest wyższy - przycinamy górę/dół
            float scale = srcAspect / dstAspect;
            return (1.0f - scale) * 0.5f;
        }
        return 0.0f;
    }

    private static float computeUMax(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FILL) return 1.0f;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect > dstAspect) {
            float scale = dstAspect / srcAspect;
            return 1.0f - (1.0f - scale) * 0.5f;
        }
        return 1.0f;
    }

    private static float computeVMax(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FILL) return 1.0f;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect < dstAspect) {
            float scale = srcAspect / dstAspect;
            return 1.0f - (1.0f - scale) * 0.5f;
        }
        return 1.0f;
    }

    // ============================================
    // === OBLICZENIA SKALOWANIA (FIT MODE)
    // ============================================
    // FIT: Cały obraz jest widoczny, prostokąt jest pomniejszony do aspect ratio obrazu.

    private static float computeX(float x, float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FIT) return x;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect < dstAspect) {
            // Obraz jest wyższy - dodajemy poziome marginesy
            float newWidth = h * srcAspect;
            return x + (w - newWidth) * 0.5f;
        }
        return x;
    }

    private static float computeY(float y, float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FIT) return y;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect > dstAspect) {
            // Obraz jest szerszy - dodajemy pionowe marginesy
            float newHeight = w / srcAspect;
            return y + (h - newHeight) * 0.5f;
        }
        return y;
    }

    private static float computeWidth(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FIT) return w;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect < dstAspect) {
            return h * srcAspect;
        }
        return w;
    }

    private static float computeHeight(float w, float h, ScaleMode mode, int imgW, int imgH) {
        if (mode != ScaleMode.FIT) return h;
        float srcAspect = (float) imgW / imgH;
        float dstAspect = w / h;
        if (srcAspect > dstAspect) {
            return w / srcAspect;
        }
        return h;
    }

    // ============================================
    // === BUDOWANIE WIERZCHOŁKÓW
    // ============================================

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = HaloVertexConsumer.of(vertexConsumer);

        float x0 = x;
        float y0 = y;
        float x1 = x + width;
        float y1 = y + height;

        // Quad: TL → BL → BR → TR (zgodny z innymi RenderState)
        addVertex(vertexConsumer, hv, x0, y0, 0.0f, 0.0f); // TL
        addVertex(vertexConsumer, hv, x0, y1, 0.0f, 1.0f); // BL
        addVertex(vertexConsumer, hv, x1, y1, 1.0f, 1.0f); // BR
        addVertex(vertexConsumer, hv, x1, y0, 1.0f, 0.0f); // TR
    }

    private void addVertex(VertexConsumer v, HaloVertexConsumer hv, float vx, float vy, float u, float vCoord) {
        v.addVertexWith2DPose(pose, vx, vy).setColor(tintColor).setUv(u, vCoord);
        // CustomData: [rectWidth, rectHeight, radius, 0]
        hv.setCustomData(width, height, radius, 0.0f);
        // Color2: unused
        hv.setColor2(0);
        // ShadowProps: [uMin, vMin, uMax, vMax] - zakres UV dla croppowania
        hv.setShadowProps(uMin, vMin, uMax, vMax);
    }

    // ============================================
    // === BOUNDS
    // ============================================

    @Nullable
    private static ScreenRectangle getBounds(float x, float y, float w, float h, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = (new ScreenRectangle((int) x, (int) y, (int) w, (int) h)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
