package com.haloclient.client.mixin.killaura;

import com.haloclient.client.rotation.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
    private void halo$applySilentRenderRotation(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) return;

        RotationManager rm = RotationManager.getInstance();
        if (!rm.isActive()) return;

        float bodyYaw = Mth.rotLerp(partialTick, rm.getPreviousServerYaw(), rm.getServerYaw());
        float pitch = Mth.lerp(partialTick, rm.getPreviousServerPitch(), rm.getServerPitch());

        state.yRot = 0.0f;
        state.bodyRot = bodyYaw;
        state.xRot = pitch;
    }
}
