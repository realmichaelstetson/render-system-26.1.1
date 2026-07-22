package com.haloclient.client.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages MSDF font instances with lazy loading and caching.
 * MSDF atlases are stored in assets/halo/fonts/msdf/ as pairs of .png (atlas) and .json (metrics).
 */
public class MsdfFontManager {
    private static final Map<String, MsdfFont> fonts = new HashMap<>();

    /**
     * Gets (or lazily loads) an MSDF font by name.
     *
     * @param name       font file basename without extension (e.g. "inter-bold")
     * @param renderSize the desired rendering size in pixels
     * @return the MsdfFont instance, or null if loading failed
     */
    public static MsdfFont getFont(String name, float renderSize) {
        String key = name + "_" + renderSize;
        if (fonts.containsKey(key)) {
            return fonts.get(key);
        }

        try {
            Identifier imageId = Identifier.fromNamespaceAndPath("halo", "fonts/msdf/" + name + ".png");
            Identifier jsonId = Identifier.fromNamespaceAndPath("halo", "fonts/msdf/" + name + ".json");

            InputStream imageStream = Minecraft.getInstance().getResourceManager().open(imageId);
            InputStream jsonStream = Minecraft.getInstance().getResourceManager().open(jsonId);

            MsdfFont font = new MsdfFont(imageStream, jsonStream, renderSize);
            fonts.put(key, font);

            imageStream.close();
            jsonStream.close();

            System.out.println("[Halo/MSDF] Loaded MSDF font: " + name + " at size " + renderSize);
            return font;
        } catch (Exception e) {
            System.err.println("[Halo/MSDF] Failed to load MSDF font: " + name);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Frees all cached fonts.
     */
    public static void freeAll() {
        for (MsdfFont font : fonts.values()) {
            font.free();
        }
        fonts.clear();
    }
}
