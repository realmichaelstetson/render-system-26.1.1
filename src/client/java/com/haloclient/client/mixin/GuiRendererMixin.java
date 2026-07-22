package com.haloclient.client.mixin;

import com.haloclient.client.render.CaptureManager;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {

    @Inject(
        method = "executeDrawRange",
        at = @At("HEAD")
    )
    private void halo$onExecuteDrawRange(
        Supplier<String> nameSupplier,
        RenderTarget target,
        GpuBufferSlice slice1,
        GpuBufferSlice slice2,
        GpuBuffer buffer,
        VertexFormat.IndexType indexType,
        int start,
        int end,
        CallbackInfo ci
    ) {
        if (nameSupplier != null && "GUI after blur".equalsIgnoreCase(nameSupplier.get())) {
            CaptureManager.updateCapture(Minecraft.getInstance());
        }
    }
}
