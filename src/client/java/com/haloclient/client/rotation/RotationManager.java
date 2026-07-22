package com.haloclient.client.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Manages server-side rotations for silent rotation modules.
 * 
 * The rotation manager maintains a "server rotation" that is sent to the server
 * via movement packets, while the client camera (what the player sees) remains unchanged.
 * 
 * Flow per tick:
 * 1. Module calls requestRotation() with desired target
 * 2. update() smooths current rotation towards target (called pre-motion)
 * 3. applyRotation() temporarily sets player yaw/pitch to server rotation (before packet send)
 * 4. restoreRotation() restores client camera rotation (after packet send)
 */
public class RotationManager {
    private static final RotationManager INSTANCE = new RotationManager();

    // Active server-side rotation (what the server believes the player is looking at)
    private Rotation currentRotation = null;
    // Desired rotation to reach
    private Rotation targetRotation = null;
    // Previous tick's rotation for interpolation and GCD fix
    private Rotation previousRotation = null;

    private int holdTicks = 0;
    private float rotationSpeed = 180.0f;
    private float randomization = 0.0f;
    private boolean active = false;

    // Stored original client rotation (saved before apply, restored after)
    private float originalYaw;
    private float originalPitch;
    private float originalPrevYaw;
    private float originalPrevPitch;
    private boolean rotationApplied = false;

    public static RotationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Request a server-side rotation. Must be called every tick by the module
     * that needs the rotation (e.g. KillAura).
     *
     * @param target        Desired rotation
     * @param speed         Maximum degrees per tick for smoothing
     * @param randomization Human-like jitter amplitude
     * @param holdTicks     Ticks to keep rotation after module stops requesting
     */
    public void requestRotation(Rotation target, float speed, float randomization, int holdTicks) {
        this.targetRotation = target;
        this.rotationSpeed = speed;
        this.randomization = randomization;
        this.holdTicks = holdTicks;
        this.active = true;
    }

    /**
     * Updates rotation smoothing. Called once per tick BEFORE motion packets are sent.
     * Smooths currentRotation towards targetRotation, applies GCD fix.
     */
    public void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            reset();
            return;
        }

        if (!active || targetRotation == null) {
            if (holdTicks > 0) {
                holdTicks--;
            } else {
                reset();
            }
            return;
        }

        // Determine current base rotation
        Rotation current;
        if (currentRotation != null) {
            current = currentRotation;
        } else {
            current = new Rotation(mc.player.getYRot(), mc.player.getXRot());
        }

        previousRotation = current.copy();

        // Smooth towards target rotation
        currentRotation = RotationUtils.smoothRotation(current, targetRotation, rotationSpeed, randomization);

        // Apply GCD fix (makes rotation look like real mouse input)
        float sensitivity = mc.options.sensitivity().get().floatValue();
        if (previousRotation != null) {
            currentRotation = currentRotation.withGCDFix(previousRotation, sensitivity);
        }

        // Target consumed — module must re-request each tick
        targetRotation = null;
    }

    /**
     * Applies the server rotation to the player entity.
     * Called BEFORE the game sends movement packets to the server.
     * Saves original rotation values for later restoration.
     */
    public void applyRotation(LocalPlayer player) {
        if (currentRotation == null || !active) return;

        // Save original client camera rotation
        originalYaw = player.getYRot();
        originalPitch = player.getXRot();
        originalPrevYaw = player.yRotO;
        originalPrevPitch = player.xRotO;

        // Apply server rotation (this is what the movement packet will contain)
        player.setYRot(currentRotation.getYaw());
        player.setXRot(currentRotation.getPitch());
        applyVisualRotation(player);

        rotationApplied = true;
    }

    /**
     * Restores the original client-side camera rotation.
     * Called AFTER the game has sent movement packets.
     */
    public void restoreRotation(LocalPlayer player) {
        if (!rotationApplied) return;

        player.setYRot(originalYaw);
        player.setXRot(originalPitch);
        player.yRotO = originalPrevYaw;
        player.xRotO = originalPrevPitch;

        if (currentRotation != null && active) {
            applyVisualRotation(player);
        }

        rotationApplied = false;
    }

    /**
     * Keeps the rendered player model locked to the server rotation without camera shake.
     */
    private void applyVisualRotation(LocalPlayer player) {
        float yaw = currentRotation.getYaw();
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
    }

    /** Resets all rotation state (called on disable or null player). */
    public void reset() {
        currentRotation = null;
        targetRotation = null;
        previousRotation = null;
        holdTicks = 0;
        active = false;
        rotationApplied = false;
    }

    public boolean isActive() {
        return active && currentRotation != null;
    }

    public Rotation getCurrentRotation() {
        return currentRotation;
    }

    public Rotation getPreviousRotation() {
        return previousRotation;
    }

    public boolean isRotationApplied() {
        return rotationApplied;
    }

    /** Returns the client camera yaw (saved before rotation was applied). */
    public float getClientYaw() {
        if (rotationApplied) return originalYaw;
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null ? p.getYRot() : 0;
    }

    /** Returns the yaw the server believes the player has. */
    public float getServerYaw() {
        if (currentRotation != null) return currentRotation.getYaw();
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null ? p.getYRot() : 0;
    }

    public float getPreviousServerYaw() {
        if (previousRotation != null) return previousRotation.getYaw();
        return getServerYaw();
    }

    /** Returns the pitch the server believes the player has. */
    public float getServerPitch() {
        if (currentRotation != null) return currentRotation.getPitch();
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null ? p.getXRot() : 0;
    }

    public float getPreviousServerPitch() {
        if (previousRotation != null) return previousRotation.getPitch();
        return getServerPitch();
    }
}
