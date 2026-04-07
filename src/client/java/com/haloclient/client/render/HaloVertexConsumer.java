package com.haloclient.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public interface HaloVertexConsumer {
    VertexConsumer setCustomData(float x, float y, float z, float w);
    VertexConsumer setColor2(int color);
    VertexConsumer setShadowProps(float x, float y, float z, float w);
    
    static HaloVertexConsumer of(VertexConsumer consumer) {
        return (HaloVertexConsumer) consumer;
    }
}
