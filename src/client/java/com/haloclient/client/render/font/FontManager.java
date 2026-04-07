package com.haloclient.client.render.font;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final Map<String, HaloFont> fonts = new HashMap<>();

    public static void init() {
        // We'll load them lazily or all at once?
        // Let's load the commonly used ones.
        load("inter-regular.ttf", 18);
        load("inter-bold.ttf", 18);
        load("productsans-regular.ttf", 18);
    }

    public static HaloFont getFont(String name, float size) {
        String key = name + "_" + size;
        if (!fonts.containsKey(key)) {
            load(name, size);
        }
        return fonts.get(key);
    }

    private static void load(String fileName, float size) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath("halo", "fonts/" + fileName);
            InputStream is = Minecraft.getInstance().getResourceManager().open(id);
            fonts.put(fileName + "_" + size, new HaloFont(is, size));
            is.close();
        } catch (IOException e) {
            System.err.println("Failed to load font: " + fileName);
            e.printStackTrace();
        }
    }
}
