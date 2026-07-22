package com.haloclient.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class SiemaMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void halo$onRenderSiema(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if ("true".equals(System.getProperty("halo.injected"))) {
            System.out.println("SIEMA FROM DYNAMIC MIXIN!");
        }
    }
}
