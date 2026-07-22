package com.haloclient.client.module.impl.combat;

import com.haloclient.client.combat.CombatUtils;
import com.haloclient.client.combat.TargetTracker;
import com.haloclient.client.event.EventBus;
import com.haloclient.client.event.EventHandler;
import com.haloclient.client.event.events.AttackEvent;
import com.haloclient.client.event.events.TickEvent;
import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.module.property.ComboBoxProperty;
import com.haloclient.client.module.property.MultipleComboBoxProperty;
import com.haloclient.client.module.property.NumberProperty;
import com.haloclient.client.movement.MovementFix;
import com.haloclient.client.rotation.Rotation;
import com.haloclient.client.rotation.RotationManager;
import com.haloclient.client.rotation.RotationUtils;
import net.minecraft.world.InteractionHand;
import com.haloclient.client.module.property.GroupProperty;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * KillAura — Full-featured automatic combat module.
 */
public class KillAuraModule extends Module {

    // === Targeting ===
    private final ComboBoxProperty targetMode = new ComboBoxProperty("Target Mode", "Single", "Single", "Switch", "Multi");
    private final ComboBoxProperty priority = new ComboBoxProperty("Priority", "Distance", "Distance", "Health", "Angle");
    private final MultipleComboBoxProperty targets = new MultipleComboBoxProperty(
            "Targets", "Entities to target",
            List.of("Players", "Mobs"),
            "Players", "Mobs", "Animals", "Invisible"
    );

    // === Timing & Switch ===
    private final NumberProperty switchDelay = new NumberProperty("Switch Delay", "ms between target switches", 100, 2000, 500, 50);

    // === Range ===
    private final NumberProperty attackRange = new NumberProperty("Range", "Attack range in blocks", 3.0, 6.0, 3.0, 0.1);
    private final NumberProperty scanRange = new NumberProperty("Scan Range", "Target scan range", 3.0, 8.0, 6.0, 0.1);
    private final NumberProperty fov = new NumberProperty("FOV", "Field of view for targeting", 0, 360, 360, 1);
    private final GroupProperty rangeGroup = new GroupProperty("Range Settings").addProperties(attackRange, scanRange, fov);

    // === CPS / Timing ===
    private final NumberProperty minCPS = new NumberProperty("Min CPS", "Minimum clicks per second", 1, 20, 8, 1);
    private final NumberProperty maxCPS = new NumberProperty("Max CPS", "Maximum clicks per second", 1, 20, 12, 1);
    private final BooleanProperty legacySwing = new BooleanProperty("Legacy Swing", "Use CPS-based clicking instead of 1.9+ cooldown", false);
    private final GroupProperty timingGroup = new GroupProperty("CPS & Timing").addProperties(minCPS, maxCPS, switchDelay, legacySwing);

    // === Rotations ===
    private final BooleanProperty silentRotations = new BooleanProperty("Silent Rotations", "Don't change client camera", true);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", "Degrees per tick when tracking target", 10, 180, 180, 1);
    private final NumberProperty switchRotationSpeed = new NumberProperty("Rotation Switch Speed", "Degrees per tick when switching target or stopping", 10, 180, 180, 1);
    private final NumberProperty rotationRandom = new NumberProperty("Rotation Jitter", "Human-like randomization", 0, 10, 0.0, 0.5);
    private final GroupProperty rotationGroup = new GroupProperty("Rotations").addProperties(silentRotations, rotationSpeed, switchRotationSpeed, rotationRandom);

    // === Movement ===
    private final ComboBoxProperty movementFix = new ComboBoxProperty("Movement Fix", "Silent", "Off", "Silent", "Strict", "AI");

    // === Validation ===
    private final BooleanProperty raycastCheck = new BooleanProperty("Raycast", "Validate line of sight", true);
    private final BooleanProperty throughWalls = new BooleanProperty("Through Walls", "Attack through blocks", false);

    // === Internal State ===
    private final TargetTracker targetTracker = new TargetTracker();
    private long lastAttackTime = 0;
    private long nextAttackDelay = 0;
    private boolean blocking = false;
    private boolean executingAttack = false;
    private LivingEntity previousTarget = null;
    private boolean targetAcquired = false;

