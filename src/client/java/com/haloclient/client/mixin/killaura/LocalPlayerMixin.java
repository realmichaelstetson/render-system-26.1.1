package com.haloclient.client.mixin.killaura;

import com.haloclient.client.event.EventBus;
import com.haloclient.client.event.events.MotionEvent;
import com.haloclient.client.event.events.TickEvent;
import com.haloclient.client.rotation.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Core mixin for KillAura silent rotations and motion events.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void halo$onTickHead(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        RotationManager rm = RotationManager.getInstance();

        EventBus.getInstance().post(new TickEvent());
        rm.update();

        if (rm.isActive()) {
            rm.applyRotation(self);
            EventBus.getInstance().post(new MotionEvent(
                    MotionEvent.State.PRE,
                    rm.getServerYaw(),
                    rm.getServerPitch(),
                    self.onGround()
            ));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void halo$onTickReturn(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        RotationManager rm = RotationManager.getInstance();

        if (rm.isActive()) {
            EventBus.getInstance().post(new MotionEvent(
                    MotionEvent.State.POST,
                    rm.getServerYaw(),
                    rm.getServerPitch(),
                    self.onGround()
            ));
            rm.restoreRotation(self);
        }
    }
}
