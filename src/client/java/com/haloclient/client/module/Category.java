package com.haloclient.client.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    RENDER("Render"),
    VISUAL("Visual"),
    EXPLOIT("Exploit"),
    WORLD("World"),
    OTHER("Other");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
