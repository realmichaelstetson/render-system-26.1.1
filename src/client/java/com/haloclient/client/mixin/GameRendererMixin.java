package com.haloclient.client.mixin;

import com.haloclient.client.render.CaptureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void halo$onAfterRender(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
        // Na samym końcu klatki, po wszystkim - idealne dla NanoVG
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void halo$onAfterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        // Po wyrenderowaniu poziomu, ale przed GUI
        CaptureManager.updateCapture(Minecraft.getInstance());
    }

}
