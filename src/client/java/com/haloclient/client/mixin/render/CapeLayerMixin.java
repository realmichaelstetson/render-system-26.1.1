package com.haloclient.client.mixin.render;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.ModuleManager;
import com.haloclient.client.module.impl.render.CapesModule;
import com.haloclient.client.render.cape.WaveyCapeRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void halo$submitWaveyCape(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        int packedLight,
        AvatarRenderState state,
        float yRot,
        float xRot,
        CallbackInfo ci
    ) {
        CapesModule capesModule = getCapesModule();
        if (capesModule == null || !capesModule.isWaveyCapesEnabled()) {
            return;
        }

        if (state == null || state.isInvisible || !state.showCape || state.skin == null || state.skin.cape() == null) {
            return;
        }

        Identifier capeId = state.skin.cape().id();
        if (capeId == null) {
            return;
        }

        poseStack.pushPose();

        // Handle crouching translation if sneaking
        if (state.isCrouching) {
            poseStack.translate(0.0f, 0.21875f, -0.0625f);
        }

        // Translate to upper back attachment point (z = 0.125f = 2 pixels behind body)
        poseStack.translate(0.0f, 0.0f, 0.125f);

        // Amplified vanilla cape rotations for full jump & movement responsiveness
        float amplifiedFlap = state.capeFlap * 1.75f;
        float amplifiedLean = state.capeLean * 1.25f;

        float capeXRot = (6.0f + amplifiedLean / 2.0f + amplifiedFlap) * (Mth.PI / 180.0f);
        float capeZRot = (state.capeLean2 / 2.0f) * (Mth.PI / 180.0f);
        float capeYRot = (-state.capeLean2 / 2.0f) * (Mth.PI / 180.0f);

        Quaternionf rotation = new Quaternionf()
            .rotateY(capeYRot)
            .rotateX(capeXRot)
            .rotateZ(capeZRot);

        poseStack.mulPose(rotation);

        nodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.entitySolid(capeId),
            (pose, buffer) -> WaveyCapeRenderer.renderWaveyCape(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, state)
        );

        poseStack.popPose();

        ci.cancel();
    }

    private CapesModule getCapesModule() {
        if (HaloClient.INSTANCE == null) return null;
        ModuleManager mm = HaloClient.INSTANCE.getModuleManager();
        return mm != null ? mm.getModule(CapesModule.class) : null;
    }
}
