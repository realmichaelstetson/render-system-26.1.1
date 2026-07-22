package com.haloclient.client.mixin;

import com.haloclient.client.gui.HaloScreen;
import com.haloclient.client.HaloClient;
import com.haloclient.client.module.Module;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void halo$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();


       // System.out.println("DEBUG KEY EVENT: key=" + event.key() + ", action=" + action + ", screen=" + (mc.screen != null ? mc.screen.getClass().getSimpleName() : "null"));

        if (action != GLFW.GLFW_PRESS) return;
        if (mc.screen != null) {
          //  System.out.println("DEBUG KEY EVENT: mc.screen is not null, it is " + mc.screen.getClass().getSimpleName());
            return;
        }

        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
        //    System.out.println("DEBUG KEY EVENT: Right Shift pressed, setting screen to ClickGUI");
            mc.setScreen(new com.haloclient.client.gui.click.ClickGUI());
            return;
        }

        HaloClient.INSTANCE.getModuleManager().getModules().forEach(module -> {
            if (module.getKey() == event.key()) {
                module.toggle();
            }
        });
    }
}
