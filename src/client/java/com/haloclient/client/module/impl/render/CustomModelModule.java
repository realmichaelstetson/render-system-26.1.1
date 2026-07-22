package com.haloclient.client.module.impl.render;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.ModeProperty;
import com.haloclient.client.module.property.NumberProperty;

public class CustomModelModule extends Module {

    private final ModeProperty modelMode = new ModeProperty("Model", "Default", "Default", "Tung Tung Sahur");
    private final NumberProperty modelScale = new NumberProperty("Scale", "Scale of the custom model.", 0.1, 5.0, 1.0, 0.05);

    public CustomModelModule() {
        super("CustomModel", "Replaces your player model with a custom 3D model.", Category.RENDER);
        
        // Show scale setting only when Tung Tung Sahur is selected
        modelScale.setVisible(() -> "Tung Tung Sahur".equalsIgnoreCase(modelMode.getValue()));
        
        addProperties(modelMode, modelScale);
    }

    public ModeProperty getModelMode() {
        return modelMode;
    }

    public NumberProperty getModelScale() {
        return modelScale;
    }

    public boolean isTungTungSahur() {
        return isEnabled() && "Tung Tung Sahur".equalsIgnoreCase(modelMode.getValue());
    }
}
