package com.haloclient.client.module.impl.movement;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.ModeProperty;
import com.haloclient.client.module.property.NumberProperty;

public class FlightModule extends Module {
    public FlightModule() {
        super("Flight", "Allows you to fly", Category.MOVEMENT);
        addProperties(
                new NumberProperty("Speed", 0.1, 5.0, 1.0, 0.1),
                new ModeProperty("Type", "Vanilla", "Vanilla", "Motion", "Damage")
        );
    }
}
