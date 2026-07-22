package com.haloclient.client.mixin.movement;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.impl.movement.SprintModule;
import net.minecraft.client.player.ClientInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientInput.class)
public abstract class SprintInputMixin {

    @Inject(method = "hasForwardImpulse", at = @At("HEAD"), cancellable = true)
    private void halo$onHasForwardImpulse(CallbackInfoReturnable<Boolean> cir) {
        if (halo$isOmniSprint()) {
            ClientInput input = (ClientInput) (Object) this;
            if (halo$isOmniMovement(input)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private boolean halo$isOmniSprint() {
        if (HaloClient.INSTANCE == null || HaloClient.INSTANCE.moduleManager == null) return false;
        SprintModule sprint = HaloClient.INSTANCE.moduleManager.getModule(SprintModule.class);
        return sprint != null && sprint.isOmniSprint();
    }

    @Unique
    private boolean halo$isOmniMovement(ClientInput input) {
        if (input.keyPresses == null) return false;
        return input.keyPresses.forward()
                || input.keyPresses.backward()
                || input.keyPresses.left()
                || input.keyPresses.right();
    }
}
