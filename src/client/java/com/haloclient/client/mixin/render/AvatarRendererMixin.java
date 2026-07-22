package com.haloclient.client.mixin.render;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.ModuleManager;
import com.haloclient.client.module.impl.render.CustomModelModule;
import com.haloclient.client.render.model.TungTungSahurRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class AvatarRendererMixin {

    @Shadow protected abstract void setupRotations(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale);
    @Shadow protected abstract void scale(LivingEntityRenderState state, PoseStack poseStack);

    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void halo$submitCustomPlayerModel(
        LivingEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState,
        CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(state instanceof AvatarRenderState avatarState)) {
            return;
        }

        // Target only the local player
        if (avatarState.id != mc.player.getId()) {
            return;
        }

        CustomModelModule module = getCustomModelModule();
        if (module == null || !module.isTungTungSahur()) {
            return;
        }

        // Do NOT replace or render body model in 1st-person camera mode
        if (mc.options.getCameraType().isFirstPerson()) {
            return;
        }

        Identifier textureId = TungTungSahurRenderer.getOrRegisterTexture();
        if (textureId == null) {
            return;
        }

        int packedLight = 0xF000F0;

        poseStack.pushPose();

        // Apply vanilla entity rotations and model scaling
        this.setupRotations(state, poseStack, state.bodyRot, state.scale);
        this.scale(state, poseStack);

        // Rotate 180 degrees so model faces forward in player movement direction
        poseStack.mulPose(new Quaternionf().rotateY(Mth.PI));

        // Flip Y axis to match Minecraft's coordinates
        poseStack.scale(-1.0F, -1.0F, 1.0F);

        // Get the custom model scale configured in settings
        float customScale = module.getModelScale().getValue().floatValue();

        // Translate down proportionally to customScale to lock feet on the ground
        float translateY = 1.501F * customScale;
        poseStack.translate(0.0F, -translateY, 0.0F);

        // Head and Torso remain 100% still/rigid without any whole-body waddling or head wobbling.

        // Apply custom model scale dynamically configured in CustomModelModule settings
        float basePixelScale = 0.0625F; // Convert pixel model coordinates to meters (16 pixels = 1 block)
        float finalScale = basePixelScale * customScale;
        poseStack.scale(finalScale, finalScale, finalScale);

        // Translate in OBJ space to center the model and align feet perfectly on the ground:
        // - Centered on X: -0.956F
        // - Feet aligned on Y: 0.0F
        // - Centered on Z: +1.519F
        poseStack.translate(-0.956F, 0.0F, 1.519F);

        // Render using entityCutout with registered DynamicTexture
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityCutout(textureId),
            (pose, buffer) -> TungTungSahurRenderer.render(pose, buffer, packedLight, OverlayTexture.NO_OVERLAY, avatarState)
        );

        poseStack.popPose();

        ci.cancel();
    }

    private CustomModelModule getCustomModelModule() {
        if (HaloClient.INSTANCE == null) return null;
        ModuleManager mm = HaloClient.INSTANCE.getModuleManager();
        return mm != null ? mm.getModule(CustomModelModule.class) : null;
    }
}
