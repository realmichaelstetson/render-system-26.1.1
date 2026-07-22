package com.haloclient.client.movement;

import com.haloclient.client.rotation.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;

/**
 * Movement Fix — corrects WASD input to match server-side rotations.
 * 
 * When using silent rotations, the server sees a different yaw than the client camera.
 * Without correction, pressing W would move the player toward the SERVER's yaw direction
 * (which is where the aura target is), not toward the CLIENT's camera direction.
 * This makes movement feel broken and obvious to anti-cheat systems.
 *
 * The fix transforms the movement input vector from client-yaw-space to server-yaw-space:
 *   1. Calculate input angle: α = atan2(strafe, forward)
 *   2. Calculate magnitude:  M = √(forward² + strafe²)
 *   3. Delta angle: Δθ = serverYaw − clientYaw
 *   4. Corrected forward = M × cos(α + Δθ)
 *   5. Corrected strafe  = M × sin(α + Δθ)
 */
public final class MovementFix {

    public enum Mode {
        /** No correction — movement follows server yaw (broken feel) */
        OFF,
        /** Full vector transformation — smooth, natural WASD movement relative to camera */
        SILENT,
        /** Same as Silent but snaps to cardinal directions for extra AC safety */
        STRICT,
        /** Dynamic smooth interpolation between client movement and server angle */
        AI
    }

    private MovementFix() {}

    /**
     * Corrects movement input values based on the yaw difference between
     * client camera and server-side (spoofed) rotation.
     *
     * @param forward Raw forward impulse (-1 to 1)
     * @param strafe  Raw strafe impulse (-1 to 1)
     * @param mode    Movement fix mode
     * @return float[2] = {correctedForward, correctedStrafe}
     */
    public static float[] correct(float forward, float strafe, Mode mode) {
        if (mode == Mode.OFF) {
            return new float[]{forward, strafe};
        }

        RotationManager rm = RotationManager.getInstance();
        if (!rm.isActive()) {
            return new float[]{forward, strafe};
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (forward == 0 && strafe == 0)) {
            return new float[]{forward, strafe};
        }

        float clientYaw = rm.getClientYaw();
        float serverYaw = rm.getServerYaw();

        // Calculate delta yaw: client yaw - server yaw
        float deltaYawRad = (float) Math.toRadians(Mth.wrapDegrees(clientYaw - serverYaw));

        if (mode == Mode.AI) {
            // AI Mode: smooth dynamic interpolation based on delta angle to avoid abrupt snaps
            float factor = (float) Math.cos(deltaYawRad);
            deltaYawRad *= Math.max(0.2f, Math.abs(factor));
        }

        // Rotate WASD movement vector (forward, strafe) by deltaYaw
        float correctedForward = forward * Mth.cos(deltaYawRad) + strafe * Mth.sin(deltaYawRad);
        float correctedStrafe = strafe * Mth.cos(deltaYawRad) - forward * Mth.sin(deltaYawRad);

        if (mode == Mode.STRICT) {
            // Snap to nearest cardinal value (-1, 0, 1) for maximum AC safety
            correctedForward = snapToCardinal(correctedForward);
            correctedStrafe = snapToCardinal(correctedStrafe);
        }

        return new float[]{correctedForward, correctedStrafe};
    }

    /**
     * LiquidBounce-style input correction: rotate the pressed WASD direction from
     * client yaw into server yaw space before Minecraft consumes the input.
     */
    public static Input correctInput(Input input, float clientYaw, float serverYaw, Mode mode) {
        if (mode == Mode.OFF) {
            return input;
        }

        float forward = impulse(input.forward(), input.backward());
        float sideways = impulse(input.left(), input.right());
        if (forward == 0.0f && sideways == 0.0f) {
            return input;
        }

        float deltaYaw = clientYaw - serverYaw;
        if (mode == Mode.AI) {
            float deltaRad = deltaYaw * Mth.DEG_TO_RAD;
            float factor = (float) Math.cos(deltaRad);
            deltaYaw *= Math.max(0.2f, Math.abs(factor));
        }

        float radians = deltaYaw * Mth.DEG_TO_RAD;
        float newSideways = sideways * Mth.cos(radians) - forward * Mth.sin(radians);
        float newForward = forward * Mth.cos(radians) + sideways * Mth.sin(radians);

        float fixedSideways = Math.round(newSideways);
        float fixedForward = Math.round(newForward);

        if (mode == Mode.STRICT) {
            if (Math.abs(fixedForward) >= Math.abs(fixedSideways)) {
                fixedSideways = 0.0f;
            } else {
                fixedForward = 0.0f;
            }
        }

        return new Input(
                fixedForward > 0.0f,
                fixedForward < 0.0f,
                fixedSideways > 0.0f,
                fixedSideways < 0.0f,
                input.jump(),
                input.shift(),
                input.sprint()
        );
    }

    /**
     * Parses a mode string into the Mode enum.
     */
    public static Mode fromString(String name) {
        return switch (name.toLowerCase()) {
            case "silent" -> Mode.SILENT;
            case "strict" -> Mode.STRICT;
            case "ai" -> Mode.AI;
            default -> Mode.OFF;
        };
    }

    /** Snaps a value to the nearest cardinal direction (-1, 0, or 1). */
    private static float snapToCardinal(float value) {
        if (value > 0.5f) return 1.0f;
        if (value < -0.5f) return -1.0f;
        return 0.0f;
    }

    private static float impulse(boolean positive, boolean negative) {
        if (positive == negative) return 0.0f;
        return positive ? 1.0f : -1.0f;
    }
}
