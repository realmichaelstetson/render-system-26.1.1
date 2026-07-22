package com.haloclient.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class HaloVertexConsumer {
    private final VertexConsumer delegate;

    public HaloVertexConsumer(VertexConsumer delegate) {
        this.delegate = delegate;
    }

    public static HaloVertexConsumer of(VertexConsumer consumer) {
        if (consumer == null) return null;
        return new HaloVertexConsumer(consumer);
    }

    public VertexConsumer getDelegate() {
        return delegate;
    }

    public VertexConsumer setCustomData(float x, float y, float z, float w) {
        try {
            java.lang.reflect.Method m = delegate.getClass().getMethod("setCustomData", float.class, float.class, float.class, float.class);
            return (VertexConsumer) m.invoke(delegate, x, y, z, w);
        } catch (Exception e) {
            return com.haloclient.client.agent.AgentHelper.setCustomData(delegate, x, y, z, w);
        }
    }

    public VertexConsumer setColor2(int color) {
        try {
            java.lang.reflect.Method m = delegate.getClass().getMethod("setColor2", int.class);
            return (VertexConsumer) m.invoke(delegate, color);
        } catch (Exception e) {
            return com.haloclient.client.agent.AgentHelper.setColor2(delegate, color);
        }
    }

    public VertexConsumer setShadowProps(float x, float y, float z, float w) {
        try {
            java.lang.reflect.Method m = delegate.getClass().getMethod("setShadowProps", float.class, float.class, float.class, float.class);
            return (VertexConsumer) m.invoke(delegate, x, y, z, w);
        } catch (Exception e) {
            return com.haloclient.client.agent.AgentHelper.setShadowProps(delegate, x, y, z, w);
        }
    }
}
