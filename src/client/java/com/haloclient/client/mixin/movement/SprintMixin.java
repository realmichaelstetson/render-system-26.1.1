package com.haloclient.client.mixin.movement;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.impl.movement.SprintModule;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class SprintMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void halo$onSprintAiStep(CallbackInfo ci) {
        if (HaloClient.INSTANCE == null || HaloClient.INSTANCE.moduleManager == null) return;

        SprintModule sprintModule = HaloClient.INSTANCE.moduleManager.getModule(SprintModule.class);
        if (sprintModule == null || !sprintModule.isEnabled()) return;

        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.input == null) return;

        if (sprintModule.isOmniSprint()) {
            if (halo$isOmniMovement(player) && !player.isShiftKeyDown()) {
                player.setSprinting(true);
            }
        } else {
            // Legit mode: match vanilla sprint requirements before aiStep movement calculation
            boolean hasEnoughFood = player.getFoodData().getFoodLevel() > 6.0F || player.getAbilities().instabuild;
            if (player.input.hasForwardImpulse()
                    && !player.isShiftKeyDown()
                    && !player.horizontalCollision
                    && hasEnoughFood
                    && !player.isUsingItem()
                    && !player.isPassenger()
                    && !player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)) {
                player.setSprinting(true);
            }
        }
    }

    @Unique
    private boolean halo$isOmniMovement(LocalPlayer player) {
        if (player.input == null || player.input.keyPresses == null) return false;
        return player.input.keyPresses.forward()
                || player.input.keyPresses.backward()
                || player.input.keyPresses.left()
                || player.input.keyPresses.right();
    }
}
