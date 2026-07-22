package com.haloclient.client.event.events;

import com.haloclient.client.event.Event;
import com.haloclient.client.module.Module;

public class SuffixEvent extends Event {
    private final Module module;
    private String suffix;

    public SuffixEvent(Module module, String suffix) {
        this.module = module;
        this.suffix = suffix;
    }

    public Module getModule() {
        return module;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
