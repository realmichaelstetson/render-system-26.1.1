package com.haloclient.client.render.model;

import com.haloclient.client.render.model.obj.ObjModel;
import com.haloclient.client.render.model.obj.ObjModelLoader;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;

public class TungTungSahurRenderer {

    private static ObjModel loadedModel = null;
    private static Identifier textureId = null;

    public static Identifier getOrRegisterTexture() {
        if (textureId == null) {
            try (InputStream stream = TungTungSahurRenderer.class.getResourceAsStream("/assets/halo/tung_tung_sahur.png")) {
                if (stream != null) {
                    NativeImage image = NativeImage.read(stream);
                    DynamicTexture dynamicTexture = new DynamicTexture(() -> "Tung Tung Sahur Texture", image);
                    textureId = Identifier.parse("halo:dynamic_tung_tung_sahur");
                    Minecraft.getInstance().getTextureManager().register(textureId, dynamicTexture);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return textureId;
    }

    public static void render(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        AvatarRenderState state
    ) {
        if (loadedModel == null) {
            try (InputStream stream = TungTungSahurRenderer.class.getResourceAsStream("/assets/halo/tung_tung_sahur.obj")) {
                if (stream != null) {
                    loadedModel = ObjModelLoader.loadModel(stream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (loadedModel != null) {
            loadedModel.render(pose, consumer, packedLight, packedOverlay, 255, 255, 255, 255, state);
        }
    }

    public static void reloadModel() {
        loadedModel = null;
        textureId = null;
    }
}
