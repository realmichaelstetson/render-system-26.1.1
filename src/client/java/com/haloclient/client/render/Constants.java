package com.haloclient.client.render;

import net.minecraft.client.Minecraft;

import java.io.File;

public final class Constants {
    public static final Minecraft mc = Minecraft.getInstance();
    /** Returns the NanoVG context handle. Always use this instead of caching! */

    /** @deprecated Use {@link #VG()} instead — this may be 0 if accessed before GL init. */
    public static long VG = 0;
    public static final File DIRECTORY = new File(mc.gameDirectory, File.separator + "Halo" + File.separator);

    public static final double FIRST_FALL_MOTION = 0.0784000015258789D;
}
