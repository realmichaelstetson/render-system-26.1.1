package com.haloclient.client.module.impl.combat;

import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.module.property.BooleanProperty;

public class AutoTotemModule extends Module {
    public AutoTotemModule() {
        super("AutoTotem", "Automatically equips totems", Category.COMBAT);
        addProperties(
                new BooleanProperty("Smart Check", true),
                new BooleanProperty("Predict", false)
        );
    }
}
