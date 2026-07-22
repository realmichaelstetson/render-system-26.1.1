package com.haloclient.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {

  

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void halo$onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {

    }

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void halo$onMove(CallbackInfo ci) {

    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void halo$onScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {

    }
}
