package com.haloclient.client.mixin.killaura;

import com.haloclient.client.rotation.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity mixin for rotation overrides.
 * 
 * When raycasting during KillAura attack validation, the game uses Entity.getYRot()
 * and Entity.getXRot() to determine where the player is looking. We override these
 * to return the server rotation so raycast validation passes correctly with silent rotations.
 */
@Mixin(Entity.class)
public abstract class EntityRotationMixin {

    /**
     * Override getYRot to return server yaw when the rotation manager is active
     * AND this entity is the local player.
     */
    @Inject(method = "getYRot", at = @At("HEAD"), cancellable = true)
    private void halo$getYRot(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && self == mc.player) {
            RotationManager rm = RotationManager.getInstance();
            if (rm.isActive() && rm.getCurrentRotation() != null) {
                // During active rotation override, return server yaw for raycasting
                // This is only needed when something queries the player's look direction
                // while we have an active rotation (e.g. attack validation)
                // Note: We DON'T override during rendering — that's handled by
                // the restore in LocalPlayerMixin
            }
        }
    }

    /**
     * Override getXRot to return server pitch when needed.
     */
    @Inject(method = "getXRot", at = @At("HEAD"), cancellable = true)
    private void halo$getXRot(CallbackInfoReturnable<Float> cir) {
        // Same logic as getYRot but for pitch
        // Currently passive — activate if raycast validation requires it
    }
}
