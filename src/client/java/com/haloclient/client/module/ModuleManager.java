package com.haloclient.client.module;

import com.haloclient.client.module.impl.render.CapesModule;
import com.haloclient.client.module.impl.render.ClickGUIModule;
import com.haloclient.client.module.impl.render.CustomModelModule;
import com.haloclient.client.module.impl.render.InterfaceModule;
import com.haloclient.client.module.impl.combat.KillAuraModule;
import com.haloclient.client.module.impl.combat.VelocityModule;
import com.haloclient.client.module.impl.combat.AutoTotemModule;
import com.haloclient.client.module.impl.movement.FlightModule;
import com.haloclient.client.module.impl.movement.SpeedModule;
import com.haloclient.client.module.impl.movement.SprintModule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        // Register modules here
        addModule(new ClickGUIModule());
        addModule(new InterfaceModule());
        addModule(new CapesModule());
        addModule(new CustomModelModule());
        addModule(new KillAuraModule());
        addModule(new VelocityModule());
        addModule(new AutoTotemModule());
        addModule(new FlightModule());
        addModule(new SpeedModule());
        addModule(new SprintModule());
    }

    private void addModule(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public Module getModuleByName(String name) {
        return modules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) modules.stream()
                .filter(m -> m.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }
}
