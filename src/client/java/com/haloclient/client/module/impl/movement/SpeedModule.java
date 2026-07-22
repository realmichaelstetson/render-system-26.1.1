package com.haloclient.client.module.impl.movement;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.BooleanProperty;
import com.haloclient.client.module.property.NumberProperty;

public class SpeedModule extends Module {
    public SpeedModule() {
        super("Speed", "Makes you move faster", Category.MOVEMENT);
        addProperties(
                new NumberProperty("Factor", 1.0, 3.0, 1.2, 0.05),
                new BooleanProperty("Use Timer", true)
        );
    }
}
