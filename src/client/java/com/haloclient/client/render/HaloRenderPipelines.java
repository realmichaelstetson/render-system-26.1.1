package com.haloclient.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class HaloRenderPipelines {
    
    public static final VertexFormatElement CUSTOM_DATA = VertexFormatElement.register(7, 0, VertexFormatElement.Type.FLOAT, false, 4);
    public static final VertexFormatElement COLOR2 = VertexFormatElement.register(8, 0, VertexFormatElement.Type.UBYTE, true, 4);
    public static final VertexFormatElement SHADOW_PROPS = VertexFormatElement.register(9, 0, VertexFormatElement.Type.FLOAT, false, 4);
    
    public static final VertexFormat ROUNDED_RECT_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("CustomData", CUSTOM_DATA)
            .add("Color2", COLOR2)
            .add("ShadowProps", SHADOW_PROPS)
            .build();

    public static final RenderPipeline ROUNDED_RECT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("halo", "pipeline/rounded_rect"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("halo", "core/rounded_rect"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("halo", "core/rounded_rect"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline ROUNDED_BLUR = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("halo", "pipeline/rounded_blur"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("halo", "core/rounded_blur"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("halo", "core/rounded_blur"))
                    .withSampler("Sampler0")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static final RenderPipeline FONT = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("halo", "pipeline/font"))
                    .withVertexShader(Identifier.fromNamespaceAndPath("halo", "core/font"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("halo", "core/font"))
                    .withSampler("Sampler0")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withVertexFormat(ROUNDED_RECT_FORMAT, VertexFormat.Mode.QUADS)
                    .build()
    );

    public static void init() {
    }
}
