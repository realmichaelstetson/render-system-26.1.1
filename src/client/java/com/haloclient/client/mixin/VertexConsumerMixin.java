package com.haloclient.client.mixin;

import com.haloclient.client.render.HaloVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin extends HaloVertexConsumer {
    @Override
    default VertexConsumer setCustomData(float x, float y, float z, float w) { return (VertexConsumer) this; }

    @Override
    default VertexConsumer setColor2(int color) { return (VertexConsumer) this; }

    @Override
    default VertexConsumer setShadowProps(float x, float y, float z, float w) { return (VertexConsumer) this; }
}
