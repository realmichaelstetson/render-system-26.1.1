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
    String text,
    Matrix3x2fc pose,
    float x, float y,
    int color,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public HaloFontRenderState(HaloFont font, String text, Matrix3x2fc pose, float x, float y, int color, @Nullable ScreenRectangle scissorArea) {
        this(HaloRenderPipelines.FONT, font.getTextureSetup(), font, text, pose, x, y, color, scissorArea, getBounds(x, y, text, font, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = (HaloVertexConsumer) vertexConsumer;
        font.drawString(vertexConsumer, hv, pose, text, x, y, color);
    }

    @Nullable
    private static ScreenRectangle getBounds(float x, float y, String text, HaloFont font, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        float width = font.getWidth(text);
        float height = font.getHeight();
        ScreenRectangle bounds = (new ScreenRectangle((int)x, (int)y, (int)width, (int)height)).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }

    @Override public RenderPipeline pipeline() { return pipeline; }
    @Override public TextureSetup textureSetup() { return textureSetup; }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
