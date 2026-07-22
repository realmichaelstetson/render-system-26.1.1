package com.haloclient.client.mixin.killaura;

import com.haloclient.client.event.EventBus;
import com.haloclient.client.event.events.AttackEvent;
import com.haloclient.client.module.impl.combat.KillAuraModule;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into MultiPlayerGameMode (the client interaction manager).
 * Hooks the attack method to dispatch AttackEvents for all attacks
 * (both from KillAura and manual player attacks).
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    /**
     * Fires a PRE attack event before the attack is processed.
     * Can be cancelled to prevent manual player attacks when KillAura requires it.
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void halo$onAttackPre(Player player, Entity target, CallbackInfo ci) {
        KillAuraModule aura = getKillAuraModule();
        if (aura != null && aura.shouldBlockAttacks()) {
            ci.cancel();
            return;
        }

        AttackEvent event = new AttackEvent(AttackEvent.State.PRE, target);
        EventBus.getInstance().post(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Fires a POST attack event after the attack is processed.
     */
    @Inject(method = "attack", at = @At("RETURN"))
    private void halo$onAttackPost(Player player, Entity target, CallbackInfo ci) {
        EventBus.getInstance().post(new AttackEvent(AttackEvent.State.POST, target));
    }

    private com.haloclient.client.module.impl.combat.KillAuraModule getKillAuraModule() {
        try {
            if (com.haloclient.client.HaloClient.INSTANCE != null && com.haloclient.client.HaloClient.INSTANCE.moduleManager != null) {
                return com.haloclient.client.HaloClient.INSTANCE.moduleManager.getModule(com.haloclient.client.module.impl.combat.KillAuraModule.class);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
