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

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void halo$onAfterRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        // Po wyrenderowaniu poziomu, ale przed GUI - i co najważniejsze na Render Thread!
        CaptureManager.updateCapture(Minecraft.getInstance());
    }

}
