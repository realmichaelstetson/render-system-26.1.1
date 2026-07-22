package com.haloclient.client.render.font;

import com.haloclient.client.render.HaloRenderPipelines;
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

@Environment(EnvType.CLIENT)
public record HaloFontRenderState(
    RenderPipeline pipeline,
    TextureSetup textureSetup,
    HaloFont font,
    MsdfFont msdfFont,
    String text,
    Matrix3x2fc pose,
    float x, float y,
    float size,
    int color,
    int color2,
    double speed,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    /**
     * Constructor for legacy HaloFont (STB bitmap font).
     */
    public HaloFontRenderState(HaloFont font, String text, Matrix3x2fc pose, float x, float y, int color, @Nullable ScreenRectangle scissorArea) {
        this(HaloRenderPipelines.FONT, font.getTextureSetup(), font, null, text, pose, x, y, 0, color, 0, 0.0, scissorArea, getBoundsLegacy(x, y, text, font, pose, scissorArea));
    }

    /**
     * Constructor for MSDF font — single solid color.
     */
    public HaloFontRenderState(MsdfFont msdfFont, String text, Matrix3x2fc pose, float x, float y, float size, int color, @Nullable ScreenRectangle scissorArea) {
        this(HaloRenderPipelines.MSDF_FONT, msdfFont.getTextureSetup(), null, msdfFont, text, pose, x, y, size, color, 0, 0.0, scissorArea, getBoundsMsdf(x, y, text, msdfFont, size, pose, scissorArea));
    }

    /**
     * Constructor for MSDF font — animated gradient colors.
     */
    public HaloFontRenderState(MsdfFont msdfFont, String text, Matrix3x2fc pose, float x, float y, float size, int color1, int color2, double speed, @Nullable ScreenRectangle scissorArea) {
        this(HaloRenderPipelines.MSDF_FONT, msdfFont.getTextureSetup(), null, msdfFont, text, pose, x, y, size, color1, color2, speed, scissorArea, getBoundsMsdf(x, y, text, msdfFont, size, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = HaloVertexConsumer.of(vertexConsumer);
        if (msdfFont != null) {
            if (color2 != 0 || speed > 0.0) {
                msdfFont.drawStringGradient(vertexConsumer, hv, pose, text, x, y, size, color, color2 != 0 ? color2 : color, speed);
            } else {
                msdfFont.drawString(vertexConsumer, hv, pose, text, x, y, size, color);
            }
        } else if (font != null) {
            font.drawString(vertexConsumer, hv, pose, text, x, y, color);
        }
    }

    @Nullable
    private static ScreenRectangle getBoundsLegacy(float x, float y, String text, HaloFont font, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        float width = font.getWidth(text);
        float height = font.getHeight();
        ScreenRectangle bounds = (new ScreenRectangle((int)x, (int)y, (int)width, (int)height)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Nullable
    private static ScreenRectangle getBoundsMsdf(float x, float y, String text, MsdfFont msdfFont, float size, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        float width = msdfFont.getWidth(text, size);
        float height = msdfFont.getHeight(size);
        ScreenRectangle bounds = (new ScreenRectangle((int)x, (int)y, (int)width, (int)height)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
