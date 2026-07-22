package com.haloclient.client.render.cape;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class WaveyCapeRenderer {

    private static final int GRID_HEIGHT = 16;

    public static void renderWaveyCape(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        AvatarRenderState state
    ) {
        float capeWidth = 0.625f;  // 10 / 16.0f (10 pixels wide)
        float capeHeight = 1.0f;   // 16 / 16.0f (16 pixels high)
        float capeDepth = 0.0625f; // 1 / 16.0f (1 pixel thick)

        float halfWidth = capeWidth / 2.0f;
        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;

        // Player movement physics inputs
        float walkSpeed = Math.max(state.walkAnimationSpeed, state.speedValue);
        float walkPos = state.walkAnimationPos;

        Matrix4f matrix = pose.pose();

        // Top face (at Y = 0)
        drawQuad(consumer, matrix, packedLight, packedOverlay,
            -halfWidth, 0, 0,
            halfWidth, 0, 0,
            halfWidth, 0, capeDepth,
            -halfWidth, 0, capeDepth,
            1/64f, 0/32f, 11/64f, 1/32f,
            0, -1, 0);

        for (int part = 0; part < GRID_HEIGHT; part++) {
            float progress = (float) part / (float) GRID_HEIGHT;
            float nextProgress = (float) (part + 1) / (float) GRID_HEIGHT;

            // In Minecraft model coordinates, +Y is DOWN towards the feet
            float yStart = progress * capeHeight;
            float yEnd = nextProgress * capeHeight;

            // Phase delay down segments for smooth traveling wave propagation
            float phaseStart = part * 0.35f;
            float phaseEnd = (part + 1) * 0.35f;

            float flutterFreq = 3.0f + walkSpeed * 4.0f;
            float flutterAmp = 0.008f + walkSpeed * 0.018f;
            float idleFactor = 1.0f - Math.min(walkSpeed * 2.0f, 1.0f);

            // Additional backward lift when jumping/falling (state.capeFlap)
            float jumpLiftStart = (state.capeFlap * 0.006f) * (float) Math.pow(progress, 1.4);
            float jumpLiftEnd = (state.capeFlap * 0.006f) * (float) Math.pow(nextProgress, 1.4);

            // Z-Offset (backward bend, jump lift & traveling wave propagation)
            float startWave = Mth.sin(walkPos * 1.2f - phaseStart) * walkSpeed * 0.04f
                            + Mth.sin(time * flutterFreq - phaseStart) * flutterAmp
                            + Mth.sin(time * 2.2f - phaseStart) * 0.012f * idleFactor;

            float endWave = Mth.sin(walkPos * 1.2f - phaseEnd) * walkSpeed * 0.04f
                          + Mth.sin(time * flutterFreq - phaseEnd) * flutterAmp
                          + Mth.sin(time * 2.2f - phaseEnd) * 0.012f * idleFactor;

            float bendFactor = 0.06f + (1.0f - Math.min(walkSpeed, 1.0f)) * 0.04f;

            float zStart = (float) Math.pow(progress, 1.25) * bendFactor + jumpLiftStart + (progress > 0 ? startWave * progress : 0);
            float zEnd = (float) Math.pow(nextProgress, 1.25) * bendFactor + jumpLiftEnd + endWave * nextProgress;

            // Sideways X-sway when turning / walking
            float xSwayStart = Mth.sin(walkPos * 0.8f - phaseStart) * (state.capeLean2 * 0.0012f) * progress;
            float xSwayEnd = Mth.sin(walkPos * 0.8f - phaseEnd) * (state.capeLean2 * 0.0012f) * nextProgress;

            float xLeftStart = -halfWidth + xSwayStart;
            float xRightStart = halfWidth + xSwayStart;
            float xLeftEnd = -halfWidth + xSwayEnd;
            float xRightEnd = halfWidth + xSwayEnd;

            // Texture V mapping per height slice (64x32 standard cape texture)
            float vStart = (1.0f + progress * 16.0f) / 32.0f;
            float vEnd = (1.0f + nextProgress * 16.0f) / 32.0f;

            // Outer face (facing +Z away from player back) - MAIN CAPE DESIGN (11..1)
            drawQuad(consumer, matrix, packedLight, packedOverlay,
                xLeftStart, yStart, zStart + capeDepth,
                xRightStart, yStart, zStart + capeDepth,
                xRightEnd, yEnd, zEnd + capeDepth,
                xLeftEnd, yEnd, zEnd + capeDepth,
                11/64f, vStart, 1/64f, vEnd,
                0, 0, 1);

            // Inner face (facing -Z towards player back) - CAPE INSIDE (22..12)
            drawQuad(consumer, matrix, packedLight, packedOverlay,
                xRightStart, yStart, zStart,
                xLeftStart, yStart, zStart,
                xLeftEnd, yEnd, zEnd,
                xRightEnd, yEnd, zEnd,
                22/64f, vStart, 12/64f, vEnd,
                0, 0, -1);

            // Left face (facing -X)
            drawQuad(consumer, matrix, packedLight, packedOverlay,
                xLeftStart, yStart, zStart,
                xLeftStart, yStart, zStart + capeDepth,
                xLeftEnd, yEnd, zEnd + capeDepth,
                xLeftEnd, yEnd, zEnd,
                0/64f, vStart, 1/64f, vEnd,
                -1, 0, 0);

            // Right face (facing +X)
            drawQuad(consumer, matrix, packedLight, packedOverlay,
                xRightStart, yStart, zStart + capeDepth,
                xRightStart, yStart, zStart,
                xRightEnd, yEnd, zEnd,
                xRightEnd, yEnd, zEnd + capeDepth,
                11/64f, vStart, 12/64f, vEnd,
                1, 0, 0);

            if (part == GRID_HEIGHT - 1) {
                // Bottom face at final segment
                drawQuad(consumer, matrix, packedLight, packedOverlay,
                    xLeftEnd, yEnd, zEnd + capeDepth,
                    xRightEnd, yEnd, zEnd + capeDepth,
                    xRightEnd, yEnd, zEnd,
                    xLeftEnd, yEnd, zEnd,
                    11/64f, 0/32f, 21/64f, 1/32f,
                    0, 1, 0);
            }
        }
    }

    private static void drawQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        int packedLight,
        int packedOverlay,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4,
        float u1, float v1, float u2, float v2,
        float nx, float ny, float nz
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(255, 255, 255, 255).setUv(u2, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(255, 255, 255, 255).setUv(u2, v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
        consumer.addVertex(matrix, x4, y4, z4).setColor(255, 255, 255, 255).setUv(u1, v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(nx, ny, nz);
    }
}
