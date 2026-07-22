package com.haloclient.client;

import com.haloclient.client.render.renderstates.BlurredQuadRenderState;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.FontManager;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.util.FrameClock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.haloclient.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

public class HaloClient implements ClientModInitializer {

    private static final Animation rectAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 300L);
    private static float targetValue = 1.0f;
    
    public static HaloClient INSTANCE;
    public ModuleManager moduleManager;

    public static final net.minecraft.client.KeyMapping.Category HALO_CATEGORY = net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("halo", "halo"));

    public static final net.minecraft.client.KeyMapping openClickGuiKey = new net.minecraft.client.KeyMapping(
            "key.halo.clickgui",
            org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT,
            HALO_CATEGORY
    );

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        HaloRenderPipelines.init();
        moduleManager = new ModuleManager();

        net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(openClickGuiKey);

        // Client tick event — dispatches module ticks and ClickGUI keybind
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // ClickGUI keybind
            while (openClickGuiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new com.haloclient.client.gui.click.ClickGUI());
                }
            }

            // Tick all enabled modules
            if (client.player != null) {
                moduleManager.getModules().stream()
                        .filter(com.haloclient.client.module.Module::isEnabled)
                        .forEach(com.haloclient.client.module.Module::onTick);
            }
        });


    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static void renderModules(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        FrameClock.update();

        if (INSTANCE != null && INSTANCE.moduleManager != null) {
            INSTANCE.moduleManager.getModules().stream()
                    .filter(com.haloclient.client.module.Module::isEnabled)
                    .forEach(m -> m.onRender(graphics, deltaTracker));
        }
    }






    public static void initializeStandalone() {
        if (INSTANCE == null) {
            System.out.println("[HaloClient] Running standalone initialization...");
            INSTANCE = new HaloClient();
            HaloRenderPipelines.init();
            INSTANCE.moduleManager = new com.haloclient.client.module.ModuleManager();
        }
    }
}