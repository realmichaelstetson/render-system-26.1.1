package com.haloclient.client.util;

/**
 * Caches {@code System.currentTimeMillis()} once per frame so that render-path
 * code (animations, rainbow colours, stopwatches, etc.) can sample the time
 * without making repeated native calls that stall the CPU–GPU pipeline.
 * <p>
 * Call {@link #update()} exactly once at the very start of each render frame.
 */
public final class FrameClock {

    private static long millis = System.currentTimeMillis();

    private FrameClock() {
    }

    /** Called once per frame to snapshot the current time. */
    public static void update() {
        millis = System.currentTimeMillis();
    }

    /** Returns the cached time for the current frame. */
    public static long millis() {
        return millis;
    }
}
