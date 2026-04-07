package com.haloclient.client.render;

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
 * Wariant RoundedRectangle, który zamiast koloru używa tekstury z przechwyconym tłem
 * i w shaderze wykonuje gaussian blur. shadowProps.x niesie blurStrength, shadowProps.y bloom.
 */
@Environment(EnvType.CLIENT)
public record BlurredRoundedRectangleRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    float x, float y, float width, float height,
    int color, int color2,
    float radius, float borderThickness,
    float blurStrength, float bloom,
    float gradientAngle,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    // Konstruktor dla unikalnego koloru (wsteczna kompatybilność)
    public BlurredRoundedRectangleRenderState(
        final RenderPipeline pipeline,
        final TextureSetup textureSetup,
        final Matrix3x2fc pose,
        final float x, final float y, final float width, final float height,
        final int color,
        final float radius,
        final float blurStrength,
        final float bloom,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, width, height,
            color, color,
            radius, 0.0f,
            blurStrength, bloom, 0.0f,
            scissorArea,
            getBounds(x, y, width, height, pose, scissorArea));
    }

    // Konstruktor dla gradientu
    public BlurredRoundedRectangleRenderState(
        final RenderPipeline pipeline,
        final TextureSetup textureSetup,
        final Matrix3x2fc pose,
        final float x, final float y, final float width, final float height,
        final int color, final int color2,
        final float radius,
        final float blurStrength,
        final float bloom,
        final float gradientAngle,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, x, y, width, height,
            color, color2,
            radius, 0.0f,
            blurStrength, bloom, gradientAngle,
            scissorArea,
            getBounds(x, y, width, height, pose, scissorArea));
    }

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = (HaloVertexConsumer) vertexConsumer;
        // Powiększamy geometrię o promień blooma (rozmycia cienia), by móc wyrenderować go poza granicami prostokąta.
        float renderX = x - bloom;
        float renderY = y - bloom;
        float renderX1 = x + width + bloom;
        float renderY1 = y + height + bloom;

        addVertex(vertexConsumer, hv, renderX, renderY, 0.0f, 0.0f); // TL
        addVertex(vertexConsumer, hv, renderX, renderY1, 0.0f, 1.0f); // BL
        addVertex(vertexConsumer, hv, renderX1, renderY1, 1.0f, 1.0f); // BR
        addVertex(vertexConsumer, hv, renderX1, renderY, 1.0f, 0.0f); // TR
    }

    private void addVertex(VertexConsumer v, HaloVertexConsumer hv, float vx, float vy, float u, float vCoord) {
        // Kolor ustawiamy na biały (shadery pobierają barwę z tekstury i miksują z FragColor)
        v.addVertexWith2DPose(pose, vx, vy).setColor(color).setUv(u, vCoord);
        // CustomData: [width, height, radius, borderThickness]
        hv.setCustomData(width, height, radius, borderThickness);
        hv.setColor2(color2);
        // ShadowProps: [blurStrength, bloom, gradientAngle, 0]
        hv.setShadowProps(blurStrength, bloom, gradientAngle, 0f);
    }

    @Nullable
    private static ScreenRectangle getBounds(final float x, final float y, final float width, final float height, final Matrix3x2fc pose, @Nullable final ScreenRectangle scissorArea) {
        ScreenRectangle bounds = (new ScreenRectangle((int)x, (int)y, (int)width, (int)height)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
