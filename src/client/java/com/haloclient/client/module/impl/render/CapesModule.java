package com.haloclient.client.module.impl.render;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.BooleanProperty;

public class CapesModule extends Module {

    private final BooleanProperty waveyCapes = new BooleanProperty("Wavey Capes", "Enables smooth physics-based wavey cape rendering.", true);

    public CapesModule() {
        super("Capes", "Custom cape rendering options including Wavey Capes.", Category.RENDER);
        addProperties(waveyCapes);
    }

    public BooleanProperty getWaveyCapes() {
        return waveyCapes;
    }

    public boolean isWaveyCapesEnabled() {
        return isEnabled() && waveyCapes.getValue();
    }
}
