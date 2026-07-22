package com.haloclient.client.mixin.killaura;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.impl.combat.KillAuraModule;
import com.haloclient.client.movement.MovementFix;
import com.haloclient.client.rotation.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void halo$fixSilentMovement(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        RotationManager rm = RotationManager.getInstance();
        if (mc.player == null || !rm.isActive()) return;

        MovementFix.Mode mode = getMovementFixMode();
        if (mode == MovementFix.Mode.OFF) return;

        Input fixed = MovementFix.correctInput(this.keyPresses, rm.getClientYaw(), rm.getServerYaw(), mode);
        this.keyPresses = fixed;

        float forward = impulse(fixed.forward(), fixed.backward());
        float sideways = impulse(fixed.left(), fixed.right());
        ((ClientInputAccessor) this).halo$setMoveVector(new Vec2(sideways, forward));
    }

    private MovementFix.Mode getMovementFixMode() {
        try {
            if (HaloClient.INSTANCE == null || HaloClient.INSTANCE.moduleManager == null) {
                return MovementFix.Mode.OFF;
            }
            KillAuraModule aura = HaloClient.INSTANCE.moduleManager.getModule(KillAuraModule.class);
            if (aura != null && aura.isEnabled() && aura.isSilentRotations()) {
                return aura.getMovementFixMode();
            }
        } catch (Exception ignored) {}
        return MovementFix.Mode.OFF;
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) return 0.0f;
        return positive ? 1.0f : -1.0f;
    }
}
