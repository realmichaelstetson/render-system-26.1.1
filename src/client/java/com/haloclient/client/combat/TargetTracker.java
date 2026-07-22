package com.haloclient.client.combat;

import com.haloclient.client.rotation.Rotation;
import com.haloclient.client.rotation.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Target Tracker — finds, filters, sorts, and selects valid combat targets.
 * Supports Single/Switch/Multi targeting modes with configurable priority sorting.
 */
public class TargetTracker {

    public enum Priority { DISTANCE, HEALTH, ANGLE, HURT_TIME }
    public enum TargetMode { SINGLE, SWITCH, MULTI }

    private final Minecraft mc = Minecraft.getInstance();

    private LivingEntity currentTarget = null;
    private final List<LivingEntity> targets = new ArrayList<>();
    private long lastSwitchTime = 0;
    private int switchIndex = 0;

    /**
     * Scans the world for valid targets and selects the current target.
     *
     * @param range        Maximum scan range in blocks
     * @param fov          FOV cone in degrees (360 = all around)
     * @param players      Include players
     * @param mobs         Include hostile mobs
     * @param animals      Include passive animals
     * @param invisible    Include invisible entities
     * @param throughWalls Include entities behind blocks
     * @param priority     Sorting priority
     * @param mode         Target selection mode
     * @param switchDelay  Delay between target switches (ms) — for Switch mode
     */
    public void update(
            double range, float fov,
            boolean players, boolean mobs, boolean animals, boolean invisible,
            boolean throughWalls,
            Priority priority, TargetMode mode, long switchDelay
    ) {
        targets.clear();

        if (mc.player == null || mc.level == null) {
            currentTarget = null;
            return;
        }

        // Scan all renderable entities
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!CombatUtils.isValidTarget(entity)) continue;

            // === Type Filters ===
            if (entity instanceof Player && !players) continue;
            if (entity instanceof Monster && !mobs) continue;
            if (entity instanceof Animal && !animals) continue;
            // Other mobs (golems, etc.) — treat as "mobs"
            if (entity instanceof Mob && !(entity instanceof Monster) && !(entity instanceof Animal) && !mobs)
                continue;

            // === Attribute Filters ===
            if (entity.isInvisible() && !invisible) continue;

            // === Range Check ===
            double dist = CombatUtils.getDistanceToEntity(mc.player, entity);
            if (dist > range) continue;

            // === FOV Check ===
            if (!RotationUtils.isInFOV(mc.player, entity, fov)) continue;

            // === Line of Sight ===
            if (!throughWalls && !CombatUtils.hasLineOfSight(mc.player, entity)) continue;

            targets.add(living);
        }

        // Sort targets by priority
        sortTargets(priority);

        // Select current target based on mode
        selectTarget(mode, switchDelay);
    }

    private void sortTargets(Priority priority) {
        if (mc.player == null) return;

        targets.sort((a, b) -> switch (priority) {
            case DISTANCE -> Double.compare(
                    CombatUtils.getDistanceToEntity(mc.player, a),
                    CombatUtils.getDistanceToEntity(mc.player, b)
            );
            case HEALTH -> Float.compare(a.getHealth(), b.getHealth());
            case ANGLE -> {
                Rotation rotA = RotationUtils.calculateRotation(mc.player.getEyePosition(1.0f), a.position());
                Rotation rotB = RotationUtils.calculateRotation(mc.player.getEyePosition(1.0f), b.position());
                Rotation current = new Rotation(mc.player.getYRot(), mc.player.getXRot());
                yield Float.compare(current.angleTo(rotA), current.angleTo(rotB));
            }
            case HURT_TIME -> Integer.compare(a.hurtTime, b.hurtTime);
        });
    }

    private void selectTarget(TargetMode mode, long switchDelay) {
        if (targets.isEmpty()) {
            currentTarget = null;
            return;
        }

        switch (mode) {
            case SINGLE -> {
                // Always lock onto the highest-priority target
                currentTarget = targets.getFirst();
            }
            case SWITCH -> {
                // Cycle through targets after a delay
                long now = System.currentTimeMillis();
                if (currentTarget == null || !targets.contains(currentTarget) || now - lastSwitchTime >= switchDelay) {
                    switchIndex = (switchIndex + 1) % targets.size();
                    currentTarget = targets.get(switchIndex);
                    lastSwitchTime = now;
                }
            }
            case MULTI -> {
                // Cycle through all targets every tick
                switchIndex = (switchIndex + 1) % targets.size();
                currentTarget = targets.get(switchIndex);
            }
        }

        // Final validation
        if (currentTarget != null && !CombatUtils.isValidTarget(currentTarget)) {
            currentTarget = null;
        }
    }

    public LivingEntity getCurrentTarget() { return currentTarget; }
    public List<LivingEntity> getTargets() { return Collections.unmodifiableList(targets); }
    public boolean hasTarget() { return currentTarget != null; }

    public void reset() {
        currentTarget = null;
        targets.clear();
        switchIndex = 0;
        lastSwitchTime = 0;
    }
}
