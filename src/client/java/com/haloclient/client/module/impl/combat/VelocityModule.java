package com.haloclient.client.module.impl.combat;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.NumberProperty;

public class VelocityModule extends Module {
    public VelocityModule() {
        super("Velocity", "Reduces knockback", Category.COMBAT);
        addProperties(
                new NumberProperty("Horizontal", 0.0, 100.0, 90.0, 1.0),
                new NumberProperty("Vertical", 0.0, 100.0, 100.0, 1.0)
        );
    }
}
