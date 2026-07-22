package com.haloclient.client.mixin;

import com.haloclient.client.render.HaloRenderPipelines;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexConsumer {

    @Shadow protected abstract long beginElement(VertexFormatElement element);

    public VertexConsumer setCustomData(float x, float y, float z, float w) {
        long p = this.beginElement(HaloRenderPipelines.CUSTOM_DATA);
        if (p != -1L) {
            MemoryUtil.memPutFloat(p, x);
            MemoryUtil.memPutFloat(p + 4L, y);
            MemoryUtil.memPutFloat(p + 8L, z);
            MemoryUtil.memPutFloat(p + 12L, w);
        }
        return this;
    }

    public VertexConsumer setColor2(int color) {
        long p = this.beginElement(HaloRenderPipelines.COLOR2);
        if (p != -1L) {
            MemoryUtil.memPutInt(p, ARGB.toABGR(color));
        }
        return this;
    }

    public VertexConsumer setShadowProps(float x, float y, float z, float w) {
        long p = this.beginElement(HaloRenderPipelines.SHADOW_PROPS);
        if (p != -1L) {
            MemoryUtil.memPutFloat(p, x);
            MemoryUtil.memPutFloat(p + 4L, y);
            MemoryUtil.memPutFloat(p + 8L, z);
            MemoryUtil.memPutFloat(p + 12L, w);
        }
        return this;
    }
}
