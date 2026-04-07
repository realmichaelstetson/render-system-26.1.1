package com.haloclient.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/**
 * Wariant BlurredRoundedRectangle, który renderuje dowolny czworokąt (quad) zamiast prostokąta.
 * sizeWidth i sizeHeight pozwalają na oszukanie SDF-a shadera w celu uniknięcia przerw (seams) między częściami wielokąta.
 */
@Environment(EnvType.CLIENT)
public record BlurredQuadRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    Matrix3x2fc pose,
    Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4,
    int color,
    float sizeWidth, float sizeHeight,
    float blurStrength,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    // Konstruktor z ręcznym rozmiarem (używany do fixowania przerw w 3D boxie)
    public BlurredQuadRenderState(
        RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
        Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4,
        int color, float sizeWidth, float sizeHeight,
        float blurStrength, @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, p1, p2, p3, p4, 
            color, sizeWidth, sizeHeight, blurStrength,
            scissorArea, calculateBounds(p1, p2, p3, p4, pose, scissorArea));
    }

    // Konstruktor domyślny (oblicza rozmiar z dystansu między punktami)
    public BlurredQuadRenderState(
        RenderPipeline pipeline, TextureSetup textureSetup, Matrix3x2fc pose,
        Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4,
        int color,
        float blurStrength, @Nullable ScreenRectangle scissorArea
    ) {
        this(pipeline, textureSetup, pose, p1, p2, p3, p4,
            color, p1.distance(p2), p2.distance(p3),
            blurStrength, scissorArea,
            calculateBounds(p1, p2, p3, p4, pose, scissorArea));
    }

    private static ScreenRectangle calculateBounds(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4, Matrix3x2fc pose, @Nullable ScreenRectangle scissor) {
        float minX = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        float minY = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        float maxX = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
        float maxY = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));
        
        ScreenRectangle bounds = (new ScreenRectangle((int)minX, (int)minY, (int)(maxX - minX), (int)(maxY - minY))).transformMaxBounds(pose);
        return scissor != null ? scissor.intersection(bounds) : bounds;
    }

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = (HaloVertexConsumer) vertexConsumer;
        addVertex(vertexConsumer, hv, p1.x, p1.y, 0.0f, 0.0f); // TL
        addVertex(vertexConsumer, hv, p2.x, p2.y, 0.0f, 1.0f); // BL
        addVertex(vertexConsumer, hv, p3.x, p3.y, 1.0f, 1.0f); // BR
        addVertex(vertexConsumer, hv, p4.x, p4.y, 1.0f, 0.0f); // TR
    }

    private void addVertex(VertexConsumer v, HaloVertexConsumer hv, float vx, float vy, float u, float vCoord) {
        v.addVertexWith2DPose(pose, vx, vy).setColor(color).setUv(u, vCoord);
        // Dodajemy margines 1000 do rozmiaru, aby krawędzie clippingu były głęboko wewnątrz SDF i nie tworzyły przerw
        // Radius hardkodowany na -1.0f zgodnie z życzeniem usera
        hv.setCustomData(sizeWidth + 1000.0f, sizeHeight + 1000.0f, -1.0f, 0.0f);
        hv.setColor2(0);
        // Bloom hardkodowany na 0.0f zgodnie z życzeniem usera
        hv.setShadowProps(blurStrength, 0.0f, 0f, 0f);
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