    public KillAuraModule() {
        super("KillAura", "Automatically attacks nearby entities", Category.COMBAT);
        addProperties(
                targetMode, priority,
                targets,
                rangeGroup,
                timingGroup,
                rotationGroup,
                movementFix,
                raycastCheck, throughWalls
        );
    }

    @Override
    public String getSuffix() {
        return targetMode.getValue();
    }

    @Override
    protected void onEnable() {
        EventBus.getInstance().register(this);
        lastAttackTime = 0;
        nextAttackDelay = calculateAttackDelay();
        blocking = false;
        previousTarget = null;
        targetAcquired = false;
    }

    @Override
    protected void onDisable() {
        EventBus.getInstance().unregister(this);
        targetTracker.reset();
        RotationManager.getInstance().reset();
        previousTarget = null;
        targetAcquired = false;
        stopBlocking();
    }

    @EventHandler(priority = 10)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) return;

        // === 1. Find & sort targets ===
        TargetTracker.Priority prio = switch (priority.getValue()) {
            case "Health" -> TargetTracker.Priority.HEALTH;
            case "Angle" -> TargetTracker.Priority.ANGLE;
            default -> TargetTracker.Priority.DISTANCE;
        };

        TargetTracker.TargetMode mode = switch (targetMode.getValue()) {
            case "Single" -> TargetTracker.TargetMode.SINGLE;
            case "Multi" -> TargetTracker.TargetMode.MULTI;
            default -> TargetTracker.TargetMode.SWITCH;
        };

        targetTracker.update(
                scanRange.getValue(), fov.getValue().floatValue(),
                targets.isSelected("Players"), targets.isSelected("Mobs"),
                targets.isSelected("Animals"), targets.isSelected("Invisible"),
                throughWalls.getValue(),
                prio, mode, switchDelay.getValue().longValue()
        );

        LivingEntity target = targetTracker.getCurrentTarget();
        if (target == null) {
            targetAcquired = false;
            if (previousTarget != null && RotationManager.getInstance().isActive() && silentRotations.getValue()) {
                Rotation clientCam = new Rotation(mc.player.getYRot(), mc.player.getXRot());
                Rotation currentServ = RotationManager.getInstance().getCurrentRotation();
                if (currentServ != null && (Math.abs(RotationUtils.getAngleDifference(currentServ.getYaw(), clientCam.getYaw())) > 2.0f
                        || Math.abs(currentServ.getPitch() - clientCam.getPitch()) > 2.0f)) {
                    RotationManager.getInstance().requestRotation(
                            clientCam,
                            switchRotationSpeed.getValue().floatValue(),
                            0.0f,
                            0
                    );
                } else {
                    RotationManager.getInstance().reset();
                    previousTarget = null;
                }
            } else {
                RotationManager.getInstance().reset();
                previousTarget = null;
            }
            stopBlocking();
            return;
        }

        // Check if target changed (from null or different target entity)
        if (previousTarget != target) {
            targetAcquired = false;
        }

        // === 2. Calculate rotation to target ===
        Rotation targetRotation = calculateStableTargetRotation(target);

        // Determine current rotation for acquisition angle check
        Rotation currentRot = (silentRotations.getValue() && RotationManager.getInstance().getCurrentRotation() != null)
                ? RotationManager.getInstance().getCurrentRotation()
                : new Rotation(mc.player.getYRot(), mc.player.getXRot());

        // If not yet acquired target, use switchRotationSpeed until aligned within 3 degrees
        if (!targetAcquired) {
            if (Math.abs(RotationUtils.getAngleDifference(currentRot.getYaw(), targetRotation.getYaw())) <= 3.0f
                    && Math.abs(currentRot.getPitch() - targetRotation.getPitch()) <= 3.0f) {
                targetAcquired = true;
            }
        }

        float currentSpeed = !targetAcquired
                ? switchRotationSpeed.getValue().floatValue()
                : rotationSpeed.getValue().floatValue();

        float jitter = rotationRandom.getValue().floatValue();

        // === 3. Request silent rotation ===
        if (silentRotations.getValue()) {
            RotationManager.getInstance().requestRotation(
                    targetRotation,
                    currentSpeed,
                    jitter,
                    5 // Hold for 5 ticks after we stop requesting
            );
        } else {
            // Non-silent: smoothly update player camera towards target
            Rotation smoothed = RotationUtils.smoothRotation(currentRot, targetRotation, currentSpeed, jitter);
            mc.player.setYRot(smoothed.getYaw());
            mc.player.setXRot(smoothed.getPitch());
        }

        previousTarget = target;

        // === 4. Check attack timing ===
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < nextAttackDelay) return;

        // === 5. Validate attack range & look direction hit ===
        double hitRange = Math.max(0.0, attackRange.getValue() - 0.05);
        double distance = CombatUtils.getDistanceToEntity(mc.player, target);
        if (distance > hitRange) return;

        if (!CombatUtils.canHitEntity(mc.player, target, currentRot.toDirection(), hitRange)) return;

        // === 6. Raycast validation along actual server look vector ===
        if (raycastCheck.getValue() && !throughWalls.getValue()) {
            if (!CombatUtils.hasLineOfSightRaycast(mc.player, target, currentRot.toDirection(), hitRange)) return;
        }

        // === 7. Check 1.9+ attack cooldown (skip if legacy swing) ===
        if (!legacySwing.getValue() && CombatUtils.getAttackCooldownProgress(mc.player) < 0.9f) return;



        // === 9. Auto-block: stop blocking before attack ===
        if (blocking) {
            stopBlocking();
        }

        // === 10. Execute attack ===
        performAttack(target);

        // === 11. Auto-block: start blocking after attack ===
      

        // === 12. Update timing ===
        lastAttackTime = now;
        nextAttackDelay = calculateAttackDelay();
    }

    private Rotation calculateStableTargetRotation(LivingEntity target) {
        AABB box = target.getBoundingBox();
        Vec3 eyes = mc.player.getEyePosition(1.0f);
        Vec3 aimPoint = new Vec3(
                (box.minX + box.maxX) * 0.5,
                box.minY + target.getBbHeight() * 0.7,
                (box.minZ + box.maxZ) * 0.5
        );

        return RotationUtils.calculateRotation(eyes, aimPoint);
    }

    private void performAttack(LivingEntity target) {
        if (mc.player == null || mc.gameMode == null) return;

        executingAttack = true;
        try {
            AttackEvent preEvent = new AttackEvent(AttackEvent.State.PRE, target);
            EventBus.getInstance().post(preEvent);
            if (preEvent.isCancelled()) return;

            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);

            mc.player.resetAttackStrengthTicker();

            EventBus.getInstance().post(new AttackEvent(AttackEvent.State.POST, target));
        } finally {
            executingAttack = false;
        }
    }

    private void startBlocking() {
        if (mc.player == null || mc.gameMode == null) return;
        if (blocking) return;

        if (mc.player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                || mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShieldItem) {

            InteractionHand hand = mc.player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

            mc.gameMode.useItem(mc.player, hand);
            blocking = true;
        }
    }

    private void stopBlocking() {
        if (mc.player == null || mc.gameMode == null) return;
        if (!blocking) return;

        mc.gameMode.releaseUsingItem(mc.player);
        blocking = false;
    }

    private long calculateAttackDelay() {
        int min = minCPS.getValue().intValue();
        int max = maxCPS.getValue().intValue();
        if (min > max) min = max;

        int cps = ThreadLocalRandom.current().nextInt(min, max + 1);
        return 1000L / cps;
    }

    public MovementFix.Mode getMovementFixMode() {
        return MovementFix.fromString(movementFix.getValue());
    }

    public boolean isSilentRotations() {
        return silentRotations.getValue();
    }

    public LivingEntity getCurrentTarget() {
        return targetTracker.getCurrentTarget();
    }

    public boolean isExecutingAttack() {
        return executingAttack;
    }

    public boolean hasTarget() {
        return getCurrentTarget() != null;
    }

    public boolean shouldBlockAttacks() {
        return isEnabled() && hasTarget() && !executingAttack;
    }

    public boolean shouldBlockMining() {
        return isEnabled() && hasTarget();
    }

    public boolean shouldBlockPlacing() {
        return isEnabled() && hasTarget();
    }
}
