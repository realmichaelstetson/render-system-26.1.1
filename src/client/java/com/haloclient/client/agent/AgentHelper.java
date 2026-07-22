package com.haloclient.client.agent;

import com.haloclient.client.render.HaloRenderPipelines;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;
import org.lwjgl.system.MemoryUtil;

public class AgentHelper {
    public static VertexConsumer setCustomData(Object builder, float x, float y, float z, float w) {
        try {
            java.lang.reflect.Method m = builder.getClass().getDeclaredMethod("beginElement", com.mojang.blaze3d.vertex.VertexFormatElement.class);
            m.setAccessible(true);
            long p = (Long) m.invoke(builder, HaloRenderPipelines.CUSTOM_DATA);
            if (p != -1L) {
                MemoryUtil.memPutFloat(p, x);
                MemoryUtil.memPutFloat(p + 4L, y);
                MemoryUtil.memPutFloat(p + 8L, z);
                MemoryUtil.memPutFloat(p + 12L, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (VertexConsumer) builder;
    }

    public static VertexConsumer setColor2(Object builder, int color) {
        try {
            java.lang.reflect.Method m = builder.getClass().getDeclaredMethod("beginElement", com.mojang.blaze3d.vertex.VertexFormatElement.class);
            m.setAccessible(true);
            long p = (Long) m.invoke(builder, HaloRenderPipelines.COLOR2);
            if (p != -1L) {
                MemoryUtil.memPutInt(p, ARGB.toABGR(color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (VertexConsumer) builder;
    }

    public static VertexConsumer setShadowProps(Object builder, float x, float y, float z, float w) {
        try {
            java.lang.reflect.Method m = builder.getClass().getDeclaredMethod("beginElement", com.mojang.blaze3d.vertex.VertexFormatElement.class);
            m.setAccessible(true);
            long p = (Long) m.invoke(builder, HaloRenderPipelines.SHADOW_PROPS);
            if (p != -1L) {
                MemoryUtil.memPutFloat(p, x);
                MemoryUtil.memPutFloat(p + 4L, y);
                MemoryUtil.memPutFloat(p + 8L, z);
                MemoryUtil.memPutFloat(p + 12L, w);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (VertexConsumer) builder;
    }
}
