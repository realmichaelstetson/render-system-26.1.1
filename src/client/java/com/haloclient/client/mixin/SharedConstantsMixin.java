package com.haloclient.client.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

@Mixin(SharedConstants.class)
public class SharedConstantsMixin {
    @Shadow private static WorldVersion CURRENT_VERSION;

    @Inject(method = "getCurrentVersion", at = @At("HEAD"), cancellable = true)
    private static void onGetCurrentVersion(CallbackInfoReturnable<WorldVersion> cir) {
        if (CURRENT_VERSION == null) {
            System.out.println("[ANTIGRAVITY DEBUG] SharedConstants.getCurrentVersion() called but CURRENT_VERSION is null. Attempting detection...");
            
            // Try calling SharedConstants.tryDetectVersion()
            try {
                SharedConstants.tryDetectVersion();
            } catch (Throwable t) {
                System.out.println("[ANTIGRAVITY DEBUG] SharedConstants.tryDetectVersion() failed: " + t);
            }
            
            if (CURRENT_VERSION == null) {
                // Let's reflectively check DetectedVersion for tryDetect, BUILT_IN, or other version source
                try {
                    Class<?> detectedVersionClass = Class.forName("net.minecraft.DetectedVersion");
                    System.out.println("[ANTIGRAVITY DEBUG] DetectedVersion class found. Listing methods and fields:");
                    for (Method m : detectedVersionClass.getDeclaredMethods()) {
                        System.out.println("  Method: " + m.toString());
                    }
                    for (Field f : detectedVersionClass.getDeclaredFields()) {
                        System.out.println("  Field: " + f.toString());
                    }
                    
                    // Let's look for a static field of type WorldVersion or DetectedVersion
                    for (Field f : detectedVersionClass.getDeclaredFields()) {
                        if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && 
                            (WorldVersion.class.isAssignableFrom(f.getType()) || f.getType().getName().contains("Version"))) {
                            f.setAccessible(true);
                            CURRENT_VERSION = (WorldVersion) f.get(null);
                            System.out.println("[ANTIGRAVITY DEBUG] Found static version field: " + f.getName() + " = " + CURRENT_VERSION);
                            if (CURRENT_VERSION != null) {
                                break;
                            }
                        }
                    }
                    
                    // If still null, try finding static method
                    if (CURRENT_VERSION == null) {
                        for (Method m : detectedVersionClass.getDeclaredMethods()) {
                            if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && 
                                m.getParameterCount() == 0 && 
                                WorldVersion.class.isAssignableFrom(m.getReturnType())) {
                                m.setAccessible(true);
                                CURRENT_VERSION = (WorldVersion) m.invoke(null);
                                System.out.println("[ANTIGRAVITY DEBUG] Invoked static version method: " + m.getName() + " = " + CURRENT_VERSION);
                                if (CURRENT_VERSION != null) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("[ANTIGRAVITY DEBUG] Reflective detection failed: " + t);
                    t.printStackTrace();
                }
            }
            
            if (CURRENT_VERSION != null) {
                cir.setReturnValue(CURRENT_VERSION);
            } else {
                System.out.println("[ANTIGRAVITY DEBUG] CURRENT_VERSION is still null. Cannot bypass!");
            }
        }
    }
}
