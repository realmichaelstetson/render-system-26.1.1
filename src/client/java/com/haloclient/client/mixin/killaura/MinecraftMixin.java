package com.haloclient.client.mixin.killaura;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.impl.combat.KillAuraModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void halo$onStartAttack(CallbackInfoReturnable<Boolean> cir) {
        KillAuraModule aura = getKillAuraModule();
        if (aura != null) {
            if (aura.shouldBlockAttacks() || aura.shouldBlockMining()) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void halo$onContinueAttack(boolean leftClick, CallbackInfo ci) {
        KillAuraModule aura = getKillAuraModule();
        if (aura != null && aura.shouldBlockMining()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void halo$onStartUseItem(CallbackInfo ci) {
        KillAuraModule aura = getKillAuraModule();
        if (aura != null && aura.shouldBlockPlacing()) {
            ci.cancel();
        }
    }

    private KillAuraModule getKillAuraModule() {
        try {
            if (HaloClient.INSTANCE != null && HaloClient.INSTANCE.moduleManager != null) {
                return HaloClient.INSTANCE.moduleManager.getModule(KillAuraModule.class);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
