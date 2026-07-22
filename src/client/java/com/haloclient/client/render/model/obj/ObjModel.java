package com.haloclient.client.render.model.obj;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ObjModel {

    public static class Vertex {
        public float x, y, z;
        public float u, v;
        public float nx, ny, nz;
        public int r = 255, g = 255, b = 255;
        public int partId = 4; // 0 = Left Leg, 1 = Right Leg, 2 = Left Arm & Bat, 3 = Right Arm, 4 = Body/Head/Face

        public Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }

        public Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int r, int g, int b) {
            this(x, y, z, u, v, nx, ny, nz);
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static class Face {
        public final List<Vertex> vertices = new ArrayList<>();
        public int colorOverride = -1;
    }

    private final List<Face> faces = new ArrayList<>();

    public void addFace(Face face) {
        faces.add(face);
    }

    public List<Face> getFaces() {
        return faces;
    }

    public void render(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int light,
        int overlay,
        int defaultR,
        int defaultG,
        int defaultB,
        int alpha,
        AvatarRenderState state
    ) {
        Matrix4f matrix = pose.pose();

        // Exact walking & running animation speed
        float walkSpeed = state != null ? state.walkAnimationSpeed : 0.0f;
        float walkPos = state != null ? state.walkAnimationPos : 0.0f;

        float leftHipAngle = 0.0f;
        float rightHipAngle = 0.0f;
        float leftKneeAngle = 0.0f;
        float rightKneeAngle = 0.0f;

        float leftArmPitch = 0.0f;
        float rightArmPitch = 0.0f;
        float rightArmRoll = 0.0f;
        float rightArmYaw = 0.0f;
        float rightElbowFlex = 0.0f;

        // Anatomically correct human walking and running kinematics for legs AND arms:
        if (walkSpeed > 0.01f) {
            float speedFactor = Mth.clamp(walkSpeed, 0.4f, 1.2f);

            // Dynamic hip swing amplitude
            float swingAmp = speedFactor * 0.45f;
            leftHipAngle = Mth.sin(walkPos * 1.4f) * swingAmp;
            rightHipAngle = -leftHipAngle;

            // Backward knee flexion angle
            float kneeFlexAmp = 0.75f * speedFactor;
            leftKneeAngle = -Math.max(0.0f, -Mth.sin(walkPos * 1.4f)) * kneeFlexAmp;
            rightKneeAngle = -Math.max(0.0f, Mth.sin(walkPos * 1.4f)) * kneeFlexAmp;

            // Arm pitch swing (opposite phase to legs, 0.5f amplitude)
            leftArmPitch = -leftHipAngle * 0.50f;
            rightArmPitch = -rightHipAngle * 0.50f;
        }

        // Realistic punch/chop/strike animation for right arm (swinging forward in front)
        float attackProgress = state != null ? state.attackTime : 0.0f;
        if (attackProgress > 0.0f) {
            // Smooth curve for forward swing
            float swingProgress = Mth.sin(attackProgress * (float) Math.PI);

            // 1. Arm swings FORWARD (Pitch +)
            rightArmPitch += swingProgress * 1.4f;
            // 2. Arm swings inward toward center (Yaw)
            rightArmYaw = swingProgress * 0.4f;
            // 3. Forearm bends forward (Elbow Flexion)
            rightElbowFlex = -swingProgress * 0.4f;
            // 4. Slight wrist/arm rotation (Roll)
            rightArmRoll = -swingProgress * 0.3f;
        }

        // Render model geometry faces
        for (Face face : faces) {
            int faceR = face.colorOverride >= 0 ? (face.colorOverride >> 16) & 0xFF : defaultR;
            int faceG = face.colorOverride >= 0 ? (face.colorOverride >> 8) & 0xFF : defaultG;
            int faceB = face.colorOverride >= 0 ? face.colorOverride & 0xFF : defaultB;

            List<Vertex> verts = face.vertices;
            if (verts.size() < 3) continue;

            // Render vertices in reverse order (CCW) to fix the inside-out pillowcase look
            if (verts.size() == 3) {
                emitTransformedVertex(consumer, matrix, verts.get(2), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(1), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(0), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(0), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
            } else if (verts.size() == 4) {
                emitTransformedVertex(consumer, matrix, verts.get(3), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(2), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(1), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                emitTransformedVertex(consumer, matrix, verts.get(0), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
            } else {
                for (int i = verts.size() - 2; i >= 1; i--) {
                    emitTransformedVertex(consumer, matrix, verts.get(verts.size() - 1), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                    emitTransformedVertex(consumer, matrix, verts.get(i), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                    emitTransformedVertex(consumer, matrix, verts.get(i - 1), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                    emitTransformedVertex(consumer, matrix, verts.get(i - 1), faceR, faceG, faceB, alpha, light, overlay, leftHipAngle, rightHipAngle, leftKneeAngle, rightKneeAngle, leftArmPitch, rightArmPitch, rightArmRoll, rightArmYaw, rightElbowFlex);
                }
            }
        }
    }

    private static void emitTransformedVertex(
        VertexConsumer consumer,
        Matrix4f matrix,
        Vertex vert,
        int r, int g, int b, int a,
        int light, int overlay,
        float leftHipAngle,
        float rightHipAngle,
        float leftKneeAngle,
        float rightKneeAngle,
        float leftArmPitch,
        float rightArmPitch,
        float rightArmRoll,
        float rightArmYaw,
        float rightElbowFlex
    ) {
        float vx = vert.x;
        float vy = vert.y;
        float vz = vert.z;

        // 1. Left Leg Kinematics (partId 0)
        if (vert.partId == 0 && (leftHipAngle != 0.0f || leftKneeAngle != 0.0f)) {
            float hipY = 14.0f;
            float hipZ = -0.5f;
            float kneeY = 18.5f;
            float kneeZ = -0.5f;

            if (vy >= 18.5f) {
                float relY_k = vy - kneeY;
                float relZ_k = vz - kneeZ;
                float cos_k = Mth.cos(leftKneeAngle);
                float sin_k = Mth.sin(leftKneeAngle);
                vy = kneeY + relY_k * cos_k - relZ_k * sin_k;
                vz = kneeZ + relY_k * sin_k + relZ_k * cos_k;
            }

            float relY_h = vy - hipY;
            float relZ_h = vz - hipZ;
            float cos_h = Mth.cos(leftHipAngle);
            float sin_h = Mth.sin(leftHipAngle);
            vy = hipY + relY_h * cos_h - relZ_h * sin_h;
            vz = hipZ + relY_h * sin_h + relZ_h * cos_h;

        // 2. Right Leg Kinematics (partId 1)
        } else if (vert.partId == 1 && (rightHipAngle != 0.0f || rightKneeAngle != 0.0f)) {
            float hipY = 14.0f;
            float hipZ = -1.5f;
            float kneeY = 18.5f;
            float kneeZ = -1.5f;

            if (vy >= 18.5f) {
                float relY_k = vy - kneeY;
                float relZ_k = vz - kneeZ;
                float cos_k = Mth.cos(rightKneeAngle);
                float sin_k = Mth.sin(rightKneeAngle);
                vy = kneeY + relY_k * cos_k - relZ_k * sin_k;
                vz = kneeZ + relY_k * sin_k + relZ_k * cos_k;
            }

            float relY_h = vy - hipY;
            float relZ_h = vz - hipZ;
            float cos_h = Mth.cos(rightHipAngle);
            float sin_h = Mth.sin(rightHipAngle);
            vy = hipY + relY_h * cos_h - relZ_h * sin_h;
            vz = hipZ + relY_h * sin_h + relZ_h * cos_h;

        // 3. Left Arm & Baseball Bat Kinematics (partId 2) - Shoulder Pivot (-3.5, 5.0, 0.0)
        // Linear Blend Skinning (LBS): blend rotation weight smoothly from Y=5.0f (shoulder) to Y=9.0f (arm)
        } else if (vert.partId == 2 && leftArmPitch != 0.0f) {
            float shoulderY = 5.0f;
            float shoulderZ = 0.0f;

            float weight = 1.0f;
            if (vy < 5.0f) {
                weight = 0.0f;
            } else if (vy < 9.0f) {
                weight = (vy - 5.0f) / 4.0f;
            }

            float blendedAngle = leftArmPitch * weight;
            if (blendedAngle != 0.0f) {
                float relY = vy - shoulderY;
                float relZ = vz - shoulderZ;
                float cos = Mth.cos(blendedAngle);
                float sin = Mth.sin(blendedAngle);
                vy = shoulderY + relY * cos - relZ * sin;
                vz = shoulderZ + relY * sin + relZ * cos;
            }

        // 4. Right Arm Kinematics (partId 3) - Shoulder Pivot (5.0, 5.0, -1.5)
        } else if (vert.partId == 3 && (rightArmPitch != 0.0f || rightArmRoll != 0.0f || rightArmYaw != 0.0f)) {
            float shoulderX = 5.0f;
            float shoulderY = 5.0f;
            float shoulderZ = -1.5f;
            float elbowY = 12.0f;

            float weight = 1.0f;
            if (vy < 5.0f) {
                weight = 0.0f;
            } else if (vy < 9.0f) {
                weight = (vy - 5.0f) / 4.0f;
            }

            // Elbow flexion (bending lower arm / forearm in attack)
            if (vy >= elbowY && rightElbowFlex != 0.0f) {
                float relY_e = vy - elbowY;
                float relZ_e = vz - shoulderZ;
                float cos_e = Mth.cos(rightElbowFlex);
                float sin_e = Mth.sin(rightElbowFlex);
                vy = elbowY + relY_e * cos_e - relZ_e * sin_e;
                vz = shoulderZ + relY_e * sin_e + relZ_e * cos_e;
            }

            // Shoulder rotations: Pitch (X-axis), Roll (Z-axis), Yaw (Y-axis)
            float relX = vx - shoulderX;
            float relY = vy - shoulderY;
            float relZ = vz - shoulderZ;

            // Apply Pitch
            if (rightArmPitch != 0.0f) {
                float angle = rightArmPitch * weight;
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);
                float ny = relY * cos - relZ * sin;
                float nz = relY * sin + relZ * cos;
                relY = ny;
                relZ = nz;
            }

            // Apply Yaw (inward chop/swing arc)
            if (rightArmYaw != 0.0f) {
                float angle = rightArmYaw * weight;
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);
                float nx = relX * cos + relZ * sin;
                float nz = -relX * sin + relZ * cos;
                relX = nx;
                relZ = nz;
            }

            // Apply Roll
            if (rightArmRoll != 0.0f) {
                float angle = rightArmRoll * weight;
                float cos = Mth.cos(angle);
                float sin = Mth.sin(angle);
                float nx = relX * cos - relY * sin;
                float ny = relX * sin + relY * cos;
                relX = nx;
                relY = ny;
            }

            vx = shoulderX + relX;
            vy = shoulderY + relY;
            vz = shoulderZ + relZ;
        }

        int finalR = vert.r != 255 ? (vert.r * r) / 255 : r;
        int finalG = vert.g != 255 ? (vert.g * g) / 255 : g;
        int finalB = vert.b != 255 ? (vert.b * b) / 255 : b;

        consumer.addVertex(matrix, vx, vy, vz)
            .setColor(finalR, finalG, finalB, a)
            .setUv(vert.u, vert.v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(vert.nx, vert.ny, vert.nz);
    }
}
