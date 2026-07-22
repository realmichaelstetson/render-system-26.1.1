package com.haloclient.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    default VertexConsumer setCustomData(float x, float y, float z, float w) { return (VertexConsumer) this; }
    default VertexConsumer setColor2(int color) { return (VertexConsumer) this; }
    default VertexConsumer setShadowProps(float x, float y, float z, float w) { return (VertexConsumer) this; }
}
