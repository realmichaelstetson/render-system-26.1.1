package com.haloclient.client.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable-style data class for yaw/pitch rotation.
 * Includes GCD sensitivity fix for anti-cheat bypass.
 */
public class Rotation {
    private float yaw;
    private float pitch;

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    /** Converts this rotation to a unit direction vector. */
    public Vec3 toDirection() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);
        float x = -Mth.sin(radYaw) * Mth.cos(radPitch);
        float y = -Mth.sin(radPitch);
        float z = Mth.cos(radYaw) * Mth.cos(radPitch);
        return new Vec3(x, y, z);
    }

    /** Returns the angular distance between this rotation and another. */
    public float angleTo(Rotation other) {
        float yawDiff = Mth.wrapDegrees(other.yaw - this.yaw);
        float pitchDiff = other.pitch - this.pitch;
        return Mth.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    public Rotation copy() {
        return new Rotation(yaw, pitch);
    }

    /** Creates a new Rotation with angles wrapped to valid ranges. */
    public static Rotation wrapped(float yaw, float pitch) {
        return new Rotation(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0f, 90.0f));
    }

    /**
     * Applies GCD (Greatest Common Divisor) fix for Minecraft mouse sensitivity.
     * Makes rotations appear as if they came from actual mouse movement,
     * which is critical for bypassing anti-cheat rotation checks.
     *
     * @param previousRotation The rotation from the previous tick
     * @param sensitivity      The player's mouse sensitivity setting (0.0 - 1.0)
     * @return A new Rotation with GCD-corrected values
     */
    public Rotation withGCDFix(Rotation previousRotation, float sensitivity) {
        float gcd = getGCDValue(sensitivity);
        float yawDiff = Mth.wrapDegrees(this.yaw - previousRotation.yaw);
        float pitchDiff = this.pitch - previousRotation.pitch;
        float fixedYawDiff = yawDiff - (yawDiff % gcd);
        float fixedPitchDiff = pitchDiff - (pitchDiff % gcd);
        return new Rotation(
                previousRotation.yaw + fixedYawDiff,
                Mth.clamp(previousRotation.pitch + fixedPitchDiff, -90.0f, 90.0f)
        );
    }

    /**
     * Calculates the GCD value from mouse sensitivity.
     * Formula: (sensitivity * 0.6 + 0.2)^3 * 8.0 * 0.15
     */
    private static float getGCDValue(float sensitivity) {
        float f = sensitivity * 0.6f + 0.2f;
        float f1 = f * f * f;
        return f1 * 8.0f * 0.15f;
    }

    @Override
    public String toString() {
        return "Rotation{yaw=" + yaw + ", pitch=" + pitch + "}";
    }
}
