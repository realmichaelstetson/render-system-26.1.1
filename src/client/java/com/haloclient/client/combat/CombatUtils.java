package com.haloclient.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Combat utility methods for attack validation, cooldown checks, and targeting.
 */
public final class CombatUtils {

    private CombatUtils() {}

    /**
     * Returns the 1.9+ attack cooldown progress (0.0 to 1.0).
     * 1.0 means fully charged and ready to attack.
     */
    public static float getAttackCooldownProgress(Player player) {
        return player.getAttackStrengthScale(0.5f);
    }

    /**
     * Checks if the player can perform a critical hit.
     * Requires: falling, not on ground, not in water, not on ladder, etc.
     */
    public static boolean canCritical(Player player) {
        return player.fallDistance > 0.0f
                && !player.onGround()
                && !player.onClimbable()
                && !player.isInWater()
                && !player.isPassenger();
    }

    /**
     * Comprehensive validation of whether an entity is a valid attack target.
     */
    public static boolean isValidTarget(Entity entity) {
        if (entity == null) return false;
        if (entity == Minecraft.getInstance().player) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (!entity.isAlive()) return false;
        if (living.isDeadOrDying()) return false;
        if (entity.isSpectator()) return false;
        return true;
    }

    /**
     * Gets the distance from the player's eyes to the nearest point on the target's bounding box.
     * More accurate than center-to-center distance for hit range validation.
     */
    public static double getDistanceToEntity(Entity from, Entity target) {
        Vec3 eyes = from.getEyePosition(1.0f);
        AABB box = target.getBoundingBox();

        double nearestX = Math.max(box.minX, Math.min(eyes.x, box.maxX));
        double nearestY = Math.max(box.minY, Math.min(eyes.y, box.maxY));
        double nearestZ = Math.max(box.minZ, Math.min(eyes.z, box.maxZ));

        return eyes.distanceTo(new Vec3(nearestX, nearestY, nearestZ));
    }

    /**
     * Checks whether the player's current server-side look ray intersects the target hitbox.
     */
    public static boolean canHitEntity(Entity from, Entity target, Vec3 lookDirection, double range) {
        Vec3 eyes = from.getEyePosition(1.0f);
        Vec3 end = eyes.add(lookDirection.normalize().scale(range));
        AABB box = target.getBoundingBox().inflate(0.03);
        Optional<Vec3> hit = box.clip(eyes, end);
        return hit.isPresent() && eyes.distanceTo(hit.get()) <= range;
    }

    /**
     * Performs a line-of-sight check between two entities using world raycasting.
     * Returns true if there are no solid blocks between the entities.
     */
    public static boolean hasLineOfSight(Entity from, Entity target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Vec3 fromEyes = from.getEyePosition(1.0f);
        Vec3 targetEyes = target.getEyePosition(1.0f);

        HitResult hitResult = mc.level.clip(new ClipContext(
                fromEyes, targetEyes,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                from
        ));

        // MISS = no block was hit (clear line of sight)
        // Or the block hit is further than the target
        return hitResult.getType() == HitResult.Type.MISS
                || hitResult.getLocation().distanceTo(fromEyes) >= fromEyes.distanceTo(targetEyes) - 0.5;
    }

    /**
     * Performs a directional raycast along lookDirection to verify if the look ray
     * intersects the target entity without being blocked by obstacles.
     */
    public static boolean hasLineOfSightRaycast(Entity from, Entity target, Vec3 lookDirection, double range) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;

        Vec3 fromEyes = from.getEyePosition(1.0f);
        Vec3 end = fromEyes.add(lookDirection.normalize().scale(range));

        HitResult blockHit = mc.level.clip(new ClipContext(
                fromEyes, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                from
        ));

        double blockDist = (blockHit.getType() != HitResult.Type.MISS)
                ? fromEyes.distanceTo(blockHit.getLocation())
                : range + 1.0;

        AABB box = target.getBoundingBox().inflate(0.03);
        Optional<Vec3> entityHit = box.clip(fromEyes, end);

        if (entityHit.isEmpty()) return false;

        double entityDist = fromEyes.distanceTo(entityHit.get());
        return entityDist <= range && entityDist <= blockDist;
    }
}
