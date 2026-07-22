package com.haloclient.client.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Static utility methods for rotation calculations.
 * Handles aim calculation, prediction, smoothing, and FOV checks.
 */
public final class RotationUtils {

    private RotationUtils() {}

    /**
     * Calculates the rotation needed to look from one point to another.
     * 
     * Yaw  = atan2(Δz, Δx) × (180/π) − 90°
     * Pitch = −atan2(Δy, √(Δx² + Δz²)) × (180/π)
     */
    public static Rotation calculateRotation(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));

        return Rotation.wrapped(yaw, pitch);
    }

    /**
     * Calculates rotation to a target with velocity-based position prediction.
     * Aims at the predicted chest area for best hit registration.
     */
    public static Rotation calculateRotationWithPrediction(Entity from, LivingEntity target, float tickDelta) {
        Vec3 eyes = from.getEyePosition(tickDelta);

        // Predict target position based on current velocity
        Vec3 velocity = target.getDeltaMovement();
        AABB box = target.getBoundingBox();

        // Aim at predicted center-chest of bounding box
        double targetX = (box.minX + box.maxX) / 2.0 + velocity.x * tickDelta;
        double targetY = box.minY + target.getBbHeight() * 0.7; // Chest height
        double targetZ = (box.minZ + box.maxZ) / 2.0 + velocity.z * tickDelta;

        return calculateRotation(eyes, new Vec3(targetX, targetY, targetZ));
    }

    /**
     * Calculates rotation to the nearest visible point on the target's bounding box.
     * Better accuracy at close range.
     */
    public static Rotation calculateRotationToBoundingBox(Entity from, LivingEntity target, float tickDelta) {
        Vec3 eyes = from.getEyePosition(tickDelta);
        AABB box = target.getBoundingBox();

        // Find nearest point on AABB to player's eyes
        double nearestX = Mth.clamp(eyes.x, box.minX, box.maxX);
        double nearestY = Mth.clamp(eyes.y, box.minY, box.maxY);
        double nearestZ = Mth.clamp(eyes.z, box.minZ, box.maxZ);

        return calculateRotation(eyes, new Vec3(nearestX, nearestY, nearestZ));
    }

    /**
     * Smoothly interpolates between current and target rotation.
     * Includes Gaussian randomization for human-like mouse movement.
     *
     * @param current       Current rotation
     * @param target        Desired target rotation
     * @param speed         Maximum degrees per tick
     * @param randomization Jitter amplitude (0 = none, higher = more random)
     * @return Smoothed rotation one step closer to target
     */
    public static Rotation smoothRotation(Rotation current, Rotation target, float speed, float randomization) {
        float yawDiff = Mth.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();

        // Clamp to max speed (pitch moves slightly slower, like a real human)
        float clampedYaw = Mth.clamp(yawDiff, -speed, speed);
        float clampedPitch = Mth.clamp(pitchDiff, -speed * 0.8f, speed * 0.8f);

        // Add human-like Gaussian jitter
        if (randomization > 0) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            clampedYaw += (float) rng.nextGaussian() * randomization * 0.3f;
            clampedPitch += (float) rng.nextGaussian() * randomization * 0.15f;
        }

        return Rotation.wrapped(
                current.getYaw() + clampedYaw,
                current.getPitch() + clampedPitch
        );
    }

    /**
     * Checks if a target entity is within the specified FOV cone.
     *
     * @param from Player entity
     * @param target Target entity
     * @param fov Full FOV angle in degrees (360 = all around)
     * @return true if target is within FOV
     */
    public static boolean isInFOV(Entity from, Entity target, float fov) {
        if (fov >= 360.0f) return true;

        float halfFov = fov / 2.0f;
        Vec3 eyes = from.getEyePosition(1.0f);
        Vec3 targetPos = target.position();

        double dx = targetPos.x - eyes.x;
        double dz = targetPos.z - eyes.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;

        float yawDiff = Math.abs(Mth.wrapDegrees(targetYaw - from.getYRot()));
        return yawDiff <= halfFov;
    }

    /** Wraps an angle to [-180, 180]. */
    public static float wrapAngleTo180(float angle) {
        return Mth.wrapDegrees(angle);
    }

    /** Gets the shortest angular difference between two angles. */
    public static float getAngleDifference(float a, float b) {
        return Mth.wrapDegrees(a - b);
    }
}
